package org.theinfinitys.ai

import net.minecraft.client.MinecraftClient
import net.minecraft.util.math.Vec3d
import org.theinfinitys.ai.task.MoveTask
import java.util.ArrayDeque
import java.util.Queue

/**
 * PlayerAIからの指示を受け取り、タスクの追加と、tickイベントによる操作を実行する
 * インターフェースクラス。
 * タスクキューを管理し、実際の操作はPlayerControllerに委譲します。
 */
class PlayerInterface(private val client: MinecraftClient) {

    // プレイヤーの操作ロジックをカプセル化
    private val controller: PlayerController = PlayerController(client)

    // 実行待ちのタスクを保持するキュー
    private val tasks: Queue<Task> = ArrayDeque()

    /**
     * キューに新しいタスクを追加します。
     * @param task 追加するタスク
     */
    fun addTask(task: Task) {
        tasks.offer(task)
    }

    /**
     * ティックイベントで呼び出され、現在のタスクを実行します。
     *
     * このメソッドは、ゲームのクライアントティック（ClientTickEventなど）にフックして
     * 毎秒20回（通常）呼び出される必要があります。
     */
    fun onClientTick() {
        // プレイヤーが存在しない、またはデッドの場合は何もしない
        if (client.player == null || client.player!!.isDead) {
            controller.stopMovementControl()
            return
        }

        val currentTask = tasks.peek()

        if (currentTask != null) {
            // 現在のタスクを1ティック実行
            val isFinished = currentTask.onTick(controller)

            if (isFinished) {
                // タスクが完了したらキューから削除
                tasks.poll()
                // 次のタスク実行前に、現在の操作を完全に停止してクリーンアップ
                controller.stopMovementControl()
            }
        } else {
            // タスクがない場合、プレイヤーの操作を停止
            controller.stopMovementControl()
        }
    }

    /**
     * 指定された目標位置への移動タスクを追加します。
     * (利便性のためのラッパー関数)
     */
    fun moveTo(targetPos: Vec3d) {
        addTask(MoveTask(targetPos))
    }

    /**
     * すべてのタスクをクリアし、プレイヤーの操作を停止します。
     */
    fun clearTasks() {
        tasks.clear()
        controller.stopMovementControl()
    }
}
