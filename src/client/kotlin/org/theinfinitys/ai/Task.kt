package org.theinfinitys.ai

/**
 * プレイヤーAIが実行すべき単一のタスクを定義するインターフェース。
 */
interface Task {
    /**
     * 1ティックごとに実行されるタスク処理ロジック。
     * @param controller 実際のクライアント操作を行うコントローラー
     * @return タスクが完了した場合はtrue、継続する場合はfalse
     */
    fun onTick(controller: PlayerController): TaskTickResult
}

sealed class TaskTickResult {
    /** 処理を継続している状態 */
    object Progress : TaskTickResult()

    /** 失敗によりタスクが終了した状態 */
    object Failure : TaskTickResult()

    /** 成功によりタスクが終了した状態 */
    object Success : TaskTickResult()

    /**
     * 実行中のタスクが自発的に中断され、指定された新しいタスクを割り込みとして挿入する状態。
     * @param interruptTask キューの先頭に挿入されるタスク
     */
    data class Interrupt(
        val interruptTask: Task,
    ) : TaskTickResult()
}
