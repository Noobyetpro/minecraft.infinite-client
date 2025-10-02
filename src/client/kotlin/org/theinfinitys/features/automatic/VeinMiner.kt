package org.theinfinitys.features.automatic

import org.theinfinitys.ConfigurableFeature
import org.theinfinitys.settings.InfiniteSetting

class VeinMiner : ConfigurableFeature(initialEnabled = false) {
    override val settings: List<InfiniteSetting<*>> =
        listOf()
}
