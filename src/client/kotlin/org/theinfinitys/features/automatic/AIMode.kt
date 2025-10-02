package org.theinfinitys.features.automatic

import org.theinfinitys.ConfigurableFeature
import org.theinfinitys.InfiniteClient
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

    override fun disabled() {
        // AIモードが無効になったら、WoodCutterとVeinMinerを無効化する
        InfiniteClient.getFeature(WoodCutter::class.java)?.let { woodCutter ->
            if (woodCutter.enabled.value) {
                woodCutter.enabled.value = false
                InfiniteClient.warn("AIモードが無効になったため、WoodCutterを無効化しました。")
            }
        }
        InfiniteClient.getFeature(VeinMiner::class.java)?.let { veinMiner ->
            if (veinMiner.enabled.value) {
                veinMiner.enabled.value = false
                InfiniteClient.warn("AIモードが無効になったため、VeinMinerを無効化しました。")
            }
        }
    }
}
