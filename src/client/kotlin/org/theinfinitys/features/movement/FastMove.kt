package org.theinfinitys.features.movement

import net.minecraft.client.MinecraftClient
import net.minecraft.util.math.Vec2f
import org.theinfinitys.ConfigurableFeature
import org.theinfinitys.settings.InfiniteSetting

class FastMove : ConfigurableFeature(initialEnabled = false) {
    override val settings: List<InfiniteSetting<*>> = emptyList()

    override fun tick() {
        var client = MinecraftClient.getInstance()
        var player = client.player ?: return

        if (player.isSpectator || player.isCreative) return // スペクテイター/クリエイティブでは適用しない

        // プレイヤーの Input オブジェクトを取得
        var input = player.input ?: return

        // PlayerInput から forward(), backward(), left(), right() の bool 値を取得する
        // ただし、Input クラスの movementVector を変更する方が、
        // 実際のゲーム内での移動（速度）に直接影響を与えやすいです。
        // ここでは、前の回答で特定した movementVector の制御に戻りつつ、
        // より正確な斜め移動判定のために PlayerInput の bool 値を利用するロジックを構築します。

        // --- 移動入力の状態判定 ---
        var playerInput = input.playerInput ?: return // PlayerInput オブジェクトを取得

        // 前後方向、左右方向の入力があるか
        val isForwardOrBackward = playerInput.forward() || playerInput.backward()
        val isStrafing = playerInput.left() || playerInput.right()

        // どちらか片方のみが入力されているか（直線移動中）をチェック
        if (isForwardOrBackward != isStrafing) {
            // --- 直線移動中の場合、斜め移動を強制するロジック ---

            // プレイヤーが現在計算されている移動ベクトルを取得
            // この movementVector が実際にゲーム内の移動に使用される
            // ※ movementVector は protected なので、Kotlinでのアクセス権限が前提となります。
            val movementVector = input.movementInput

            var newX = movementVector.x // 左右ストレイフ (A/D)
            var newY = movementVector.y // 前後移動 (W/S)

            if (isForwardOrBackward) {
                // WまたはSキーのみが押されている場合 (前後直線移動)

                // 左右の成分（X）に斜め移動の入力を追加
                // 前進(W, newY > 0)なら右ストレイフ(D)を追加、後退(S, newY < 0)なら左ストレイフ(A)を追加
                newX = if (newY > 0) 1.0f else -1.0f
            } else if (isStrafing) {
                // AまたはDキーのみが押されている場合 (左右直線移動)

                // 前後の成分（Y）に斜め移動の入力を追加
                // 左右どちらのストレイフ中でも、前進(W)を追加して斜め前進を強制
                // (速度増加効果が最大化されるのは斜め前進の組み合わせが多いため)
                newY = 1.0f
            }

            // movementVector を新しい値で更新
            // これにより、例えば W の直線入力が (0.0, 1.0) から斜めの (1.0, 1.0) になり、
            // 速度増加効果が発生することを期待します。
            input.movementVector = Vec2f(newX, newY)

            // プレイヤーの回転やカメラの向きは、movementVector ではなく、
            // rotationYaw や rotationPitch で制御されるため、この方法で変更しても影響はありません。
        }
    }
}
