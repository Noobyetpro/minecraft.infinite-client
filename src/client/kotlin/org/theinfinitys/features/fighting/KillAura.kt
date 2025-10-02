package org.theinfinitys.features.fighting

import org.theinfinitys.ConfigurableFeature
import org.theinfinitys.settings.InfiniteSetting

class KillAura : ConfigurableFeature(initialEnabled = false) {
    override val settings: List<InfiniteSetting<*>> =
        listOf(
            InfiniteSetting.FloatSetting(
                "Range",
                "エンティティを攻撃する最大距離を設定します。",
                4.2f,
                3.0f,
                7.0f,
            ),
            InfiniteSetting.IntSetting(
                "AttackDelay",
                "攻撃間のティック遅延を設定します。（0 = 最速）",
                1,
                0,
                20,
            ),
            InfiniteSetting.BooleanSetting(
                "Players",
                "プレイヤーをターゲットにします。",
                true,
            ),
            InfiniteSetting.BooleanSetting(
                "Mobs",
                "モブをターゲットにします。",
                false,
            ),
        )
}
