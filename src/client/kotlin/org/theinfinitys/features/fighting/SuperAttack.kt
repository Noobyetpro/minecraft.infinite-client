package org.theinfinitys.features.fighting

import org.theinfinitys.ConfigurableFeature
import org.theinfinitys.settings.InfiniteSetting

class SuperAttack : ConfigurableFeature(initialEnabled = false) {
    enum class AttackMethod {
        PACKET,
        MINI_JUMP,
        FULL_JUMP,
    }

    override val settings: List<InfiniteSetting<*>> =
        listOf(
            InfiniteSetting.EnumSetting(
                "Method",
                "クリティカル攻撃の方法を選択します。",
                AttackMethod.MINI_JUMP,
                AttackMethod.entries.toList(),
            ),
        )
}
