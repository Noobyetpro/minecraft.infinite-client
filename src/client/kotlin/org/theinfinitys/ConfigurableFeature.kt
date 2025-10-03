package org.theinfinitys

import com.mojang.brigadier.CommandDispatcher
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import org.lwjgl.glfw.GLFW
import org.theinfinitys.settings.InfiniteSetting
import org.theinfinitys.settings.Property

abstract class ConfigurableFeature(
    private val initialEnabled: Boolean = false,
) {
    internal var enabled: Property<Boolean> = Property(initialEnabled)
    private val disabled: Property<Boolean> = Property(!initialEnabled)
    val toggleKeyBind: Property<Int> = Property(GLFW.GLFW_DONT_CARE)

    init {
        enabled.addListener { _, newValue ->
            disabled.value = !newValue
            if (newValue) {
                enabled()
            } else {
                disabled()
            }
        }
        disabled.addListener { _, newValue ->
            enabled.value = !newValue
            if (newValue) {
                disabled()
            } else {
                enabled()
            }
        }
    }

    fun reset() {
        enabled.value = initialEnabled
        settings.forEach { it.reset() }
    }

    abstract val settings: List<InfiniteSetting<*>>

    open fun tick() {}

    open fun start() {}

    open fun stop() {}

    open fun enabled() {}

    open fun disabled() {}

    fun enable() {
        enabled.value = true
    }

    fun disable() {
        disabled.value = true
    }

    fun isEnabled(): Boolean = enabled.value

    fun isDisabled(): Boolean = disabled.value

    /**
     * 設定の名前で設定を取得します。
     * @param name 設定の名前
     * @return 指定された名前の設定、または見つからない場合はnull
     */
    fun getSetting(name: String): InfiniteSetting<*>? = settings.find { it.name == name }

    open fun registerCommands(dispatcher: CommandDispatcher<FabricClientCommandSource>) {}
}
