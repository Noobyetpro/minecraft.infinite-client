package org.theinfinitys.features.automatic

import net.minecraft.client.MinecraftClient
import org.theinfinitys.ConfigurableFeature
import org.theinfinitys.InfiniteClient
import org.theinfinitys.settings.InfiniteSetting

/**
 * クライアントプレイヤーのAI制御を管理するフィーチャー。
 */
class AIMode : ConfigurableFeature(initialEnabled = false) {
    private val aiFeatureClasses: List<Class<out ConfigurableFeature>> = listOf(WoodCutter::class.java)

    // ダメージ検知用の変数
    private var lastKnownHealth: Float = -1.0f

    // --- 設定 ---
    override val settings: List<InfiniteSetting<*>> =
        listOf(
            InfiniteSetting.BooleanSetting(
                "AllowPlayerInput",
                "AIモード中でもプレイヤーの入力を許可します。",
                false,
            ),
            // 新しい設定: ダメージを受けた際にAIを強制中断する
            InfiniteSetting.BooleanSetting(
                "CancelOnDamaged",
                "プレイヤーがダメージを受けた際にAIを強制中断します。",
                false,
            ),
        )

    override fun enabled() {
        lastKnownHealth = MinecraftClient.getInstance().player?.health ?: -1.0f
    }

    override fun disabled() {
        lastKnownHealth = -1.0f // 無効化時にリセット
    }

    override fun tick() {
        if (!aiFeatureClasses.any { aiFeature -> InfiniteClient.isFeatureEnabled(aiFeature) }) {
            disable()
            return
        }
    }
}
