package kuro9.mahjongbot.db.data

import kuro9.mahjongbot.annotation.GuildRes
import kuro9.mahjongbot.annotation.UserRes
import java.sql.Timestamp

data class Game(
    val id: Long = 0,
    @GuildRes val guildID: Long,
    @UserRes val addedBy: Long,
    val gameGroup: String = "",
    val createdAt: Timestamp = Timestamp(0)
)
