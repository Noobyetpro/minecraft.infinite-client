package org.theinfinitys.features.movement

import org.theinfinitys.ConfigurableFeature
import org.theinfinitys.settings.InfiniteSetting

class FastBreak : ConfigurableFeature(initialEnabled = false) {
    override val settings: List<InfiniteSetting<*>> = emptyList()
}
