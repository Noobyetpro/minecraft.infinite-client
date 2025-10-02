package org.theinfinitys.features.rendering

import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.client.MinecraftClient
import net.minecraft.registry.Registries
import net.minecraft.util.math.BlockPos
import org.theinfinitys.ConfigurableFeature
import org.theinfinitys.settings.InfiniteSetting

class XRay : ConfigurableFeature(initialEnabled = false) {
    override val settings: List<InfiniteSetting<*>> =
        listOf(
            InfiniteSetting.BlockListSetting(
                "BlockList",
                "XRayで表示するブロックのリスト（ブロックIDをカンマ区切りで指定）。",
                mutableListOf(
                    "minecraft:ancient_debris",
                    "minecraft:anvil",
                    "minecraft:beacon",
                    "minecraft:bone_block",
                    "minecraft:bookshelf",
                    "minecraft:brewing_stand",
                    "minecraft:chain_command_block",
                    "minecraft:chest",
                    "minecraft:clay",
                    "minecraft:coal_block",
                    "minecraft:coal_ore",
                    "minecraft:command_block",
                    "minecraft:copper_ore",
                    "minecraft:crafting_table",
                    "minecraft:deepslate_coal_ore",
                    "minecraft:deepslate_copper_ore",
                    "minecraft:deepslate_diamond_ore",
                    "minecraft:deepslate_emerald_ore",
                    "minecraft:deepslate_gold_ore",
                    "minecraft:deepslate_iron_ore",
                    "minecraft:deepslate_lapis_ore",
                    "minecraft:deepslate_redstone_ore",
                    "minecraft:diamond_block",
                    "minecraft:diamond_ore",
                    "minecraft:dispenser",
                    "minecraft:dropper",
                    "minecraft:emerald_block",
                    "minecraft:emerald_ore",
                    "minecraft:enchanting_table",
                    "minecraft:end_portal",
                    "minecraft:end_portal_frame",
                    "minecraft:ender_chest",
                    "minecraft:furnace",
                    "minecraft:glowstone",
                    "minecraft:gold_block",
                    "minecraft:gold_ore",
                    "minecraft:hopper",
                    "minecraft:iron_block",
                    "minecraft:iron_ore",
                    "minecraft:ladder",
                    "minecraft:lapis_block",
                    "minecraft:lapis_ore",
                    "minecraft:lava",
                    "minecraft:lodestone",
                    "minecraft:mossy_cobblestone",
                    "minecraft:nether_gold_ore",
                    "minecraft:nether_portal",
                    "minecraft:nether_quartz_ore",
                    "minecraft:raw_copper_block",
                    "minecraft:raw_gold_block",
                    "minecraft:raw_iron_block",
                    "minecraft:redstone_block",
                    "minecraft:redstone_ore",
                    "minecraft:repeating_command_block",
                    "minecraft:spawner",
                    "minecraft:suspicous_sand",
                    "minecraft:tnt",
                    "minecraft:torch",
                    "minecraft:trapped_chest",
                    "minecraft:water",
                ),
            ),
        )

    override fun enabled() {
        // Trigger world re-render when XRay is enabled
        MinecraftClient.getInstance().worldRenderer.reload()
    }

    override fun disabled() {
        // Trigger world re-render when XRay is disabled
        MinecraftClient.getInstance().worldRenderer.reload()
    }

    fun getBlockList(): MutableList<String> = (settings[0] as InfiniteSetting.BlockListSetting).value

    fun isVisible(
        block: Block,
        pos: BlockPos,
    ): Boolean {
        val blockList = getBlockList()
        val blockId = Registries.BLOCK.getId(block).toString()

        if (blockList.contains(blockId)) {
            return true
        }

        return false
    }

    fun shouldDrawSide(
        blockState: BlockState,
        blockPos: BlockPos?,
    ): Boolean? {
        if (!enabled.value) return null

        val block = blockState.block
        val pos = blockPos ?: return null

        return isVisible(block, pos)
    }
}
