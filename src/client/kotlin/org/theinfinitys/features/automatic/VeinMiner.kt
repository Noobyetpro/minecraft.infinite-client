package org.theinfinitys.features.automatic

import org.theinfinitys.ConfigurableFeature
import org.theinfinitys.settings.InfiniteSetting

class VeinMiner : ConfigurableFeature(initialEnabled = false) {
    override val settings: List<InfiniteSetting<*>> = listOf()

    override val depends: List<Class<out ConfigurableFeature>> = listOf(AIMode::class.java)
    override val conflicts: List<Class<out ConfigurableFeature>> = listOf(WoodCutter::class.java)
}
