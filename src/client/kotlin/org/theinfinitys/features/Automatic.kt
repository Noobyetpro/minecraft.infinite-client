package org.theinfinitys.features

import org.theinfinitys.feature
import org.theinfinitys.features.automatic.AIMode
import org.theinfinitys.features.automatic.VeinMiner
import org.theinfinitys.features.automatic.WoodCutter

val automatic =
    listOf(
        feature(
            "AIMode",
            AIMode(),
            "MinecraftのAIにすべての操作を任せます。",
        ),
        feature(
            "WoodCutter",
            WoodCutter(),
            "木を自動で採集します。",
        ),
        feature(
            "Reach",
            VeinMiner(),
            "プレイヤーの到達距離を拡張し、より遠くのブロックやエンティティを操作できるようにします。",
        ),
    )
