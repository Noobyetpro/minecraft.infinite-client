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

    // リスナーの同期に使用する専用のロックオブジェクト
    private val listenerLock = Any()

    // 依存関係・矛盾関係のリスナーを保持するリスト
    private val dependencyListeners = mutableListOf<() -> Unit>()

    init {
        // Featureが有効になったとき (enabled.value = true)
        enabled.addListener { _, newValue ->
            disabled.value = !newValue
            if (newValue) {
                // 依存・矛盾の即時解決
                resolve()
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
                // enabled.value = false から true に戻る場合 (通常は enable() 経由)
                resolve()
                enabled()
            }
        }
    }

    fun reset() {
        enabled.value = initialEnabled
        settings.forEach { it.reset() }
    }

    abstract val settings: List<InfiniteSetting<*>>

    // 論理積 (AND) 依存: 全て有効である必要がある
    open val depends: List<Class<out ConfigurableFeature>> = emptyList()

    // 【新規】論理和 (OR) 依存: どれか一つ有効であれば良い
    open val dependsOneOf: List<Class<out ConfigurableFeature>> = emptyList()

    // 矛盾関係
    open val conflicts: List<Class<out ConfigurableFeature>> = emptyList()

    open fun tick() {}
    open fun start() {}

    /**
     * Featureの依存関係の監視を開始する処理（リスナー登録）
     */
    private fun startResolver() {
        synchronized(listenerLock) {
            // --- 1. 論理和 (OR) 依存の監視 ---
            for (dependOr in dependsOneOf) {
                val feature = InfiniteClient.getFeature(dependOr) ?: continue

                // OR依存のFeatureが無効になったとき、依存関係のいずれかがまだ有効かチェック
                val listener: (Boolean, Boolean) -> Unit = { _, newDisabled ->
                    if (newDisabled && isEnabled()) {
                        if (!canBeEnabledByOrDependencies()) {
                            // 有効化できる依存が一つもなくなったら、この Feature を Disable にする
                            disable()
                        }
                    }
                }
                feature.disabled.addListener(listener)
                dependencyListeners.add { feature.disabled.removeListener(listener) }
            }
            // --- 2. 論理積 (AND) 依存の監視 ---
            for (depend in depends) {
                val feature = InfiniteClient.getFeature(depend) ?: continue
                val listener: (Boolean, Boolean) -> Unit = { _, newDisabled ->
                    if (newDisabled && isEnabled()) {
                        disable()
                    }
                }
                feature.disabled.addListener(listener)
                dependencyListeners.add { feature.disabled.removeListener(listener) }
            }

            // --- 3. 矛盾関係の監視 ---
            for (conflict in conflicts) {
                val feature = InfiniteClient.getFeature(conflict) ?: continue
                val listener: (Boolean, Boolean) -> Unit = { _, newEnabled ->
                    if (newEnabled && isEnabled()) {
                        disable()
                    }
                }
                feature.enabled.addListener(listener)
                dependencyListeners.add { feature.enabled.removeListener(listener) }
            }

        }
    }

    open fun stop() {}

    /**
     * Featureの依存関係の監視を停止する処理（リスナー解除）
     */
    private fun stopResolver() {
        synchronized(listenerLock) {
            dependencyListeners.forEach { it() }
            dependencyListeners.clear()
        }
    }

    open fun enabled() {}
    open fun disabled() {}

    fun enable() {
        if (isEnabled() || !checkDependOneOf()) return
        // 1. 依存関係の監視を開始し、リスナーを登録
        startResolver()
        // 2. プロパティの値を変更 (enabledリスナーが発火し、resolve()を実行)
        enabled.value = true
    }

    private fun checkDependOneOf(): Boolean {
        var result = false
        for (dependOr in dependsOneOf) {
            val feature = InfiniteClient.getFeature(dependOr) ?: continue
            result = result || feature.isEnabled()
        }
        return result
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

    /**
     * dependsOneOf のいずれかの Feature が有効かどうかをチェックする。
     */
    private fun canBeEnabledByOrDependencies(): Boolean {
        // dependsOneOf が設定されていない場合、条件は常に満たされている
        if (dependsOneOf.isEmpty()) return true

        // OR依存の Feature を一つでも見つけたら true
        return dependsOneOf.any { dependClass ->
            InfiniteClient.getFeature(dependClass)?.isEnabled() == true
        }
    }

    /**
     * このFeatureが有効になったときに、依存・矛盾を解決する（依存Enable、矛盾Disable）
     */
    private fun resolve() {
        // --- 1. 論理積 (AND) 依存の解決: 依存先を Enable にする ---
        for (depend in depends) {
            val feature = InfiniteClient.getFeature(depend) ?: continue
            if (feature.isDisabled()) {
                feature.enable()
            }
        }

        // --- 2. 矛盾関係の解決: 矛盾先を Disable にする ---
        for (conflict in conflicts) {
            val feature = InfiniteClient.getFeature(conflict) ?: continue
            if (feature.isEnabled()) {
                feature.disable()
            }
        }

        // --- 3. 論理和 (OR) 依存の解決: OR依存の Feature は自動的に Enable にしない ---
        // (どれか一つが有効であれば良いため、自身が Enable になるときに依存先を強制するロジックは不要)
    }
}