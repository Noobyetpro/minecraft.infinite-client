package org.theinfinitys.features.automatic

import org.theinfinitys.ConfigurableFeature
import org.theinfinitys.settings.InfiniteSetting

class AIMode : ConfigurableFeature(initialEnabled = false) {
    override val settings: List<InfiniteSetting<*>> =
        listOf(
            InfiniteSetting.BooleanSetting(
                "AllowPlayerInput",
                "AIモード中でもプレイヤーの入力を許可します。",
                false,
            ),
        )
    override val dependsOneOf: List<Class<out ConfigurableFeature>> =
        listOf(VeinMiner::class.java, WoodCutter::class.java)
}
