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
    fun onTick(controller: PlayerController): Boolean
}
