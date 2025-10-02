package org.theinfinitys.features.movement

import org.theinfinitys.ConfigurableFeature
import org.theinfinitys.settings.InfiniteSetting

class Freeze : ConfigurableFeature(initialEnabled = false) {
    private var freezeStartTime: Long = 0L

    override val settings: List<InfiniteSetting<*>> =
        listOf(
            InfiniteSetting.FloatSetting(
                "Duration",
                "フリーズが自動的に無効になるまでの最長持続時間（秒）。0で無制限。",
                0.0f,
                0.0f,
                600.0f, // 10 minutes max
            ),
        )

    override fun start() {
        freezeStartTime = System.currentTimeMillis()
    }

    override fun tick() {
        val durationSetting = settings[0] as InfiniteSetting.FloatSetting
        if (durationSetting.value > 0.0f) {
            val elapsedSeconds = (System.currentTimeMillis() - freezeStartTime) / 1000.0f
            if (elapsedSeconds >= durationSetting.value) {
                disable() // Disable the feature after duration
            }
        }
    }
}
