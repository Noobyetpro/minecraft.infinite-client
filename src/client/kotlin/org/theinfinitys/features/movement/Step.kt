package org.theinfinitys.features.movement

import org.theinfinitys.ConfigurableFeature
import org.theinfinitys.settings.InfiniteSetting

class Step : ConfigurableFeature(initialEnabled = false) {
    override val settings: List<InfiniteSetting<*>> =
        listOf(
            InfiniteSetting.FloatSetting(
                "AdjustStepHeight",
                "上れる最大の高さ（ブロック）を設定します。",
                1.0f,
                0.6f,
                2.0f,
            ),
            InfiniteSetting.BooleanSetting(
                "AutoJumpAllowed",
                "Stepが有効な時に、MinecraftのAutoJumpを強制的に無効化します。",
                true,
            ),
        )
}
