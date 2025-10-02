package org.theinfinitys.gui.widget

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.Text
import org.theinfinitys.ConfigurableFeature
import org.theinfinitys.Feature
import org.theinfinitys.InfiniteClient

class InfiniteFeatureToggle(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    val feature: Feature,
    onSettings: () -> Unit,
) : ClickableWidget(x, y, width, height, Text.literal(feature.name)) {
    private val toggleButton: InfiniteToggleButton
    private val settingsButton: InfiniteButton
    private val resetButton: InfiniteButton // New reset button
    private val textRenderer = MinecraftClient.getInstance().textRenderer

    init {
        val buttonWidth = 50
        val settingsButtonWidth = 20
        val resetButtonWidth = 20 // Width for the reset button
        val spacing = 5

        val configurableFeature = feature.instance as ConfigurableFeature

        toggleButton =
            InfiniteToggleButton(
                x + width - buttonWidth,
                y,
                buttonWidth,
                height,
                configurableFeature.isEnabled(),
                true,
            ) { newState ->
                if (newState) {
                    configurableFeature.enable()
                } else {
                    configurableFeature.disable()
                }
            }

        settingsButton =
            InfiniteButton(
                x + width - buttonWidth - spacing - settingsButtonWidth,
                y,
                settingsButtonWidth,
                height,
                Text.literal("S"),
            ) { onSettings() }

        resetButton =
            InfiniteButton(
                x + width - buttonWidth - spacing * 2 - settingsButtonWidth - resetButtonWidth,
                y,
                resetButtonWidth,
                height,
                Text.literal("R"), // Placeholder for reset icon/text
            ) {
                // OnPress action for reset button
                configurableFeature.reset() // Reset feature's enabled state
                configurableFeature.settings.forEach { setting ->
                    setting.reset() // Reset individual settings
                }
                InfiniteClient.log("${feature.name} の設定をリセットしました。")
            }

        // Add listener to update toggle button when feature.enabled changes
        configurableFeature.enabled.addListener { _, newValue ->
            toggleButton.setState(newValue)
        }
    }

    override fun renderWidget(
        context: DrawContext,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        context.drawTextWithShadow(
            textRenderer,
            Text.literal(feature.name),
            x + 60,
            y + (height - 8) / 2,
            0xFFFFFFFF.toInt(),
        )

        toggleButton.x = x + width - toggleButton.width
        toggleButton.y = y
        settingsButton.x = x + width - toggleButton.width - 5 - settingsButton.width
        settingsButton.y = y
        resetButton.x = x + width - toggleButton.width - 5 * 2 - settingsButton.width - resetButton.width // Position reset button
        resetButton.y = y

        toggleButton.render(context, mouseX, mouseY, delta)
        settingsButton.render(context, mouseX, mouseY, delta)
        resetButton.render(context, mouseX, mouseY, delta) // Render reset button
    }

    override fun mouseClicked(
        mouseX: Double,
        mouseY: Double,
        button: Int,
    ): Boolean =
        toggleButton.mouseClicked(mouseX, mouseY, button) ||
            settingsButton.mouseClicked(mouseX, mouseY, button) ||
            resetButton.mouseClicked(mouseX, mouseY, button) // Handle reset button click

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        this.appendDefaultNarrations(builder)
    }
}
