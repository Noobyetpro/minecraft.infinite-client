package org.theinfinitys.features.fighting

import org.theinfinitys.ConfigurableFeature
import org.theinfinitys.settings.InfiniteSetting

class PlayerManager : ConfigurableFeature(initialEnabled = false) {
    override val settings: List<InfiniteSetting<*>> =
        listOf(
            InfiniteSetting.PlayerListSetting(
                "Friends",
                "フレンドとして扱うプレイヤーのリスト。",
                mutableListOf(),
            ),
            InfiniteSetting.PlayerListSetting(
                "Enemies",
                "敵として扱うプレイヤーのリスト。",
                mutableListOf(),
            ),
        )
}
