package org.theinfinitys.features

import org.theinfinitys.feature
import org.theinfinitys.features.fighting.KillAura
import org.theinfinitys.features.fighting.Reach

val fighting =
    listOf(
        feature(
            "KillAura",
            KillAura(),
            "近くのエンティティを自動的に攻撃します。",
        ),
        feature(
            "Reach",
            Reach(),
            "プレイヤーの到達距離を拡張し、より遠くのブロックやエンティティを操作できるようにします。",
        ),
    )
