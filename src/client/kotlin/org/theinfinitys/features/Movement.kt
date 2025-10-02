package org.theinfinitys.features

import org.theinfinitys.feature
import org.theinfinitys.features.movement.Freeze
import org.theinfinitys.features.movement.SafeWalk
import org.theinfinitys.features.movement.SuperSprint

val movement =
    listOf(
        feature(
            "SuperSprint",
            SuperSprint(),
            "スプリントを拡張します",
        ),
        feature(
            "SafeWalk",
            SafeWalk(),
            "地形の橋から落下せずに安全に移動できるようになります",
        ),
        feature(
            "Freeze",
            Freeze(),
            "有効にしている間は、サーバーにデータを送信しません。",
        ),
    )
