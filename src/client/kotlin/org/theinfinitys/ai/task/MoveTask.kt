package org.theinfinitys.ai.task

import net.minecraft.util.math.Vec3d
import org.theinfinitys.ai.PlayerController
import org.theinfinitys.ai.Task
/**
 * 指定された目標位置への移動を処理するタスク。
 * @param targetPos 移動目標のワールド座標
 * @param requiredDistance 目標位置に到達したと見なす距離 (デフォルトは0.5ブロック)
 */
class MoveTask(
    private val targetPos: Vec3d,
    private val requiredDistance: Double = 0.5
) : Task {

    override fun onTick(controller: PlayerController): Boolean {
        // プレイヤーが死んでいる場合はタスクを中断
        if (controller.getPlayer().isDead) {
            controller.stopMovementControl()
            return true
        }

        val playerPos = controller.getPlayer().pos
        val distanceSq = playerPos.squaredDistanceTo(targetPos)

        // 目標に十分近づいたらタスク完了
        if (distanceSq < requiredDistance * requiredDistance) {
            controller.stopMovementControl()
            return true // タスク完了
        }

        // プレイヤーの操作を実行（向き変更と前進）
        controller.moveTo(targetPos)

        return false // タスク継続
    }
}