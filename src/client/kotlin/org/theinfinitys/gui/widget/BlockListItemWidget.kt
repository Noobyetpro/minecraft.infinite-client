package org.theinfinitys.gui.widget

import net.minecraft.block.Blocks
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Identifier

class BlockListItemWidget(
    x: Int,
    y: Int,
    width: Int,
    height: Int, // This height is for the individual item
    private val blockId: String,
    private val onRemove: (String) -> Unit,
) : ClickableWidget(x, y, width, height, Text.literal(blockId)) {
    private val textRenderer = MinecraftClient.getInstance().textRenderer
    private val padding = 8
    private val iconSize = 16
    private val iconPadding = 2
    private val iconTotalWidth = iconSize + iconPadding
    private val removeButtonWidth = 20

    /**
     * ブロックID文字列から対応するItemStackを取得します。
     * IDが無効な場合は代替となるItemStack（例：バリアブロック）を返します。
     */
    private fun getItemStackFromId(id: String): ItemStack =
        try {
            val identifier = Identifier.of(id)
            val block = Registries.BLOCK.get(identifier)
            if (block != Blocks.AIR) {
                block.asItem().defaultStack
            } else {
                Items.BARRIER.defaultStack
            }
        } catch (e: Exception) {
            Items.BARRIER.defaultStack
        }

    override fun renderWidget(
        context: DrawContext,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        val itemX = x + padding
        val iconX = itemX + 2
        val iconY = y + (this.height - iconSize) / 2

        val itemStack = getItemStackFromId(blockId)
        context.drawItem(itemStack, iconX, iconY)

        val textX = iconX + iconTotalWidth
        val textY = y + this.height / 2 - 4
        context.drawTextWithShadow(textRenderer, Text.literal(blockId), textX, textY, 0xFFAAAAAA.toInt())

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
            onRemove(blockId)
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
        builder.put(NarrationPart.TITLE, Text.literal("Block List Item: $blockId"))
    }
}
