package org.theinfinitys.features.rendering

import org.theinfinitys.ConfigurableFeature
import org.theinfinitys.settings.InfiniteSetting

class XRay : ConfigurableFeature(initialEnabled = false) {
    private var defaultGamma: Double = 0.0

    override val settings: List<InfiniteSetting<*>> =
        listOf(
            InfiniteSetting.BlockListSetting(
                "BlockList",
                "XRayで表示するブロックのリスト（ブロックIDをカンマ区切りで指定）。",
                mutableListOf(
                    "minecraft:diamond_ore",
                    "minecraft:deepslate_diamond_ore",
                    "minecraft:gold_ore",
                    "minecraft:deepslate_gold_ore",
                    "minecraft:iron_ore",
                    "minecraft:deepslate_iron_ore",
                    "minecraft:coal_ore",
                    "minecraft:deepslate_coal_ore",
                    "minecraft:lapis_ore",
                    "minecraft:deepslate_lapis_ore",
                    "minecraft:redstone_ore",
                    "minecraft:deepslate_redstone_ore",
                    "minecraft:ancient_debris",
                    "minecraft:spawner",
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
}
