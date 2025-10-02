package org.theinfinitys.features.rendering

import org.theinfinitys.ConfigurableFeature
import org.theinfinitys.settings.InfiniteSetting

class AntiOverlay : ConfigurableFeature(initialEnabled = false) {
    override val settings: List<InfiniteSetting<*>> =
        listOf(
            InfiniteSetting.BooleanSetting("NoPumpkinOverlay", "カボチャのオーバーレイを削除します。", true),
            InfiniteSetting.BooleanSetting("NoDarknessOverlay", "暗闇のオーバーレイ（盲目効果など）を削除します。", true),
            InfiniteSetting.BooleanSetting("NoLiquidOverlay", "水/溶岩中のオーバーレイを削除します。", true),
            InfiniteSetting.BooleanSetting("NoFogOverlay", "フォグ(霧のようなエフェクト)を無効化します。", true),
        )
}
