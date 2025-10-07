package org.theinfinitys.features.automatic

import net.minecraft.client.MinecraftClient
import net.minecraft.registry.Registries
import net.minecraft.util.math.BlockPos
import org.theinfinitys.ConfigurableFeature
import org.theinfinitys.InfiniteClient
import org.theinfinitys.ai.PlayerInterface
import org.theinfinitys.ai.TaskTickResult
import org.theinfinitys.ai.task.BreakBlockTask
import org.theinfinitys.ai.task.MoveTask
import org.theinfinitys.settings.InfiniteSetting

/**
 * 木こりAIの状態
 */
private enum class WoodCutterState {
    IDLE, // 待機中
    SEARCHING, // 木を探索中
    MOVING_TO_TREE, // 木の根元へ移動中
    CUTTING_LOGS, // 丸太を伐採中
    COLLECTING_ITEMS, // ドロップアイテムを回収中
}

class WoodCutter : ConfigurableFeature(initialEnabled = false) {
    private lateinit var playerInterface: PlayerInterface
    override val available: Boolean = false
    override val settings: List<InfiniteSetting<*>> =
        listOf(
            InfiniteSetting.IntSetting(
                "Range",
                "探索範囲。x,z方向にこの範囲内のブロックを捜索する。",
                32,
                5,
                64,
            ),
            InfiniteSetting.IntSetting(
                "Height",
                "探索範囲。y方向にこの範囲内のブロックを捜索する。",
                5,
                2,
                10,
            ),
            InfiniteSetting.BooleanSetting(
                "CollectItems",
                "伐採後にドロップアイテムを回収します。",
                true,
            ),
        )

    override val depends: List<Class<out ConfigurableFeature>> = listOf(AIMode::class.java)

    // --- 状態管理変数 ---
    private var currentState: WoodCutterState = WoodCutterState.IDLE
    private var targetTree: Tree? = null

    // どのログまで破壊したかを追跡するインデックス
    private var currentLogIndex: Int = 0

    override fun enabled() {
        playerInterface = PlayerInterface(MinecraftClient.getInstance())
        currentState = WoodCutterState.IDLE
        InfiniteClient.log("WoodCutter enabled. Starting search.")
    }

    override fun disabled() {
        // タスクを強制停止
        playerInterface.clearTasks()
        currentState = WoodCutterState.IDLE
        targetTree = null
        InfiniteClient.log("WoodCutter disabled.")
    }

