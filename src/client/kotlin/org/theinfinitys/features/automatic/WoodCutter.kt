package org.theinfinitys.features.automatic

import net.minecraft.client.MinecraftClient
import net.minecraft.registry.Registries
import net.minecraft.util.math.BlockPos
import org.theinfinitys.ConfigurableFeature
import org.theinfinitys.InfiniteClient
import org.theinfinitys.ai.PlayerInterface
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
    override val settings: List<InfiniteSetting<*>> = listOf(
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

    override fun enabled() {
        playerInterface = PlayerInterface(MinecraftClient.getInstance())
        currentState = WoodCutterState.IDLE
        InfiniteClient.log("WoodCutter enabled. Starting search.")
    }

    override fun disabled() {
        currentState = WoodCutterState.IDLE
        targetTree = null
        InfiniteClient.log("WoodCutter disabled.")
    }

    override fun tick() {
        when (currentState) {
            WoodCutterState.IDLE -> {
                currentState = WoodCutterState.SEARCHING
            }

            WoodCutterState.SEARCHING -> {
                val foundTrees = searchTrees()
                if (foundTrees.isNotEmpty()) {
                    targetTree = foundTrees.first() // 最も近い木をターゲット
                    InfiniteClient.log("Tree found: ${targetTree!!.rootPos}. Moving to tree.")
                    currentState = WoodCutterState.MOVING_TO_TREE
                    val movePoint = targetTree?.rootPos?.toCenterPos()
                    if (movePoint != null) {
                        playerInterface.addTask(MoveTask(movePoint,1.0))
                    }
                } else {
                    InfiniteClient.log("No trees found in range.")
                }
            }

            WoodCutterState.MOVING_TO_TREE -> {
                val tree = targetTree
                if (tree == null) {
                    currentState = WoodCutterState.SEARCHING // ターゲット消失
                    return
                }
            }

            WoodCutterState.CUTTING_LOGS -> {
                // ログ伐採ロジックをここに実装する（ブロックの破壊、次のノードへの移動など）
                InfiniteClient.log("Cutting logic placeholder. Assuming finished.")
                // ログをすべて伐採したら、次の状態へ

                val collectItems = settings.find { it.name == "CollectItems" }?.value as? Boolean ?: false
                currentState = if (collectItems) {
                    WoodCutterState.COLLECTING_ITEMS
                } else {
                    WoodCutterState.SEARCHING // 次の木を探索
                }
            }

            WoodCutterState.COLLECTING_ITEMS -> {
                // アイテム回収ロジックをここに実装する
                InfiniteClient.log("Collecting items logic placeholder. Finished.")
                currentState = WoodCutterState.SEARCHING // 次の木を探索
            }
        }
        playerInterface.onClientTick()
    }

    /**
     * MinecraftのブロックIDが「*_log」のパターンに一致するかどうかを判定します。
     * （例: "minecraft:oak_log" -> true, "minecraft:stone" -> false）
     *
     * @param id チェックするMinecraftのブロックID文字列。
     * @return IDが*_logパターンに一致する場合は true、それ以外は false。
     */
    fun isLogBlock(id: String): Boolean {
        // パターン: "minecraft:" で始まり、間に任意の1文字以上があり、"_log" で終わる
        // \w+ は、1つ以上の単語文字（文字、数字、アンダースコア）にマッチします。
        // \w+ の代わりに .+ を使うと、より柔軟に任意の文字（1文字以上）にマッチできます。
        val pattern = Regex("minecraft:.+_log")

        return pattern.matches(id)
    }

    // ... (searchTrees 関数と Tree Data Class は元のままで、省略) ...
    private fun searchTrees(): List<Tree> {
        val client = MinecraftClient.getInstance()
        val world = client.world ?: return emptyList()
        val player = client.player ?: return emptyList()

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

                        if (blockUnderId != blockId) {
                            val tree = Tree(
                                rootPos = currentPos,
                                id = blockId,
                                client = client,
                            )
                            // 🌟 ログの座標を事前に取得しておく
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

data class Tree(
    val rootPos: BlockPos,
    val id: String,
    private val client: MinecraftClient,
) {
    // 🌟 ログブロックの座標を保持するリスト
    val logBlocks: MutableList<BlockPos> = mutableListOf()

    // logCountはlogBlocksのサイズから取得
    val logCount: Int
        get() = logBlocks.size

    /**
     * rootPosから接続されたログブロックをすべて探索し、logBlocksに格納します。
     * @return ログブロックの総数
     */
    fun calculateLogBlocks(): Int {
        val world = client.world ?: return 0
        logBlocks.clear() // リセット

        val countedLogs: MutableSet<BlockPos> = mutableSetOf()
        val toSearch: MutableList<BlockPos> = mutableListOf(rootPos)

        while (toSearch.isNotEmpty()) {
            val currentPos = toSearch.removeAt(0)

            if (countedLogs.contains(currentPos)) continue

            val state = world.getBlockState(currentPos)
            val currentBlockId = Registries.BLOCK.getId(state.block).toString()

            if (currentBlockId != id) continue

            // ログとしてカウントし、リストに追加
            logBlocks.add(currentPos)
            countedLogs.add(currentPos)

            val searchDirections = listOf(
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
        // Treeの初期化時にログブロックを計算
        calculateLogBlocks()
    }

    /**
     * 木の情報を簡潔な文字列で表現します。
     */
    override fun toString(): String = "$id Tree at (${rootPos.x}, ${rootPos.y}, ${rootPos.z}) with $logCount logs"
}
