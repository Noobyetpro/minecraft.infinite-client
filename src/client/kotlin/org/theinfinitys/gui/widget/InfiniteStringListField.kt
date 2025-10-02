package org.theinfinitys.gui.widget

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.Text
import org.theinfinitys.settings.InfiniteSetting

class InfiniteStringListField(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    private val setting: InfiniteSetting.StringListSetting,
) : ClickableWidget(x, y, width, height, Text.literal(setting.name)) {
    private val textField: InfiniteTextField
    private val textRenderer = MinecraftClient.getInstance().textRenderer

    init {
        textField =
            InfiniteTextField(
                textRenderer,
                x + 5 + textRenderer.getWidth(setting.name) + 5, // Position after label
                y,
                150, // Fixed width for debugging
                height,
                Text.literal(setting.value.joinToString(", ")),
                InfiniteTextField.InputType.BLOCK_ID,
            )
        textField.text = setting.value.joinToString(", ")
        textField.setChangedListener { newText ->
            setting.value = newText.split(",").map { it.trim() }.filter { it.isNotBlank() }
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
            Text.literal(setting.name),
            x + 5,
            y + (height - 8) / 2,
            0xFFFFFFFF.toInt(),
        )

        textField.x = x + 5 + textRenderer.getWidth(setting.name) + 5
        textField.y = y
        textField.render(context, mouseX, mouseY, delta)
    }

    override fun mouseClicked(
        mouseX: Double,
        mouseY: Double,
        button: Int,
    ): Boolean {
        val handled = textField.mouseClicked(mouseX, mouseY, button)
        if (handled) {
            textField.isFocused = true
        }
        return handled
    }

    override fun keyPressed(
        keyCode: Int,
        scanCode: Int,
        modifiers: Int,
    ): Boolean = textField.keyPressed(keyCode, scanCode, modifiers)

    override fun charTyped(
        chr: Char,
        modifiers: Int,
    ): Boolean = textField.charTyped(chr, modifiers)

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        this.appendDefaultNarrations(builder)
    }
}