    override fun tick() {
        // PlayerInterface が未初期化の場合は何もしない
        if (!::playerInterface.isInitialized) return

        when (currentState) {
            WoodCutterState.IDLE -> {
                currentState = WoodCutterState.SEARCHING
            }

            WoodCutterState.SEARCHING -> {
                // ... (探索ロジックはそのまま) ...
                val foundTrees = searchTrees()
                if (foundTrees.isNotEmpty()) {
                    targetTree = foundTrees.first()
                    currentLogIndex = 0 // ログ破壊インデックスをリセット
                    InfiniteClient.log("Tree found: ${targetTree!!.rootPos}. Moving to tree.")
                    currentState = WoodCutterState.MOVING_TO_TREE
                    val movePoint = targetTree?.rootPos?.toCenterPos()
                    // プレイヤーが丸太の真下に移動するように MoveTask を発行
                    if (movePoint != null) {
                        playerInterface.addTask(
                            MoveTask(
                                movePoint,
                                requiredDistance = 1.0,
                                breakableBlock = { id -> isLeave(id) },
                            ),
                        )
                    }
                } else {
                    InfiniteClient.log("No trees found in range.") // ログが多すぎるのを避けるためコメントアウト
                }
            }

            WoodCutterState.MOVING_TO_TREE -> {
                val tree = targetTree
                if (tree == null) {
                    currentState = WoodCutterState.SEARCHING // ターゲット消失
                    return
                }

                // プレイヤーインターフェースのトップタスク（MoveTask）が完了したかチェック
                val taskResult = playerInterface.lastTaskResult
                if (taskResult == TaskTickResult.Success) {
                    InfiniteClient.log("Reached tree. Starting to cut logs.")
                    currentState = WoodCutterState.CUTTING_LOGS
                    playerInterface.clearTasks() // 念のため移動タスクをクリア
                } else if (taskResult == TaskTickResult.Failure) {
                    InfiniteClient.log("Failed to move to tree. Searching again.")
                    currentState = WoodCutterState.SEARCHING
                }
            }

            WoodCutterState.CUTTING_LOGS -> {
                val tree = targetTree
                if (tree == null || tree.logBlocks.isEmpty()) {
                    InfiniteClient.log("Cutting finished or tree disappeared.")
                    // ログをすべて伐採したら、次の状態へ
                    val collectItems = settings.find { it.name == "CollectItems" }?.value as? Boolean ?: false
                    currentState =
                        if (collectItems) {
                            WoodCutterState.COLLECTING_ITEMS
                        } else {
                            WoodCutterState.SEARCHING
                        }
                    return
                }

                // すでにアクティブなタスク（BreakBlockTaskなど）がある場合は、その完了を待つ
                if (playerInterface.hasActiveTasks()) {
                    // タスクの結果を確認。成功していれば次のログへ進むロジックだが、
                    // PlayerInterfaceがSuccess時にタスクをクリアする前提ならここでは何もしない
                    // 完了を待ち、次のティックで hasActiveTasks() が false になるのを期待する。
                    return
                }

                // アクティブなタスクがなく、ログリストの末尾に達しているかチェック
                if (currentLogIndex >= tree.logBlocks.size) {
                    // ログリストを全て処理し終わった（タスクのクリアによりここに到達）
                    InfiniteClient.log("All known logs cut. Checking tree status again.")

                    // ログがまだ残っている可能性を考慮し、再探索/次の状態へ
                    val oldLogCount = tree.logCount
                    tree.calculateLogBlocks() // ログが残っていないか再計算

                    if (tree.logBlocks.isEmpty() || tree.logCount < oldLogCount) { // ログ数が減った/ゼロになった
                        // ログがなくなったか、連鎖的に減った（伐採成功）
                        InfiniteClient.log("Tree logs updated (Count: ${tree.logCount}).")

                        if (tree.logBlocks.isEmpty()) {
                            // 本当に終わった
                            InfiniteClient.log("Tree completely gone. Target cleared.")
                            targetTree = null
                            currentState = WoodCutterState.CUTTING_LOGS // 次の状態遷移ロジックへ
                        } else {
                            // ログが残っていた場合、インデックスをリセットして破壊再開
                            InfiniteClient.log("Remaining logs found. Restarting log cutting.")
                            currentLogIndex = 0
                            // 次のティックで BreakBlockTask が発行される
                        }
                    } else {
                        // ログ数に変化なし or 増加 (異常) -> 破壊フェーズ終了と見なす
                        InfiniteClient.log("No new logs found after check. Moving to next state.")
                        targetTree = null
                        currentState = WoodCutterState.CUTTING_LOGS // 次の状態遷移ロジックへ
                    }
                    return // このティックでは次のタスクを発行しない
                }

                // 伐採する次のログブロックを取得
                val logPosToBreak = tree.logBlocks[currentLogIndex]

                // BreakBlockTaskを発行し、インデックスを進める
                InfiniteClient.log("Breaking log at: $logPosToBreak (Index: $currentLogIndex / ${tree.logBlocks.size}).")
                playerInterface.addTask(BreakBlockTask(logPosToBreak))

                // タスク発行後、すぐにインデックスを次のログへ進める。
                // 次のティックでは hasActiveTasks() が true になり、このティックは return する。
                // BreakBlockTask が成功して PlayerInterface がタスクをクリアすると、
                // 次のティックで hasActiveTasks() が false になり、次のログの伐採に進む。
                currentLogIndex++
            }

            WoodCutterState.COLLECTING_ITEMS -> {
                // アイテム回収ロジックをここに実装する
                // TODO: 破壊された丸太の周囲のドロップアイテムを探索し、MoveTaskを発行して回収する
                InfiniteClient.log("Collecting items logic placeholder. Finished.")
                currentState = WoodCutterState.SEARCHING // 次の木を探索
            }
        }
        playerInterface.onClientTick()
    }

    /**
     * MinecraftのブロックIDが「*_log」のパターンに一致するかどうかを判定します。
     */
    fun isLogBlock(id: String): Boolean {
        // パターン: "minecraft:" で始まり、間に任意の1文字以上があり、"_log" で終わる
        val pattern = Regex("minecraft:.+_log")
        return pattern.matches(id)
    }

