package org.theinfinitys.ai

import net.minecraft.client.MinecraftClient
import net.minecraft.util.math.Vec3d
import org.theinfinitys.ai.task.MoveTask
import java.util.ArrayDeque // ArrayDequeを使用

/**
 * PlayerAIからの指示を受け取り、タスクの追加と、tickイベントによる操作を実行する
 * インターフェースクラス。
 * タスクキューを管理し、実際の操作はPlayerControllerに委譲します。
 */
class PlayerInterface(private val client: MinecraftClient) {

    // プレイヤーの操作ロジックをカプセル化
    fun hasActiveTasks(): Boolean {
        return tasks.isNotEmpty()
    }

    private val controller: PlayerController = PlayerController(client)
    var lastTaskResult: TaskTickResult? = null

    // 実行待ちのタスクを保持するキュー。ArrayDequeはQueueインターフェースを実装しているため問題ありません。
    // ArrayDequeに変更
    private val tasks: ArrayDeque<Task> = ArrayDeque()

    /**
     * キューの**末尾**に新しいタスクを追加します。（通常実行）
     * @param task 追加するタスク
     */
    fun addTask(task: Task) {
        tasks.offer(task) // 末尾に追加
    }

    /**
     * 現在のタスクを中断し、新しいタスクを**キューの先頭**に追加して直ちに実行させます。（割り込み）
     *
     * 割り込みタスクが完了すると、キューの次にある（中断された）タスクが再開されます。
     * @param task 割り込みタスク
     */
    fun interruptTask(task: Task) {
        tasks.addFirst(task) // 先頭に追加
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
            val taskTickResult = currentTask.onTick(controller)
            lastTaskResult = taskTickResult
            when (taskTickResult) {
                is TaskTickResult.Progress -> {}
                is TaskTickResult.Success, is TaskTickResult.Failure -> {
                    // 完了または失敗したタスクはキューから削除
                    tasks.poll()
                    controller.stopMovementControl()
                }

                is TaskTickResult.Interrupt -> {
                    // 1. 現在のタスクをキューに残したまま、操作を停止
                    controller.stopMovementControl()

                    // 2. 割り込みとして渡された新しいタスクをキューの先頭に追加 (Interrupt処理)
                    tasks.addFirst(taskTickResult.interruptTask)

                    // NOTE: 現在のタスク（currentTask）はキューに残ります。
                    // 次のティックでは、割り込みタスクが実行されます。
                }
            }
        } else {
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