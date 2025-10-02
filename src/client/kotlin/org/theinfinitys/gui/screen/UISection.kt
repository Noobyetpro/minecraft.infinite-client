package org.theinfinitys.gui.screen

import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.Text
import net.minecraft.util.math.ColorHelper
import org.theinfinitys.Feature
import org.theinfinitys.gui.widget.InfiniteButton
import org.theinfinitys.gui.widget.InfiniteFeatureToggle
import org.theinfinitys.gui.widget.InfiniteScrollableContainer

class UISection(
    val id: String,
    private val screen: Screen,
    featureList: List<Feature>? = null,
) {
    private var closeButton: InfiniteButton? = null
    val widgets = mutableListOf<ClickableWidget>()

    init {
        when (id) {
            "about" -> {}
            else -> {
                featureList?.let {
                    setupFeatureWidgets(it)
                }
            }
        }
    }

    private fun setupFeatureWidgets(features: List<Feature>) {
        val featureWidgets =
            features.map { feature ->
                feature.name
                InfiniteFeatureToggle(0, 0, 280, 20, feature) {
                    MinecraftClient.getInstance().setScreen(FeatureSettingsScreen(screen, feature))
                }
            }

        if (featureWidgets.isNotEmpty()) {
            val container = InfiniteScrollableContainer(0, 0, 300, 180, featureWidgets)
            widgets.add(container)
        }
    }

    fun render(
        context: DrawContext,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
        isSelected: Boolean,
        textRenderer: TextRenderer,
        borderColor: Int,
        alpha: Int,
        renderContent: Boolean,
    ) {
        val backgroundColor = ColorHelper.getArgb(alpha, 0, 0, 0)

        context.fill(x, y, x + width, y + height, backgroundColor)
        context.drawBorder(x, y, width, height, borderColor)

        val titleText =
            when (id) {
                "about" -> "About"
                else ->
                    id
                        .replace("-settings", "")
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } +
                        " Settings"
            }

        if (id == "about") {
            renderAbout(context, x, y, width, textRenderer, isSelected, mouseX, mouseY, delta, renderContent)
        } else {
            renderSettings(context, x, y, width, height, textRenderer, titleText, isSelected, mouseX, mouseY, delta, renderContent)
        }

        if (isSelected && renderContent) {
            if (closeButton == null || closeButton?.x != x + width - 30 || closeButton?.y != y + 10) {
                closeButton =
                    InfiniteButton(
                        x = x + width - 30,
                        y = y + 10,
                        width = 20,
                        height = 20,
                        message = Text.literal("X"),
                    ) {
                        screen.close()
                    }
            }
            closeButton?.render(context, mouseX, mouseY, delta)
        } else {
            closeButton = null
        }
    }

    private fun renderAbout(
        context: DrawContext,
        x: Int,
        y: Int,
        width: Int,
        textRenderer: TextRenderer,
        isSelected: Boolean,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
        renderContent: Boolean,
    ) {
        renderTitle(context, x, y, width, textRenderer, "About", isSelected)
        if (!renderContent) return

        val lineY = y + 50
        context.drawTextWithShadow(textRenderer, Text.literal("Version: 1.0"), x + 20, lineY, 0xFFFFFFFF.toInt())
        context.drawTextWithShadow(textRenderer, Text.literal("Author: The Infinitys"), x + 20, lineY + 15, 0xFFFFFFFF.toInt())

        val urlText = "URL: https://github.com/the-infinitys/minecraft.infinite-client"
        val wrappedText = textRenderer.wrapLines(Text.literal(urlText), width - 40)
        var textY = lineY + 30
        for (line in wrappedText) {
            context.drawTextWithShadow(textRenderer, line, x + 20, textY, 0xFFFFFFFF.toInt())
            textY += textRenderer.fontHeight
        }

        val widgetY = textY + 10
        widgets.forEach { widget ->
            widget.x = x + 20
            widget.y = widgetY
            widget.render(context, mouseX, mouseY, delta)
        }
    }

    private fun renderSettings(
        context: DrawContext,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        textRenderer: TextRenderer,
        titleText: String,
        isSelected: Boolean,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
        renderContent: Boolean,
    ) {
        renderTitle(context, x, y, width, textRenderer, titleText, isSelected)
        if (!renderContent) return

        var currentY = y + 50
        widgets.forEach { widget ->
            if (widget is InfiniteScrollableContainer) {
                // スクロールコンテナの位置と高さの再計算
                widget.setPosition(x + (width - widget.width) / 2 - 20, y + 50)
                widget.height = height - 60
            } else {
                widget.x = x + 20
                widget.y = currentY
                currentY += widget.height + 5
            }
            widget.render(context, mouseX, mouseY, delta)
        }
    }

    private fun renderTitle(
        context: DrawContext,
        x: Int,
        y: Int,
        width: Int,
        textRenderer: TextRenderer,
        titleText: String,
        isSelected: Boolean,
    ) {
        val title = Text.of(titleText)
        val textWidth = textRenderer.getWidth(title)
        val textX = x + (width - textWidth) / 2
        val textY = y + 20

        val color = if (isSelected) 0xFFFFFFFF.toInt() else 0xFF888888.toInt()
        context.drawTextWithShadow(textRenderer, title, textX, textY, color)
    }

    // ★ 修正点: mouseClicked を Boolean 戻り値に変更し、
    // イベントを処理したウィジェットでループを停止する
    fun mouseClicked(
        mouseX: Double,
        mouseY: Double,
        button: Int,
        isSelected: Boolean,
    ): Boolean { // ★ 戻り値を Boolean に変更
        if (!isSelected) return false

        // 1. closeButtonのクリック
        if (closeButton?.mouseClicked(mouseX, mouseY, button) == true) {
            return true
        }

        // 2. 他のウィジェットのクリック
        for (widget in widgets) {
            if (widget.mouseClicked(mouseX, mouseY, button)) {
                return true // ★ 最初に応答したウィジェットで停止し、フォーカスを与える
            }
        }

        return false
    }

    fun keyPressed(
        keyCode: Int,
        scanCode: Int,
        modifiers: Int,
        isSelected: Boolean,
    ) {
        if (!isSelected) return
        // keyPressed は一般的に全ての子に転送されます
        widgets.forEach { it.keyPressed(keyCode, scanCode, modifiers) }
    }

    fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        horizontalAmount: Double,
        verticalAmount: Double,
        isSelected: Boolean,
    ): Boolean {
        if (!isSelected) return false
        for (widget in widgets) {
            if (widget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
                return true
            }
        }
        return false
    }

    // ★ 修正点: mouseDragged を全てのウィジェットに転送
    fun mouseDragged(
        mouseX: Double,
        mouseY: Double,
        button: Int,
        deltaX: Double,
        deltaY: Double,
        isSelected: Boolean,
    ): Boolean { // ★ 戻り値は Boolean
        if (!isSelected) return false

        // closeButtonへのドラッグを処理
        if (closeButton?.mouseDragged(mouseX, mouseY, button, deltaX, deltaY) == true) {
            return true
        }

        // ★ スクロールコンテナとその他のウィジェット（スライダーなど）の両方に転送
        for (widget in widgets) {
            if (widget.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                return true
            }
        }
        return false
    }

    // ★ 修正点: mouseReleased を全てのウィジェットに転送
    fun mouseReleased(
        mouseX: Double,
        mouseY: Double,
        button: Int,
        isSelected: Boolean,
    ): Boolean { // ★ 戻り値は Boolean
        if (!isSelected) return false

        // closeButtonの mouseReleased を処理
        if (closeButton?.mouseReleased(mouseX, mouseY, button) == true) {
            return true
        }

        // ★ スクロールコンテナとその他のウィジェットの両方に転送
        for (widget in widgets) {
            if (widget.mouseReleased(mouseX, mouseY, button)) {
                return true
            }
        }
        return false
    }
}
