package org.theinfinitys.features.movement

import net.minecraft.client.MinecraftClient
import org.theinfinitys.ConfigurableFeature
import org.theinfinitys.settings.InfiniteSetting

class SuperSprint : ConfigurableFeature(initialEnabled = false) {
    override val settings: List<InfiniteSetting<*>> =
        listOf(
            InfiniteSetting.BooleanSetting("OnlyWhenForward", "前方移動時のみスプリントを有効にします。", true),
            InfiniteSetting.BooleanSetting(
                "EvenIfHungry",
                "満腹度が不足している場合でも強制的にスプリントをします。",
                false,
            ),
            InfiniteSetting.FloatSetting(
                "BoostSprint",
                "通常よりもさらに速いスピードで移動を行えます。",
                1.0f,
                1.0f,
                1.4f,
            ),
        )

    override fun tick() {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return

        val onlyWhenForward = (settings[0] as InfiniteSetting.BooleanSetting).value
        val evenIfHungry = (settings[1] as InfiniteSetting.BooleanSetting).value
        val boostSprint = (settings[2] as InfiniteSetting.FloatSetting).value

        if (onlyWhenForward) {
            val lookVec = player.rotationVector // Get player's look vector
            val velocityVec = player.velocity // Get player's velocity vector

            // Calculate dot product to determine if moving forward or backward
            val dotProduct = lookVec.dotProduct(velocityVec)

            // If moving backward (dot product is negative), prevent sprinting and return
            // A small epsilon can be used to account for floating point inaccuracies
            if (dotProduct < -0.01) { // Adjust epsilon as needed
                player.isSprinting = false
                player.setVelocity(0.0, player.velocity.y, 0.0) // Stop horizontal movement
                return
            }
        }

        // Handle EvenIfHungry
        if (!evenIfHungry) { // If EvenIfHungry is false, check hunger
            // Player needs at least 7 hunger points (3.5 hunger bars) to sprint
            if (player.hungerManager.foodLevel <= 6) {
                player.isSprinting = false // Stop sprinting if hunger is too low
                return // Prevent further sprint logic
            }
        }

        if ((player.velocity.x != 0.0 || player.velocity.z != 0.0) && !player.isSprinting) {
            player.isSprinting = true
        }

        // Apply BoostSprint
        if (player.isSprinting) {
            // This is a basic speed boost. More advanced implementations might
            // modify player attributes or apply potion effects.
            // Directly setting velocity can be problematic and might be reset by the server.
            // A common approach is to modify the player's movement speed attribute.
            // For simplicity, we'll directly modify velocity, but be aware of its limitations.
            val currentVelocity = player.velocity
            val boostedVelocityX = currentVelocity.x * boostSprint
            val boostedVelocityZ = currentVelocity.z * boostSprint
            player.setVelocity(boostedVelocityX, currentVelocity.y, boostedVelocityZ)
        }
    }
}
