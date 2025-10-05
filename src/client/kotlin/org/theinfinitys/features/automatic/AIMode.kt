package org.theinfinitys.features.automatic

import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import org.theinfinitys.ConfigurableFeature
import org.theinfinitys.InfiniteClient
import org.theinfinitys.features.ai.PlayerAI
import org.theinfinitys.settings.InfiniteSetting

/**
 * クライアントプレイヤーのAI制御を管理するフィーチャー。
 */
class AIMode : ConfigurableFeature(initialEnabled = false) {

    // PlayerAIのインスタンスを保持し、外部からアクセスできるように公開
    val playerAI: PlayerAI = PlayerAI(MinecraftClient.getInstance())

    // AI機能のリスト
    private val aiFeatureClasses: List<Class<out ConfigurableFeature>> = listOf(WoodCutter::class.java, VeinMiner::class.java)

    // ダメージ検知用の変数
    private var lastKnownHealth: Float = -1.0f

    // --- 設定 ---
    override val settings: List<InfiniteSetting<*>> = listOf(
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
        )
    )

    // --- 他のフィーチャーからPlayerAIの機能を利用するための公開インターフェース ---

    fun startMoveTo(pos: Vec3d) {
        if (!isEnabled()) {
            enable()
        }
        playerAI.startMoveTo(pos)
    }

    fun startBreakBlock(pos: BlockPos) {
        if (!isEnabled()) {
            enable()
        }
        playerAI.startBreakBlock(pos)
    }

    fun stopAITask() {
        playerAI.stopTask()
    }

    // --- ライフサイクルメソッドのオーバーライド ---
    private var isAutoJump = false
    override fun enabled() {
        isAutoJump = MinecraftClient.getInstance().options.autoJump.value
        MinecraftClient.getInstance().options.autoJump.value = true

        // 有効化時に初期ヘルスを記録
        lastKnownHealth = MinecraftClient.getInstance().player?.health ?: -1.0f
    }

    override fun disabled() {
        playerAI.stopTask()
        MinecraftClient.getInstance().options.autoJump.value = isAutoJump
        lastKnownHealth = -1.0f // 無効化時にリセット
    }

    override fun tick() {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return

        // 1. 他のAIフィーチャーの有効性チェック
        if (!aiFeatureClasses.any { aiFeature -> InfiniteClient.isFeatureEnabled(aiFeature) }) {
            disable()
            return
        }

        // 2. PlayerAIのメインロジック実行
        if (isEnabled()) {
            // --- CancelOnDamagedロジック ---
            val cancelOnDamaged = InfiniteClient.isSettingEnabled(AIMode::class.java, "CancelOnDamaged")
            val currentHealth = player.health

            if (cancelOnDamaged && lastKnownHealth > 0 && currentHealth < lastKnownHealth) {
                // ダメージを検知！強制中断
                player.sendMessage(Text.of("§c[AIMode] Damage detected! AI mode interrupted."), false)
                disable() // AIMode自体を無効化
                return
            }
            // ------------------------------------

            val allowInput = InfiniteClient.isSettingEnabled(AIMode::class.java, "AllowPlayerInput")

            // プレイヤー入力を禁止している場合は、AIが制御を奪う前にリセット
            if (!allowInput) {
                player.input.movementInput
            }

            playerAI.tick()

            // ティックの最後にヘルスを更新
            lastKnownHealth = currentHealth
        }
    }

    fun currentTaskIsNull(): Boolean {
        return playerAI.currentTaskIsNull()
    }

    companion object {
        fun getInstance(): AIMode? {
            return InfiniteClient.getFeature(AIMode::class.java)
        }
    }
}