    fun isLeave(id: String): Boolean {
        // パターン: "minecraft:" で始まり、間に任意の1文字以上があり、"_log" で終わる
        val pattern = Regex("minecraft:.+_leave")
        return pattern.matches(id)
    }

    // ... (searchTrees 関数はそのまま) ...
    private fun searchTrees(): List<Tree> {
        val client = MinecraftClient.getInstance()
        val world = client.world ?: return emptyList()
        val player = client.player ?: return emptyList()

        // ... (Range/Height の設定取得ロジックはそのまま) ...
        val rangeSetting = settings.find { it.name == "Range" } as? InfiniteSetting.IntSetting
        val heightSetting = settings.find { it.name == "Height" } as? InfiniteSetting.IntSetting

        val range = rangeSetting?.value ?: 32
        val height = heightSetting?.value ?: 5

        val playerPos = player.blockPos
        val foundTrees = mutableListOf<Tree>()
        val searchedLogRoots = mutableSetOf<BlockPos>()

        val minX = playerPos.x - range
        val maxX = playerPos.x + range
        val minY = playerPos.y - height
        val maxY = playerPos.y + height
        val minZ = playerPos.z - range
        val maxZ = playerPos.z + range

        for (x in minX..maxX) {
            for (y in minY..maxY) {
                for (z in minZ..maxZ) {
                    val currentPos = BlockPos(x, y, z)
                    if (searchedLogRoots.contains(currentPos)) continue

                    val blockState = world.getBlockState(currentPos)
                    val blockId = Registries.BLOCK.getId(blockState.block).toString()

                    if (isLogBlock(blockId)) {
                        val blockUnderPos = currentPos.down()
                        val blockUnderState = world.getBlockState(blockUnderPos)
                        val blockUnderId = Registries.BLOCK.getId(blockUnderState.block).toString()

                        // 根元判定: 下のブロックのIDが現在のログと同じでない
                        if (blockUnderId != blockId) {
                            val tree =
                                Tree(
                                    rootPos = currentPos,
                                    id = blockId,
                                    client = client,
                                )

                            // ログの座標を事前に取得しておく
                            // Tree の init ブロックで既に呼ばれているが、念のため
                            tree.calculateLogBlocks()

                            if (tree.logCount > 0) {
                                foundTrees.add(tree)
                                searchedLogRoots.add(currentPos)
                            }
                        }
                    }
                }
            }
        }
        return foundTrees.sortedBy { it.rootPos.getSquaredDistance(playerPos) }
    }
}

// ... (Tree Data Class はそのまま) ...
data class Tree(
    val rootPos: BlockPos,
    val id: String,
    private val client: MinecraftClient,
) {
    val logBlocks: MutableList<BlockPos> = mutableListOf()
    val logCount: Int
        get() = logBlocks.size

    fun calculateLogBlocks(): Int {
        val world = client.world ?: return 0
        logBlocks.clear()

        val countedLogs: MutableSet<BlockPos> = mutableSetOf()
        val toSearch: MutableList<BlockPos> = mutableListOf(rootPos)

        while (toSearch.isNotEmpty()) {
            val currentPos = toSearch.removeAt(0)

            if (countedLogs.contains(currentPos)) continue

            val state = world.getBlockState(currentPos)
            val currentBlockId = Registries.BLOCK.getId(state.block).toString()

            if (currentBlockId != id) continue

            logBlocks.add(currentPos)
            countedLogs.add(currentPos)

            val searchDirections =
                listOf(
                    currentPos.up(),
                    currentPos.down(),
                    currentPos.north(),
                    currentPos.south(),
                    currentPos.east(),
                    currentPos.west(),
                )

            for (neighborPos in searchDirections) {
                if (!countedLogs.contains(neighborPos)) {
                    val neighborState = world.getBlockState(neighborPos)
                    val neighborBlockId = Registries.BLOCK.getId(neighborState.block).toString()

                    if (neighborBlockId == id) {
                        toSearch.add(neighborPos)
                    }
                }
            }
        }
        return logCount
    }

    init {
        calculateLogBlocks()
    }

    override fun toString(): String = "$id Tree at (${rootPos.x}, ${rootPos.y}, ${rootPos.z}) with $logCount logs"
}
