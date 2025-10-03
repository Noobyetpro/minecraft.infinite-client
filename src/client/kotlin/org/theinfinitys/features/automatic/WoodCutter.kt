package org.theinfinitys.features.automatic

import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.world.ClientWorld
import net.minecraft.item.AxeItem
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import org.theinfinitys.ConfigurableFeature
import org.theinfinitys.InfiniteClient // ★ 追加
import org.theinfinitys.features.automatic.WoodCutter.Facing
import org.theinfinitys.features.automatic.WoodCutter.SwingHand
import org.theinfinitys.settings.InfiniteSetting
import java.util.ArrayDeque

class WoodCutter : ConfigurableFeature(initialEnabled = false) {
    private var currentTree: Tree? = null // To store the currently targeted tree

    data class Tree(
        val stump: BlockPos,
        val logs: MutableList<BlockPos>,
    )

    /**
     * ブロックが葉ブロックかどうかを判定します。
     */
    private fun isLeaves(state: BlockState): Boolean =
        state.block == Blocks.OAK_LEAVES ||
            state.block == Blocks.SPRUCE_LEAVES ||
            state.block == Blocks.BIRCH_LEAVES ||
            state.block == Blocks.JUNGLE_LEAVES ||
            state.block == Blocks.ACACIA_LEAVES ||
            state.block == Blocks.DARK_OAK_LEAVES

    /**
     * 指定されたブロック位置の周囲の原木ブロックを取得します。
     */
    private fun getNeighbors(
        pos: BlockPos,
        world: net.minecraft.client.world.ClientWorld,
    ): List<BlockPos> {
        val neighbors = mutableListOf<BlockPos>()
        for (x in -1..1) {
            for (y in -1..1) {
                for (z in -1..1) {
                    if (x == 0 && y == 0 && z == 0) continue // Skip the center block
                    val neighborPos = pos.add(x, y, z)
                    if (isLog(world.getBlockState(neighborPos))) {
                        neighbors.add(neighborPos)
                    }
                }
            }
        }
        return neighbors
    }

    /**
     * 指定された切り株から木全体を解析し、すべての原木ブロックを特定します。
     */
    private fun analyzeTree(
        stump: BlockPos,
        world: net.minecraft.client.world.ClientWorld,
    ): Tree {
        val logs = mutableListOf<BlockPos>()
        val queue = ArrayDeque<BlockPos>()

        logs.add(stump)
        queue.add(stump)

        // Limit the search to prevent infinite loops in case of malformed trees or very large structures
        for (i in 0 until 1024) {
            if (queue.isEmpty()) break

            val current = queue.removeFirst()

            for (next in getNeighbors(current, world)) {
                if (!logs.contains(next)) {
                    logs.add(next)
                    queue.add(next)
                }
            }
        }
        return Tree(stump, logs)
    }

    enum class Facing {
        OFF,
        SERVER_SIDE,
        CLIENT_SIDE,
        ;

        fun face(hitVec: Vec3d) {
            // TODO: Implement facing logic based on the selected mode
            // This will involve modifying player's yaw and pitch
            // For now, it's a placeholder.
        }
    }

    enum class SwingHand {
        MAIN_HAND,
        OFF_HAND,
        SERVER,
        ;

        fun swing(
            player: ClientPlayerEntity,
            hand: Hand,
        ) {
            player.swingHand(hand)
        }
    }

    override val settings: List<InfiniteSetting<*>> =
        listOf(
            InfiniteSetting.FloatSetting(
                "Range",
                "How far WoodCutter will reach to break blocks.",
                4.5f,
                1f,
                6f,
            ),
            InfiniteSetting.EnumSetting(
                "Facing",
                "How WoodCutter should face the logs and leaves when breaking them.",
                Facing.SERVER_SIDE,
                listOf(Facing.OFF, Facing.SERVER_SIDE, Facing.CLIENT_SIDE),
            ),
            InfiniteSetting.EnumSetting(
                "Swing Hand",
                "Which hand WoodCutter should swing.",
                SwingHand.SERVER,
                listOf(SwingHand.MAIN_HAND, SwingHand.OFF_HAND, SwingHand.SERVER),
            ),
        )

