package org.theinfinitys.features

import org.theinfinitys.feature
import org.theinfinitys.features.automatic.AIMode
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
            "自動で木を集めます。",
        ),
    )
