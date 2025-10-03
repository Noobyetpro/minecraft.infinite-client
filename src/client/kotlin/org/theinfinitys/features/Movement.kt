package org.theinfinitys.features

import org.theinfinitys.feature
import org.theinfinitys.features.movement.AutoMine
import org.theinfinitys.features.movement.AutoWalk
import org.theinfinitys.features.movement.FastBreak
import org.theinfinitys.features.movement.FastMove
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
            "FastMove",
            FastMove(),
            "斜め移動を駆使して移動速度を上昇させます",
        ),
        feature(
            "SafeWalk",
            SafeWalk(),
            "地形の橋から落下せずに安全に移動できるようになります",
        ),
        feature(
            "Freeze",
            Freeze(),
            "有効にしている間は、サーバーにデータを送信しません",
        ),
        feature(
            "AutoWalk",
            AutoWalk(),
            "自動で前進します",
        ),
        feature(
            "AutoMine",
            AutoMine(),
            "自動でブロックを採掘します",
        ),
        feature(
            "FastBreak",
            FastBreak(),
            "ブロックを壊す際のインターバルを削除します",
        ),
    )