    override fun enabled() {
        // WoodCutterが有効になったら、VeinMinerを無効化する
        if (InfiniteClient.isFeatureEnabled(VeinMiner::class.java)) {
            val veinMiner = InfiniteClient.getFeature(VeinMiner::class.java)
            veinMiner?.disable()
            InfiniteClient.warn("VeinMinerを無効化しました。WoodCutterと競合します。")
        }
    }

    private var targetBlockPos: BlockPos? = null
    private var miningProgress = 0
    private var hasWarnedAxe = false // ★ 警告が連続して出ないようにフラグを追加
    private var isMoving = false // Add a flag to track if we are currently moving

    override fun tick() {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return
        val world = client.world ?: return

        // 斧を持っていない場合は採集ロジックをスキップし、警告を出す
        if (player.mainHandStack.item !is AxeItem) {
            if (!hasWarnedAxe) {
                // InfiniteClient.warn 関数を呼び出す
                InfiniteClient.warn("木こりボット：斧がメインハンドに見つかりません。採集を中断します。")
                hasWarnedAxe = true
            }
            // 採掘アニメーションが残っている場合があるので停止を試みる
            client.interactionManager?.stopUsingItem(player)
            targetBlockPos = null
            stopMovement(client) // Stop movement if axe is not held
            return
        } else {
            hasWarnedAxe = false // 斧を持ったらフラグをリセット
        }

        // 1. ターゲットが設定されていない場合、新しいターゲットを見つける
        if (targetBlockPos == null || isTargetBroken(world, targetBlockPos!!)) {
            targetBlockPos = findNearestLog(player, world)
            miningProgress = 0
            stopMovement(client) // Stop movement if target is broken or not found
        }

        val target = targetBlockPos ?: return

        // 2. ターゲットへ移動と採掘
        if (player.blockPos.isWithinDistance(target, 3.0)) {
            stopMovement(client) // Stop movement when close enough to mine
            startMining(client, target)
        } else {
            // 移動ロジックをここに記述
            moveToTarget(client, player, world, target)
        }
    }

    override fun disabled() {
        targetBlockPos = null
        miningProgress = 0
        hasWarnedAxe = false
        MinecraftClient.getInstance().interactionManager?.stopUsingItem(MinecraftClient.getInstance().player)
        stopMovement(MinecraftClient.getInstance()) // Stop movement when feature is disabled
    }

