package org.theinfinitys.features.rendering

import org.theinfinitys.ConfigurableFeature
import org.theinfinitys.settings.InfiniteSetting

class PortalGui : ConfigurableFeature(initialEnabled = false) {
    override val settings: List<InfiniteSetting<*>> = listOf()
}
