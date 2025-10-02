package org.theinfinitys

import org.lwjgl.glfw.GLFW
import org.theinfinitys.settings.InfiniteSetting
import org.theinfinitys.settings.Property

abstract class ConfigurableFeature(
    private val initialEnabled: Boolean = false,
) {
    var enabled: Property<Boolean> = Property(initialEnabled)
    val disabled: Property<Boolean> = Property(!initialEnabled)
    val toggleKeyBind: Property<Int> = Property(GLFW.GLFW_DONT_CARE)

    init {
        enabled.addListener { _, newValue ->
            disabled.value = !newValue
            enabled()
        }
        disabled.addListener { _, newValue ->
            enabled.value = !newValue
            disabled()
        }
    }

    fun reset() {
        enabled.value = initialEnabled
    }

    abstract val settings: List<InfiniteSetting<*>>

    open fun tick() {}

    open fun start() {}

    open fun stop() {}

    open fun enabled() {}

    open fun disabled() {}

    /**
     * 設定の名前で設定を取得します。
     * @param name 設定の名前
     * @return 指定された名前の設定、または見つからない場合はnull
     */
    fun getSetting(name: String): InfiniteSetting<*>? = settings.find { it.name == name }
}
