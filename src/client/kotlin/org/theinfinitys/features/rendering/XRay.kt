package org.theinfinitys.features.rendering

import net.minecraft.client.MinecraftClient // Added import
import org.theinfinitys.ConfigurableFeature
import org.theinfinitys.settings.InfiniteSetting

class XRay : ConfigurableFeature(initialEnabled = false) {
    private var defaultGamma: Double = 0.0

    override val settings: List<InfiniteSetting<*>> = listOf(
        InfiniteSetting.BlockListSetting(
            "BlockList",
            "XRayで表示するブロックのリスト（ブロックIDをカンマ区切りで指定）。",
            mutableListOf(
                "ancient_debris",
                "anvil",
                "beacon",
                "bone_block",
                "bookshelf",
                "brewing_stand",
                "chain_command_block",
                "chest",
                "clay",
                "coal_block",
                "coal_ore",
                "command_block",
                "copper_ore",
                "crafting_table",
                "deepslate_coal_ore",
                "deepslate_copper_ore",
                "deepslate_diamond_ore",
                "deepslate_emerald_ore",
                "deepslate_gold_ore",
                "deepslate_iron_ore",
                "deepslate_lapis_ore",
                "deepslate_redstone_ore",
                "diamond_block",
                "diamond_ore",
                "dispenser",
                "dropper",
                "emerald_block",
                "emerald_ore",
                "enchanting_table",
                "end_portal",
                "end_portal_frame",
                "ender_chest",
                "furnace",
                "glowstone",
                "gold_block",
                "gold_ore",
                "hopper",
                "iron_block",
                "iron_ore",
                "ladder",
                "lapis_block",
                "lapis_ore",
                "lava",
                "lodestone",
                "mossy_cobblestone",
                "nether_gold_ore",
                "nether_portal",
                "nether_quartz_ore",
                "raw_copper_block",
                "raw_gold_block",
                "raw_iron_block",
                "redstone_block",
                "redstone_ore",
                "repeating_command_block",
                "spawner",
                "suspicous_sand",
                "tnt",
                "torch",
                "trapped_chest",
                "water"
            ),
        ),
        InfiniteSetting.BooleanSetting(
            "OnlyExposed",
            "洞窟内で見える鉱石のみを表示します。これにより、アンチX-Rayプラグイン対策に役立ちます。",
            false,
        ),
        InfiniteSetting.FloatSetting(
            "Opacity",
            "X-Rayが有効な場合の非鉱石ブロックの不透明度。",
            0.0f,
            0.0f,
            0.99f,
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
}
