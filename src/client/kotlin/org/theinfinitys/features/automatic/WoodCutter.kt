package org.theinfinitys.features.automatic

import net.minecraft.block.Block
import net.minecraft.block.LeavesBlock
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.ItemEntity
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import org.theinfinitys.ConfigurableFeature
import org.theinfinitys.settings.InfiniteSetting

/**
 * 自動伐採機能。
 */
class WoodCutter : ConfigurableFeature(initialEnabled = false) {
    // --- 状態管理 ---
    private enum class State {
        IDLE,
        SEARCHING,
        MOVING_TO_TREE,
        BREAKING_LOGS,
        BREAKING_INTERFERING_BLOCK,
        COLLECTING_ITEMS,
        COMPLETED_TREE,
    }

    private var currentState: State = State.IDLE
    private var targetTreeRoot: BlockPos? = null
    private var currentLogTarget: BlockPos? = null
    private var currentInterferingBlock: BlockPos? = null
    private var itemCollectionRange: Float = 10.0f

    // --- 設定 ---
    override val settings: List<InfiniteSetting<*>> =
        listOf(
            InfiniteSetting.FloatSetting(
                "Range",
                "探索範囲 (ブロック)。",
                32.0f,
                5.0f,
                64.0f,
            ),
            InfiniteSetting.BlockListSetting(
                "LogBlocks",
                "伐採対象の丸太ブロックIDリスト。",
                mutableListOf("minecraft:oak_log", "minecraft:spruce_log", "minecraft:birch_log"),
            ),
            InfiniteSetting.BlockListSetting(
                "LeavesBlocks",
                "破壊対象の葉ブロックIDリスト。",
                mutableListOf("minecraft:oak_leaves", "minecraft:spruce_leaves", "minecraft:birch_leaves"),
            ),
            InfiniteSetting.BooleanSetting(
                "CollectItems",
                "伐採後にドロップアイテムを回収します。",
                true,
            ),
        )

    override val depends: List<Class<out ConfigurableFeature>> = listOf(AIMode::class.java)
    override val conflicts: List<Class<out ConfigurableFeature>> = listOf(VeinMiner::class.java)

    // --- ヘルパーメソッド ---

    /**
     * 設定されたリスト内のログブロックであるか判定します。
     */
    private fun isLogBlock(block: Block): Boolean {
        val logIDs =
            (settings.find { it.name == "LogBlocks" } as? InfiniteSetting.BlockListSetting)?.value ?: emptyList()
        val currentBlockID = Registries.BLOCK.getId(block).toString()
        return logIDs.contains(currentBlockID)
    }

    /**
     * 設定されたリスト内の葉ブロックであるか判定します。
     */
    private fun isLeavesBlock(block: Block): Boolean {
        val leavesIDs =
            (settings.find { it.name == "LeavesBlocks" } as? InfiniteSetting.BlockListSetting)?.value ?: emptyList()
        val currentBlockID = Registries.BLOCK.getId(block).toString()
        return leavesIDs.contains(currentBlockID)
    }

    /**
     * 近くにある最も効率の良い木を探索します。（簡易：最も近いログブロックを探す）
     */
    private fun findBestTree(playerPos: BlockPos): BlockPos? {
        val world = MinecraftClient.getInstance().world ?: return null
        val rangeFloat = (settings.find { it.name == "Range" } as? InfiniteSetting.FloatSetting)?.value ?: 32.0f
        val range = rangeFloat.toInt()

        // 探索範囲はプレイヤーの上下に広げる
        val searchArea =
            BlockPos.iterate(
                playerPos.add(-range, -range, -range),
                playerPos.add(range, range, range),
            )

        var closestTreeRoot: BlockPos? = null
        var minDistanceSq = Double.MAX_VALUE

        for (pos in searchArea) {
            val block = world.getBlockState(pos).block

            if (isLogBlock(block)) {
                // --- 根元を見つけるロジック ---
                var rootPosCandidate = pos.toImmutable()
                var currentPos = pos.toImmutable()

                // 下にログがある限り掘り進める
                while (true) {
                    val nextPos = currentPos.down()
                    val nextState = world.getBlockState(nextPos)

                    if (isLogBlock(nextState.block)) {
                        rootPosCandidate = nextPos
                        currentPos = nextPos
                    } else if (nextState.isAir) {
                        // 下が空気の場合、そのまま上に進んでしまうのを防ぐため、探索を打ち切り
                        break
                    } else {
                        // 下がログではないブロック（土など）の場合、currentPosが根元
                        rootPosCandidate = currentPos
                        break
                    }
                }

                // 見つけた根元が、現在記録されている最も近い根元より近ければ更新
                val distanceSq = rootPosCandidate.toCenterPos().squaredDistanceTo(playerPos.toCenterPos())

                if (distanceSq < minDistanceSq) {
                    minDistanceSq = distanceSq
                    closestTreeRoot = rootPosCandidate
                }
            }
        }
        return closestTreeRoot
    }

