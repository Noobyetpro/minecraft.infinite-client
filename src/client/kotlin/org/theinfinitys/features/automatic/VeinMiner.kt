package org.theinfinitys.features.automatic

import org.theinfinitys.ConfigurableFeature
import org.theinfinitys.InfiniteClient
import org.theinfinitys.settings.InfiniteSetting

class VeinMiner : ConfigurableFeature(initialEnabled = false) {
    override val settings: List<InfiniteSetting<*>> =
        listOf()

    override fun enabled() {
        // VeinMinerが有効になったら、WoodCutterを無効化する
        InfiniteClient.getFeature(WoodCutter::class.java)?.let { woodCutter ->
            if (woodCutter.isEnabled()) {
                woodCutter.disable()
                InfiniteClient.warn("WoodCutterを無効化しました。VeinMinerと競合します。")
            }
        }
    }
}
