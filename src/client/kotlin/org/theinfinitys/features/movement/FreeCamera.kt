package org.theinfinitys.features.movement

import org.theinfinitys.ConfigurableFeature
import org.theinfinitys.settings.InfiniteSetting

class FreeCamera : ConfigurableFeature(initialEnabled = false) {
    override val settings: List<InfiniteSetting<*>> =
        listOf(
            InfiniteSetting.FloatSetting(
                "Speed",
                "Freecam中の移動速度を設定します。",
                1.0f,
                0.1f,
                5.0f,
            ),
        )
}
