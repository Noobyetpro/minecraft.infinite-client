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

    private val listenerLock = Any()
    private val dependencyListeners = mutableListOf<() -> Unit>()

    init {
        // Featureが有効になったとき (enabled.value = true)
        enabled.addListener { _, newValue ->
            disabled.value = !newValue
            if (newValue) {
                // resolve() の呼び出しは enable() 内に移動済み
                enabled()
            } else {
                disabled()
            }
        }

        // Featureが無効になったとき (disabled.value = true)
        disabled.addListener { _, newValue ->
            enabled.value = !newValue
            if (newValue) {
                disabled()
            } else {
                // enabled.value = false から true に戻る場合の処理は不要
            }
        }
    }

    fun reset() {
        enabled.value = initialEnabled
        settings.forEach { it.reset() }
    }

    abstract val settings: List<InfiniteSetting<*>>

    // 論理積 (AND) 依存: 全て有効である必要がある（自動有効化の対象）
    open val depends: List<Class<out ConfigurableFeature>> = emptyList()

    // 【削除】論理和 (OR) 依存: dependsOneOf プロパティを削除

    // 矛盾関係
    open val conflicts: List<Class<out ConfigurableFeature>> = emptyList()

    open fun tick() {}
    open fun start() {}
    open fun stop() {}
    open fun enabled() {}
    open fun disabled() {}

    // --- リスナー登録用API ---
    fun addEnabledChangeListener(listener: (oldValue: Boolean, newValue: Boolean) -> Unit) {
        enabled.addListener(listener)
    }

    fun removeEnabledChangeListener(listener: (oldValue: Boolean, newValue: Boolean) -> Unit) {
        enabled.removeListener(listener)
    }

    /**
     * Featureの依存関係の監視を開始する処理（リスナー登録）
     */
    private fun startResolver() {
        synchronized(listenerLock) {
            // --- 1. 論理積 (AND) 依存の監視 ---
            for (depend in depends) {
                val feature = InfiniteClient.getFeature(depend) ?: continue
                val listener: (Boolean, Boolean) -> Unit = { _, newDisabled ->
                    if (newDisabled && isEnabled()) { disable() } // 依存先が切れたら自身も無効化
                }
                feature.disabled.addListener(listener)
                dependencyListeners.add { feature.disabled.removeListener(listener) }
            }

            // --- 2. 矛盾関係の監視 ---
            for (conflict in conflicts) {
                val feature = InfiniteClient.getFeature(conflict) ?: continue
                val listener: (Boolean, Boolean) -> Unit = { _, newEnabled ->
                    if (newEnabled && isEnabled()) { disable() } // 矛盾先が有効化されたら自身を無効化
                }
                feature.enabled.addListener(listener)
                dependencyListeners.add { feature.enabled.removeListener(listener) }
            }

            // 【削除】論理和 (OR) 依存の監視ロジックを削除
        }
    }

    private fun stopResolver() {
        synchronized(listenerLock) {
            dependencyListeners.forEach { it() }
            dependencyListeners.clear()
        }
    }

    // 【削除】canBeEnabledByOrDependencies() メソッドを削除

    /**
     * このFeatureが有効になったときに、依存・矛盾を解決する
     */
    private fun resolve() {
        // AND依存の解決: 依存先を自動的に Enable にして、満たす
        for (depend in depends) {
            val feature = InfiniteClient.getFeature(depend) ?: continue
            if (feature.isDisabled()) {
                feature.enable()
            }
        }

        // 矛盾関係の解決: 矛盾先を Disable にする
        for (conflict in conflicts) {
            val feature = InfiniteClient.getFeature(conflict) ?: continue
            if (feature.isEnabled()) {
                feature.disable()
            }
        }
    }

    // --- パブリック API ---

    fun enable() {
        if (isEnabled()) return

        // 【削除】OR依存の前提条件チェックを削除 (dependsOneOfが存在しないため)

        // 1. 依存関係の監視を開始し、リスナーを登録
        startResolver()

        // 2. 依存関係と矛盾関係を解決する (AND依存の自動有効化、矛盾の無効化)
        resolve()

        // 3. プロパティの値を変更 (enabledリスナーが発火し、enabled()を実行)
        enabled.value = true
    }

    fun disable() {
        if (isDisabled()) return

        // 1. 依存関係の監視を停止し、リスナーを解除
        stopResolver()
        // 2. プロパティの値を変更 (disabledリスナーが発火)
        disabled.value = true
    }

    fun isEnabled(): Boolean = enabled.value
    fun isDisabled(): Boolean = disabled.value

    fun getSetting(name: String): InfiniteSetting<*>? = settings.find { it.name == name }
    open fun registerCommands(dispatcher: CommandDispatcher<FabricClientCommandSource>) {}
}