    /**
     * 現在のログブロックの上にある、次のログブロックと、間に挟まれた邪魔なブロックを見つけます。
     */
    private fun findNextLogAndInterferingBlock(currentLogPos: BlockPos): Pair<BlockPos?, BlockPos?> {
        val world = MinecraftClient.getInstance().world ?: return Pair(null, null)

        var checkPos = currentLogPos.up()

        var maxCheckCount = 5 // チェック回数を増やす
        while (maxCheckCount > 0) {
            val state = world.getBlockState(checkPos)
            val block = state.block

            if (isLogBlock(block)) {
                return Pair(checkPos.toImmutable(), null)
            } else if (block is LeavesBlock || (!state.isReplaceable && !state.isAir)) {
                return Pair(null, checkPos.toImmutable())
            } else if (state.isAir) {
                checkPos = checkPos.up()
            } else {
                checkPos = checkPos.up()
            }
            maxCheckCount--
        }

        return Pair(null, null)
    }

    /**
     * 周囲のドロップアイテムのうち、最も近くにあるログブロックアイテムを探索します。
     */
    private fun findClosestItem(playerPos: Vec3d): ItemEntity? {
        val world = MinecraftClient.getInstance().world ?: return null
        var closestItem: ItemEntity? = null
        var minDistanceSq = itemCollectionRange * itemCollectionRange

        val box =
            Box(
                playerPos.x - itemCollectionRange,
                playerPos.y - itemCollectionRange,
                playerPos.z - itemCollectionRange,
                playerPos.x + itemCollectionRange,
                playerPos.y + itemCollectionRange,
                playerPos.z + itemCollectionRange,
            )

        val entities = world.getOtherEntities(null, box)

        for (entity in entities) {
            if (entity is ItemEntity) {
                val itemStack = entity.stack
                val itemBlock = Block.getBlockFromItem(itemStack.item)

                if (!isLogBlock(itemBlock)) continue

                val distanceSq = entity.pos.squaredDistanceTo(playerPos)

                if (distanceSq < 0.5) continue

                if (distanceSq.toFloat() < minDistanceSq) {
                    minDistanceSq = distanceSq.toFloat()
                    closestItem = entity
                }
            }
        }
        return closestItem
    }

    /**
     * プレイヤーの目の位置からターゲットログまでの直線上に邪魔なブロックがないかチェックします。
     */
    private fun findInterferingBlockBetweenPlayerAndTarget(
        startPos: Vec3d,
        targetPos: BlockPos,
    ): BlockPos? {
        val client = MinecraftClient.getInstance()
        val world = client.world ?: return null

        val endPos = Vec3d.ofCenter(targetPos)
        val delta = endPos.subtract(startPos)
        val distance = delta.length()

        val steps = (distance * 2).toInt().coerceAtLeast(1)
        val stepVec = delta.multiply(1.0 / steps.toDouble())

        for (i in 1..steps) {
            val checkPosVec = startPos.add(stepVec.multiply(i.toDouble()))
            val checkPos = BlockPos.ofFloored(checkPosVec)

            if (checkPos == targetPos) continue

            val state = world.getBlockState(checkPos)
            val block = state.block

            if (block is LeavesBlock || (!state.isReplaceable && !state.isAir)) {
                if (!isLogBlock(block)) {
                    return checkPos.toImmutable()
                }
            }
        }
        return null
    }

