package org.theinfinitys.features.automatic

import net.minecraft.client.MinecraftClient
import net.minecraft.registry.Registries
import net.minecraft.util.math.BlockPos
import org.theinfinitys.ConfigurableFeature
import org.theinfinitys.InfiniteClient
import org.theinfinitys.settings.InfiniteSetting

class WoodCutter : ConfigurableFeature(initialEnabled = false) {
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
                "探索範囲。この範囲の高さを捜索する。",
                5,
                2,
                10,
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

    private fun searchTrees(): List<Tree> {
        val client = MinecraftClient.getInstance()
        val world = client.world ?: return emptyList()
        val player = client.player ?: return emptyList()

        val rangeSetting = settings.find { it.name == "Range" } as? InfiniteSetting.IntSetting
        val heightSetting = settings.find { it.name == "Height" } as? InfiniteSetting.IntSetting
        val logBlocksSetting = settings.find { it.name == "LogBlocks" } as? InfiniteSetting.BlockListSetting

        val range = rangeSetting?.value ?: 32
        val height = heightSetting?.value ?: 5
        val targetLogIds = logBlocksSetting?.value ?: emptyList()

        if (targetLogIds.isEmpty()) return emptyList()

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

                    if (targetLogIds.contains(blockId)) {
                        val blockUnderPos = currentPos.down()
                        val blockUnderState = world.getBlockState(blockUnderPos)
                        val blockUnderId = Registries.BLOCK.getId(blockUnderState.block).toString()

                        if (blockUnderId != blockId) {
                            val tree =
                                Tree(
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

    override fun tick() {
        InfiniteClient.log("\nSearchTree:\n${searchTrees()}")
    }
}

// ... (Treeクラスの定義を修正) ...

/**
 * Minecraft内の単一の木の情報（木の根元、種類、取れるブロック数）を保持するクラス。
 * logCountは、初期化時にrootPosから接続されたログブロックを自動で探索して計算されます。
 */
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
        // Treeの初期化時にログブロックを計算
        calculateLogBlocks()
    }

    /**
     * 木の情報を簡潔な文字列で表現します。
     */
    override fun toString(): String = "$id Tree at (${rootPos.x}, ${rootPos.y}, ${rootPos.z}) with $logCount logs"
}
