package org.theinfinitys

import org.theinfinitys.features.fighting
import org.theinfinitys.features.movement
import org.theinfinitys.features.rendering

data class Feature(
    val name: String,
    val instance: Any,
    val description: String = "",
)

data class FeatureCategory(
    val name: String,
    val features: List<Feature>,
)

fun feature(
    name: String,
    instance: Any,
    description: String,
): Feature = Feature(name, instance, description)

val featureCategories =
    listOf(
        FeatureCategory("Movement", movement),
        FeatureCategory("Rendering", rendering),
        FeatureCategory("Fighting", fighting),
    )