    override fun tick() {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return
        val world = client.world ?: return
        val aiMode = AIMode.getInstance() ?: return

        when (currentState) {
            State.IDLE, State.COMPLETED_TREE -> {
                aiMode.stopAITask()
                client.player?.sendMessage(Text.of("§b[WC] Starting search for the best tree..."), false)
                currentState = State.SEARCHING
            }

            State.SEARCHING -> {
                val bestTreePos = findBestTree(player.blockPos)

                if (bestTreePos != null) {
                    targetTreeRoot = bestTreePos
                    currentLogTarget = bestTreePos

                    // 【修正】移動目標を根元ブロックの足元に設定。
                    // プレイヤーをブロックの真下ではなく、側面へ移動させる
                    val targetMovePos = Vec3d.ofBottomCenter(bestTreePos.offset(player.horizontalFacing.opposite))
                    aiMode.startMoveTo(targetMovePos)

                    currentState = State.MOVING_TO_TREE
                } else {
                    client.player?.sendMessage(Text.of("§e[WC] No tree found in range. Idling."), false)
                    currentState = State.IDLE
                }
            }

            State.MOVING_TO_TREE -> {
                val rootPos =
                    targetTreeRoot ?: run {
                        currentState = State.IDLE
                        return
                    }

                // 移動タスク完了チェックをPlayerAIに依存
                if (aiMode.currentTaskIsNull()) {
                    client.player?.sendMessage(Text.of("§a[WC] Reached tree. Starting log break."), false)

                    // 最初にターゲットログを破壊する
                    aiMode.startBreakBlock(rootPos)
                    currentState = State.BREAKING_LOGS
                }
            }

            State.BREAKING_LOGS -> {
                val currentTarget =
                    currentLogTarget ?: run {
                        // currentLogTargetがnullなのにこのステートにいるのはおかしい
                        currentState = State.IDLE
                        return
                    }

                // ターゲットログの破壊が完了したかチェック
                if (world.getBlockState(currentTarget).isAir) {
                    val (nextLog, interferingBlock) = findNextLogAndInterferingBlock(currentTarget)

                    if (interferingBlock != null) {
                        currentInterferingBlock = interferingBlock
                        aiMode.startBreakBlock(interferingBlock)
                        currentState = State.BREAKING_INTERFERING_BLOCK
                    } else if (nextLog != null) {
                        currentLogTarget = nextLog
                        aiMode.startBreakBlock(nextLog)
                    } else {
                        // 次のログが見つからなかった場合（木の最上部に到達）
                        client.player?.sendMessage(Text.of("§a[WC] Tree fully chopped. Seeking items."), false)

                        // 【修正】ターゲットをnullにリセットして、AIを停止
                        currentLogTarget = null
                        aiMode.stopAITask()
                        currentState = State.COLLECTING_ITEMS
                    }
                }
                // else: ブロックがまだ空気でない場合（破壊タスク実行中）、何もしない
            }

            State.BREAKING_INTERFERING_BLOCK -> {
                val targetBlock =
                    currentInterferingBlock ?: run {
                        currentState = State.IDLE
                        return
                    }

                if (world.getBlockState(targetBlock).isAir) {
                    client.player?.sendMessage(
                        Text.of("§a[WC] Interfering block broken. Resuming log break."),
                        true,
                    )
                    currentInterferingBlock = null

                    // 【修正】破壊タスクを停止し、ログ破壊ステートに戻って次のティックで再計算させる
                    aiMode.stopAITask()
                    currentState = State.BREAKING_LOGS
                }
            }

            State.COLLECTING_ITEMS -> {
                // 1. PlayerAIが現在タスクを実行中かチェック (アイテムへの移動タスク)
                if (aiMode.currentTaskIsNull()) {
                    // 2. タスクが完了している (または実行されていない) 場合、次のアイテムを探す
                    val nextItem = findClosestItem(player.pos)

                    if (nextItem != null) {
                        // 次のアイテムが見つかったら、そこへ移動タスクを開始
                        aiMode.startMoveTo(nextItem.pos)
                        client.player?.sendMessage(Text.of("§a[WC] Moving to collect next item."), false)
                    } else {
                        // 収集範囲内にアイテムがもうない場合、収集完了
                        client.player?.sendMessage(
                            Text.of("§a[WC] Item collection complete. Seeking next tree."),
                            false,
                        )
                        targetTreeRoot = null
                        currentState = State.COMPLETED_TREE // 収集完了で次のツリーへ
                    }
                }
            }
            // ... (COLLECTING_ITEMSは変更なし) ...
        }
    }

// ... (findNextLogAndInterferingBlockは変更なし) ...
// ... (findClosestItemは変更なし) ...
// ... (findInterferingBlockBetweenPlayerAndTargetは、MoveToPosTaskで処理するため、削除しても良いが、ここでは一時的に残す) ...

    // --- ライフサイクル ---

    override fun enabled() {
        currentState = State.IDLE
        MinecraftClient.getInstance().player?.sendMessage(Text.of("§aWoodCutter enabled. Starting cycle."), false)

        val aiMode = AIMode.getInstance() ?: return

        // ログブロックIDを取得
        val logIDs =
            (settings.find { it.name == "LogBlocks" } as? InfiniteSetting.BlockListSetting)?.value ?: emptyList()

        // LeavesBlockのIDを取得
        val leavesIDs =
            (settings.find { it.name == "LeavesBlocks" } as? InfiniteSetting.BlockListSetting)?.value ?: emptyList()

        // PlayerAIの許可リストを設定
        aiMode.playerAI.allowedBreakBlocks.clear()
        aiMode.playerAI.allowedBreakBlocks.addAll(logIDs)
        // 経路上にある葉ブロックを自動で破壊できるように許可リストに追加
        aiMode.playerAI.allowedBreakBlocks.addAll(leavesIDs)
    }

    override fun disabled() {
        val aiMode = AIMode.getInstance()

        // PlayerAIの許可リストからWoodCutterのブロックをクリア (許可ブロックはすべて削除)
        aiMode?.playerAI?.allowedBreakBlocks?.clear()

        aiMode?.stopAITask()
        currentState = State.IDLE
        targetTreeRoot = null
        currentLogTarget = null
        currentInterferingBlock = null
        MinecraftClient.getInstance().player?.sendMessage(Text.of("§cWoodCutter disabled. Task stopped."), false)
    }
}
