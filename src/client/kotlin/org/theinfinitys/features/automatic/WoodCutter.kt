package org.theinfinitys.features.automatic

import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.item.AxeItem
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import org.theinfinitys.ConfigurableFeature
import org.theinfinitys.InfiniteClient // ★ 追加
import org.theinfinitys.settings.InfiniteSetting

class WoodCutter : ConfigurableFeature(initialEnabled = false) {
    override val settings: List<InfiniteSetting<*>> =
        listOf(
            // 設定をここに記述
        )

    override fun enabled() {
        // WoodCutterが有効になったら、VeinMinerを無効化する
        if (InfiniteClient.isFeatureEnabled(VeinMiner::class.java)) {
            val veinMiner = InfiniteClient.getFeature(VeinMiner::class.java)
            veinMiner?.enabled?.value = false
            InfiniteClient.warn("VeinMinerを無効化しました。WoodCutterと競合します。")
        }
    }

    private var targetBlockPos: BlockPos? = null
    private var miningProgress = 0
    private var hasWarnedAxe = false // ★ 警告が連続して出ないようにフラグを追加

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
            return
        } else {
            hasWarnedAxe = false // 斧を持ったらフラグをリセット
        }

        // 1. ターゲットが設定されていない場合、新しいターゲットを見つける
        if (targetBlockPos == null || isTargetBroken(world, targetBlockPos!!)) {
            targetBlockPos = findNearestLog(player, world)
            miningProgress = 0
        }

        val target = targetBlockPos ?: return

        // 2. ターゲットへ移動と採掘
        if (player.blockPos.isWithinDistance(target, 3.0)) {
            startMining(client, target)
        } else {
            // 移動ロジックをここに記述
        }
    }

    override fun disabled() {
        targetBlockPos = null
        miningProgress = 0
        hasWarnedAxe = false
        MinecraftClient.getInstance().interactionManager?.stopUsingItem(MinecraftClient.getInstance().player)
    }

// ... (findNearestLog, isLog, isTargetBroken, startMining の各メソッドは変更なし) ...
// findNearestLog, isLog, isTargetBroken, startMining の実装は前回の回答を参照してください。
// (コードブロックが長くなるため、警告機能に関係のない部分は省略しています)
// WoodCutter クラス内に追加するメソッド

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
        return state.block == Blocks.OAK_LOG || state.block == Blocks.SPRUCE_LOG || state.block == Blocks.BIRCH_LOG ||
            state.block == Blocks.JUNGLE_LOG ||
            state.block == Blocks.ACACIA_LOG ||
            state.block == Blocks.DARK_OAK_LOG
    }
}
