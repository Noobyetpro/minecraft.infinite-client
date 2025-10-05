package org.theinfinitys.gui.widget

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.Text

class PlayerListItemWidget(
    x: Int,
    y: Int,
    width: Int,
    height: Int, // This height is for the individual item
    private val playerName: String,
    private val onRemove: (String) -> Unit,
) : ClickableWidget(x, y, width, height, Text.literal(playerName)) {
    private val textRenderer = MinecraftClient.getInstance().textRenderer
    private val padding = 8
    private val removeButtonWidth = 20

    override fun renderWidget(
        context: DrawContext,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        val textX = x + padding
        val textY = y + this.height / 2 - 4
        context.drawTextWithShadow(textRenderer, Text.literal(playerName), textX, textY, 0xFFAAAAAA.toInt())

        val removeButtonX = x + width - padding - removeButtonWidth
        val removeButtonY = y
        val isRemoveButtonHovered =
            mouseX >= removeButtonX &&
                mouseX < removeButtonX + removeButtonWidth &&
                mouseY >= removeButtonY &&
                mouseY < removeButtonY + this.height

        val removeColor = if (isRemoveButtonHovered) 0xFFAA4444.toInt() else 0xFF882222.toInt()

        context.fill(removeButtonX, removeButtonY, removeButtonX + removeButtonWidth, removeButtonY + this.height, removeColor)
        context.drawText(
            textRenderer,
            "x",
            removeButtonX + removeButtonWidth / 2 - 3,
            removeButtonY + this.height / 2 - 4,
            0xFFFFFFFF.toInt(),
            false,
        )
    }

    override fun mouseClicked(
        mouseX: Double,
        mouseY: Double,
        button: Int,
    ): Boolean {
        val removeButtonX = x + width - padding - removeButtonWidth
        val removeButtonY = y

        if (mouseX >= removeButtonX &&
            mouseX < removeButtonX + removeButtonWidth &&
            mouseY >= removeButtonY &&
            mouseY < removeButtonY + this.height
        ) {
            onRemove(playerName)
            return true
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun keyPressed(
        keyCode: Int,
        scanCode: Int,
        modifiers: Int,
    ): Boolean = super.keyPressed(keyCode, scanCode, modifiers)

    override fun charTyped(
        chr: Char,
        modifiers: Int,
    ): Boolean = super.charTyped(chr, modifiers)

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        builder.put(NarrationPart.TITLE, Text.literal("Player List Item: $playerName"))
    }
}
