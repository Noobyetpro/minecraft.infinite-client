package org.theinfinitys.features

import org.theinfinitys.feature
import org.theinfinitys.features.automatic.AIMode

val automatic =
    listOf(
        feature(
            "AIMode",
            AIMode(),
            "MinecraftのAIにすべての操作を任せます。",
        ),
    )