    /**
     * ターゲットブロックに向かってプレイヤーを移動させます。
     * プレイヤーのキー操作をシミュレートします。
     */
    private fun moveToTarget(
        client: MinecraftClient,
        player: ClientPlayerEntity,
        world: ClientWorld,
        target: BlockPos,
    ) {
        val options = client.options
        val playerPos = player.pos
        val targetVec = Vec3d.ofCenter(target)

        // Calculate direction vector
        val diffX = targetVec.x - playerPos.x
        val diffY = targetVec.y - playerPos.y
        val diffZ = targetVec.z - playerPos.z

        // Calculate yaw to face the target
        val yaw = MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(diffZ, diffX))) - 90.0f
        player.yaw = yaw.toFloat()

        // Simulate forward movement
        options.forwardKey.isPressed = true
        isMoving = true

        // Simple jump logic if there's a block directly in front and slightly above
        // This is a very basic obstacle avoidance. A proper pathfinding would be more robust.
        val playerBlockPos = player.blockPos
        val forwardBlockPos = playerBlockPos.offset(player.horizontalFacing)
        val blockInFront = world.getBlockState(forwardBlockPos)
        val blockAboveInFront = world.getBlockState(forwardBlockPos.up())

        if (!blockInFront.isAir && blockAboveInFront.isAir) {
            options.jumpKey.isPressed = true
        } else {
            options.jumpKey.isPressed = false
        }
    }

    /**
     * すべての移動キーの押下状態を解除します。
     */
    private fun stopMovement(client: MinecraftClient) {
        if (isMoving) {
            val options = client.options
            options.forwardKey.isPressed = false
            options.backKey.isPressed = false
            options.leftKey.isPressed = false
            options.rightKey.isPressed = false
            options.jumpKey.isPressed = false
            isMoving = false
        }
    }

    /**
     * 周囲の原木ブロックを探します。
     */
    private fun findNearestLog(
        player: ClientPlayerEntity,
        world: net.minecraft.client.world.ClientWorld,
    ): BlockPos? {
        // 【目標】プレイヤーから最も近い範囲内の原木ブロック (Log) を見つける。
        val range = 8 // 探索範囲（8ブロック四方）
        var nearestLog: BlockPos? = null
        var nearestDistanceSq = Double.MAX_VALUE

        // Minecraftの BlockPos.stream を使用して範囲内の全ての座標を走査
        BlockPos
            .stream(player.blockPos.add(-range, -range, -range), player.blockPos.add(range, range, range))
            .filter { pos -> isLog(world.getBlockState(pos)) } // isLogで原木か判定
            .forEach { pos ->
                val distanceSq = player.squaredDistanceTo(Vec3d.ofCenter(pos))
                if (distanceSq < nearestDistanceSq) {
                    nearestDistanceSq = distanceSq
                    nearestLog = pos.toImmutable() // 見つかった原木をターゲットに設定
                }
            }
        return nearestLog
    }

    /**
     * ターゲットが採掘によって壊されているか、または元々ログではないかを確認します。
     */
    private fun isTargetBroken(
        world: net.minecraft.client.world.ClientWorld,
        pos: BlockPos,
    ): Boolean {
        // 【目標】ターゲット位置のブロックが、もはや原木ではない（採掘済み）かを判断する。
        val state = world.getBlockState(pos)
        // ターゲットが原木ではない（壊された、または別のブロックに変わった）場合に true を返す
        return !isLog(state)
    }

    /**
     * サーバーに採掘をシミュレートするパケットを送信します。
     */
    private fun startMining(
        client: MinecraftClient,
        target: BlockPos,
    ) {
        // 【目標】クライアントの InteractionManager を利用して採掘操作をサーバーに送信する。
        val interactionManager = client.interactionManager ?: return

        // 採掘開始パケットに必要な BlockHitResult を作成
        // どの面を叩くかは、ここでは一旦 Direction.DOWN（下）で固定します。
        BlockHitResult(
            Vec3d.ofCenter(target),
            Direction.DOWN,
            target,
            false,
        )

        if (miningProgress == 0) {
            // 採掘アニメーションを開始するパケットをサーバーに送信
            interactionManager.attackBlock(target, Direction.DOWN)
        }

        // クライアント側の採掘進行度を更新
        miningProgress++

        // 採掘が完了するまでの時間（ティック数）をシミュレートします。
        // 実際の採掘時間はツールやエフェクトに依存しますが、ここではシンプルな固定値を使用。
        // ※ この値を調整することで、採掘速度が変わります。
        val ticksToBreak = 40 // 2秒 (40ティック) で壊れると仮定

        if (miningProgress >= ticksToBreak) {
            // 採掘完了パケットを送信し、ブロックを破壊するようサーバーに要求
            interactionManager.breakBlock(target)

            // ターゲットをリセットして次の木を探す準備
            targetBlockPos = null
            miningProgress = 0
        }
    }

    /**
     * ブロックが原木かどうかを判定します。
     * (findNearestLog および isTargetBroken から呼ばれるヘルパーメソッド)
     */
    private fun isLog(state: BlockState): Boolean {
        // Minecraftの Blocks クラスに含まれる原木ブロックで判定
        // Tag判定（BlockTags.LOGS）の方がより汎用性がありますが、シンプルな例として列挙します。
        return state.block == Blocks.OAK_LOG ||
            state.block == Blocks.SPRUCE_LOG ||
            state.block == Blocks.BIRCH_LOG ||
            state.block == Blocks.JUNGLE_LOG ||
            state.block == Blocks.ACACIA_LOG ||
            state.block == Blocks.DARK_OAK_LOG
    }
}
