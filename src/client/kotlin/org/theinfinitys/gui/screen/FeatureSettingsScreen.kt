package org.theinfinitys.gui.screen

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.Text
import org.theinfinitys.ConfigurableFeature
import org.theinfinitys.Feature
import org.theinfinitys.gui.widget.InfiniteBlockListField
import org.theinfinitys.gui.widget.InfiniteButton
import org.theinfinitys.gui.widget.InfiniteEntityListField
import org.theinfinitys.gui.widget.InfinitePlayerListField
import org.theinfinitys.gui.widget.InfiniteScrollableContainer
import org.theinfinitys.gui.widget.InfiniteSelectionList
import org.theinfinitys.gui.widget.InfiniteSettingTextField
import org.theinfinitys.gui.widget.InfiniteSettingToggle
import org.theinfinitys.gui.widget.InfiniteSlider
import org.theinfinitys.gui.widget.InfiniteStringListField
import org.theinfinitys.settings.InfiniteSetting

class FeatureSettingsScreen(
    private val parent: Screen,
    private val feature: Feature,
) : Screen(Text.literal(feature.name)) {
    private var savedPageIndex: Int = 0
    private lateinit var scrollableContainer: InfiniteScrollableContainer

    override fun init() {
        super.init()
        // ... (initのコードは変更なし) ...

        if (parent is InfiniteScreen) {
            savedPageIndex = parent.pageIndex
        }

        val settingWidgets = mutableListOf<ClickableWidget>()
        var currentY = 50 // Starting Y position for settings
        val widgetWidth = width - 40 // Adjust width for padding
        val defaultWidgetHeight = 20 // Standard height for most setting widgets
        val blockListFieldHeight = height / 2 // Height for InfiniteBlockListField
        val padding = 5 // Padding between widgets

        (feature.instance as? ConfigurableFeature)?.settings?.forEach { setting ->
            when (setting) {
                is InfiniteSetting.BooleanSetting -> {
                    settingWidgets.add(InfiniteSettingToggle(20, currentY, widgetWidth, defaultWidgetHeight, setting))
                    currentY += defaultWidgetHeight + padding
                }
                is InfiniteSetting.IntSetting -> {
                    settingWidgets.add(InfiniteSlider(20, currentY, widgetWidth, defaultWidgetHeight, setting))
                    currentY += defaultWidgetHeight + padding
                }
                is InfiniteSetting.FloatSetting -> {
                    settingWidgets.add(InfiniteSlider(20, currentY, widgetWidth, defaultWidgetHeight, setting))
                    currentY += defaultWidgetHeight + padding
                }
                is InfiniteSetting.StringSetting -> {
                    settingWidgets.add(InfiniteSettingTextField(20, currentY, widgetWidth, defaultWidgetHeight, setting))
                    currentY += defaultWidgetHeight + padding
                }
                is InfiniteSetting.StringListSetting -> {
                    settingWidgets.add(InfiniteStringListField(20, currentY, widgetWidth, defaultWidgetHeight, setting))
                    currentY += defaultWidgetHeight + padding
                }
                is InfiniteSetting.EnumSetting<*> -> {
                    settingWidgets.add(InfiniteSelectionList(20, currentY, widgetWidth, defaultWidgetHeight, setting))
                    currentY += defaultWidgetHeight + padding
                }
                is InfiniteSetting.BlockIDSetting -> {
                    settingWidgets.add(InfiniteSettingTextField(20, currentY, widgetWidth, defaultWidgetHeight, setting))
                    currentY += defaultWidgetHeight + padding
                }
                is InfiniteSetting.EntityIDSetting -> {
                    settingWidgets.add(InfiniteSettingTextField(20, currentY, widgetWidth, defaultWidgetHeight, setting))
                    currentY += defaultWidgetHeight + padding
                }
                is InfiniteSetting.BlockListSetting -> {
                    settingWidgets.add(InfiniteBlockListField(20, currentY, widgetWidth, blockListFieldHeight, setting))
                    currentY += blockListFieldHeight + padding // Use blockListFieldHeight here
                }
                is InfiniteSetting.EntityListSetting -> {
                    settingWidgets.add(InfiniteEntityListField(20, currentY, widgetWidth, blockListFieldHeight, setting))
                    currentY += blockListFieldHeight + padding // Use blockListFieldHeight here
                }
                is InfiniteSetting.PlayerListSetting -> {
                    settingWidgets.add(InfinitePlayerListField(20, currentY, widgetWidth, blockListFieldHeight, setting))
                    currentY += blockListFieldHeight + padding // Use blockListFieldHeight here
                }
            }
        }

        val scrollableContainer =
            InfiniteScrollableContainer(
                20, // x
                50, // y (below feature name/description)
                width - 40, // width
                height - 100, // height (leaving space for title and close button)
                settingWidgets,
            )
        addDrawableChild(scrollableContainer)
        this.scrollableContainer = scrollableContainer

        // Add a close button to return to the parent screen
        addDrawableChild(
            InfiniteButton(
                width / 2 - 50,
                height - 30,
                100,
                20,
                Text.literal("Close"),
            ) {
                if (parent is InfiniteScreen) {
                    InfiniteScreen.selectedPageIndex = savedPageIndex
                }
                this.client?.setScreen(parent)
            },
        )
    }

    // --- イベント転送メソッドの追加と修正 ---

    override fun mouseClicked(
        mouseX: Double,
        mouseY: Double,
        button: Int,
    ): Boolean {
        // addDrawableChildで追加されたウィジェットはsuper.mouseClickedで処理される。
        // ただし、scrollableContainer は他のウィジェットの親であるため、最初に転送を試みるのが安全。
        if (scrollableContainer.mouseClicked(mouseX, mouseY, button)) return true
        return super.mouseClicked(mouseX, mouseY, button)
    }

    /**
     * ★ 追加・修正箇所 1: mouseDragged イベントを子のコンテナに転送
     * mouseClickedで true を返したウィジェットが mouseDragged を受け取る
     */
    override fun mouseDragged(
        mouseX: Double,
        mouseY: Double,
        button: Int,
        deltaX: Double,
        deltaY: Double,
    ): Boolean {
        // scrollableContainer は addDrawableChild に登録されているため、
        // super.mouseDragged が自動的に転送を試みるはずだが、念のため明示的に呼び出す。
        if (scrollableContainer.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }

    /**
     * ★ 追加・修正箇所 2: mouseReleased イベントを子のコンテナに転送
     */
    override fun mouseReleased(
        mouseX: Double,
        mouseY: Double,
        button: Int,
    ): Boolean {
        // super.mouseReleased の前に明示的に呼び出すことで、確実にイベントを処理させる
        if (scrollableContainer.mouseReleased(mouseX, mouseY, button)) return true
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun keyPressed(
        keyCode: Int,
        scanCode: Int,
        modifiers: Int,
    ): Boolean {
        if (scrollableContainer.keyPressed(keyCode, scanCode, modifiers)) return true
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun charTyped(
        chr: Char,
        modifiers: Int,
    ): Boolean {
        if (scrollableContainer.charTyped(chr, modifiers)) return true
        return super.charTyped(chr, modifiers)
    }

    // ... (render と shouldPause は変更なし) ...

    override fun render(
        context: DrawContext,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        // Render a semi-transparent background to make it look like a popup
        // renderBackground(context, mouseX, mouseY, delta) // This causes a crash
        context.fill(0, 0, width, height, 0x80000000.toInt())

        context.drawCenteredTextWithShadow(
            textRenderer,
            Text.literal(feature.name),
            width / 2,
            20,
            0xFFFFFFFF.toInt(),
        )
        context.drawCenteredTextWithShadow(
            textRenderer,
            Text.literal(feature.description),
            width / 2,
            35,
            0xFFAAAAAA.toInt(),
        )

        // Render other widgets for the feature settings here
        super.render(context, mouseX, mouseY, delta)
    }

    override fun shouldPause(): Boolean = false
}
