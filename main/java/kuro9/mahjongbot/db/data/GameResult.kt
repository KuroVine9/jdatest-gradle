package kuro9.mahjongbot.db.data

import kuro9.mahjongbot.annotation.IntRange
import kuro9.mahjongbot.annotation.UserRes


data class GameResult(
    val gameID: Long = 0,
    @UserRes val userID: Long,
    @IntRange(1, 4) val rank: Int,
    val score: Int
)