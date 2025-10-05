package org.theinfinitys.features.ai

import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.network.ClientPlayerInteractionManager
import net.minecraft.command.argument.EntityAnchorArgumentType
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

/**
 * クライアントプレイヤーのAIの行動ロジックをカプセル化するクラス。
 */
class PlayerAI(private val client: MinecraftClient) {

    private var currentTask: AITask? = null

    /**
     * AIが移動中に**破壊を許可された**ブロックのIDリスト。
     */
    val allowedBreakBlocks: MutableSet<String> = mutableSetOf()

    /**
     * すべてのAIタスクの基底インターフェース
     */
    interface AITask {
        fun execute(player: ClientPlayerEntity, interactionManager: ClientPlayerInteractionManager): Boolean
    }

    /**
     * 特定の座標にプレイヤーを移動させるタスク。
     */
    private class MoveToPosTask(
        private val targetPos: Vec3d,
        private val client: MinecraftClient,
        private val aiController: PlayerAI
    ) : AITask {
        private val threshold = 0.5 // 到達と見なす許容誤差 (メートル)

        /**
         * 邪魔なブロック（破壊許可リストに含まれるブロック）を判定します。
         */
        private fun isPathObstruction(pos: BlockPos, world: net.minecraft.world.World): Boolean {
            val state = world.getBlockState(pos)
            val block = state.block

            val blockId = Registries.BLOCK.getId(block).toString()
            // 破壊許可ブロックに含まれていて、かつ空気ブロックでない場合に障害物とみなす
            if (aiController.allowedBreakBlocks.contains(blockId)) {
                return !state.isAir
            }
            // 破壊不可能な固いブロックも障害物とみなす（ただし、破壊タスクは開始しない）
            // このメソッドは破壊可能な障害物チェックに特化させる
            return false
        }

        override fun execute(player: ClientPlayerEntity, interactionManager: ClientPlayerInteractionManager): Boolean {
            val playerPos = player.pos
            val options = client.options
            val world = client.world ?: return true

            // 目的地到達チェック
            if (targetPos.squaredDistanceTo(playerPos) < threshold * threshold) {
                options.forwardKey.isPressed = false
                options.jumpKey.isPressed = false
                return true
            }

            // 視線合わせ: ターゲット座標の足元を向く
            player.lookAt(EntityAnchorArgumentType.EntityAnchor.FEET, targetPos)

            // 前進
            options.forwardKey.isPressed = true
            options.leftKey.isPressed = false
            options.rightKey.isPressed = false

            // --- パスクリアチェックと障害物破壊 ---
            // プレイヤーの進行方向のブロックを見て、破壊可能な障害物をチェックする
            val forwardDirection = player.horizontalFacing
            val playerBlockPos = player.blockPos
            val blockAboveInFrontPos = playerBlockPos.offset(forwardDirection).up()
            val headHeightPos = playerBlockPos.up(1).offset(forwardDirection)
            val forwardPos = playerBlockPos.offset(forwardDirection)

            // 優先度 a) 頭の高さにあるブロック（移動を完全にブロックする）
            if (isPathObstruction(headHeightPos, world)) {
                options.forwardKey.isPressed = false
                aiController.startBreakBlock(headHeightPos)
                return false
            }

            // 優先度 b) ジャンプ先や足元のブロック（移動をブロックする可能性がある）
            if (isPathObstruction(blockAboveInFrontPos, world)) {
                options.forwardKey.isPressed = false
                aiController.startBreakBlock(blockAboveInFrontPos)
                return false
            }
            if (isPathObstruction(forwardPos, world)) {
                options.forwardKey.isPressed = false
                aiController.startBreakBlock(forwardPos)
                return false
            }


            // --- ジャンプ/段差処理 ---
            val blockInFrontState = world.getBlockState(forwardPos)
            val blockAboveInFrontState = world.getBlockState(blockAboveInFrontPos)

            // 1ブロックの段差 (ジャンプで登れる)
            val isClimbingStep = player.isOnGround &&
                    blockInFrontState.isSolidBlock(world, forwardPos) &&
                    blockAboveInFrontState.isAir // 頭の高さは空いている

            // 2ブロックの壁 (ジャンプで登れない、または頭がぶつかる)
            val isTwoBlockWall = blockInFrontState.isSolidBlock(world, forwardPos) &&
                    world.getBlockState(playerBlockPos.up(2)).isSolidBlock(world, playerBlockPos.up(2))

            if (isClimbingStep) {
                options.jumpKey.isPressed = true // 1ブロックの段差ならジャンプ
            } else if (isTwoBlockWall) {
                options.forwardKey.isPressed = false // 登れない壁なので停止
                options.jumpKey.isPressed = false
            } else {
                // 登る必要がないか、空中にいる場合はジャンプを停止
                options.jumpKey.isPressed = false
            }

            return false
        }
    }

    /**
     * 特定のブロックを破壊するタスク。（キーバインド操作のみを使用）
     */
    // PlayerAI.kt の BreakBlockTask.execute() を以下のように修正
    private class BreakBlockTask(private val blockPos: BlockPos, private val client: MinecraftClient) : AITask {

        // 破壊リーチの許容値 (デフォルトの破壊リーチ)
        private val MAX_REACH = 5.0

        override fun execute(player: ClientPlayerEntity, interactionManager: ClientPlayerInteractionManager): Boolean {
            val world = client.world ?: return true
            val options = client.options

            // 1. ブロックがすでに空気なら完了
            if (world.getBlockState(blockPos).isAir) {
                options.attackKey.isPressed = false // 完了: 攻撃キーをリセット
                return true
            }

            val targetCenter = Vec3d.ofCenter(blockPos)

            // 2. 距離チェック: ターゲットがリーチ内にない場合はタスクを停止（スタック防止）
            if (player.eyePos.distanceTo(targetCenter) > MAX_REACH) {
                options.attackKey.isPressed = false
                player.sendMessage(Text.of("§e[AI] Target block out of reach. Stopping break task."), true)
                return true
            }

            // 3. 視線合わせ (破壊対象のブロックの中心を向く)
            player.lookAt(EntityAnchorArgumentType.EntityAnchor.FEET, targetCenter)
            options.attackKey.isPressed = true
            return false // 破壊完了までタスクを継続
        }
    }
    // ----------------------
    // 外部から呼び出すメソッド
    // ----------------------

    fun tick() {
        val player = client.player ?: return
        val interactionManager = client.interactionManager ?: return

        // タスクが完了したら (execute()がtrueを返したら)
        if (currentTask?.execute(player, interactionManager) == true) {
            currentTask = null
        }

        if (currentTask == null) {
            val options = client.options
            options.forwardKey.isPressed = false
            options.backKey.isPressed = false
            options.leftKey.isPressed = false
            options.rightKey.isPressed = false
            options.attackKey.isPressed = false
            options.jumpKey.isPressed = false
        }
    }

    fun startMoveTo(pos: Vec3d) {
        currentTask = MoveToPosTask(pos, client, this)
    }

    fun startBreakBlock(pos: BlockPos) {
        currentTask = BreakBlockTask(pos, client)
    }

    fun stopTask() {
        currentTask = null
        val options = client.options
        options.forwardKey.isPressed = false
        options.backKey.isPressed = false
        options.leftKey.isPressed = false
        options.rightKey.isPressed = false
        options.attackKey.isPressed = false
        options.jumpKey.isPressed = false
    }

    fun currentTaskIsNull(): Boolean {
        return currentTask == null
    }
}