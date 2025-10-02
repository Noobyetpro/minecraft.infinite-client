package org.theinfinitys.features.rendering

import org.theinfinitys.ConfigurableFeature
import org.theinfinitys.settings.InfiniteSetting

class SuperSight : ConfigurableFeature(initialEnabled = false) {
    override val settings: List<InfiniteSetting<*>> =
        listOf(
            InfiniteSetting.BooleanSetting("PlayerESP", "プレイヤーをハイライトします。", true),
            InfiniteSetting.BooleanSetting("ChestESP", "チェストをハイライトします。", true),
            InfiniteSetting.BooleanSetting("MobESP", "モブをハイライトします。", true),
            InfiniteSetting.BooleanSetting("PortalESP", "ポータルをハイライトします。", true),
            InfiniteSetting.FloatSetting("VisibleRange", "ハイライトする距離を設定します。", 16f, 8f, 256f),
            InfiniteSetting.BooleanSetting("FullBright", "ゲーム内の明るさを最大にします。", true),
            InfiniteSetting.BooleanSetting("AntiBlind", "盲目や暗闇のエフェクトを無効にします。", true),
        )
}
