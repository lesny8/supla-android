package org.supla.android.data.source.local.entity
/*
Copyright (C) AC SOFTWARE SP. Z O.O.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.supla.android.data.model.general.ChannelBase
import org.supla.android.data.source.local.entity.ChannelGroupEntity.Companion.COLUMN_LOCATION_ID
import org.supla.android.data.source.local.entity.ChannelGroupEntity.Companion.COLUMN_PROFILE_ID
import org.supla.android.data.source.local.entity.ChannelGroupEntity.Companion.COLUMN_REMOTE_ID
import org.supla.android.data.source.local.entity.ChannelGroupEntity.Companion.TABLE_NAME
import org.supla.android.usecases.group.totalvalue.GroupTotalValue
import org.supla.android.usecases.group.totalvalue.GroupValue
import org.supla.core.shared.data.model.general.SuplaFunction

@Entity(
  tableName = TABLE_NAME,
  indices = [
    Index(
      value = [COLUMN_REMOTE_ID],
      name = "${TABLE_NAME}_${COLUMN_REMOTE_ID}_index"
    ),
    Index(
      value = [COLUMN_LOCATION_ID],
      name = "${TABLE_NAME}_${COLUMN_LOCATION_ID}_index"
    ),
    Index(
      value = [COLUMN_PROFILE_ID],
      name = "${TABLE_NAME}_${COLUMN_PROFILE_ID}_index"
    )
  ]
)
data class ChannelGroupEntity(
  @ColumnInfo(name = COLUMN_ID) @PrimaryKey override val id: Long?,
  @ColumnInfo(name = COLUMN_REMOTE_ID) override val remoteId: Int,
  @ColumnInfo(name = COLUMN_CAPTION) override val caption: String,
  @ColumnInfo(name = COLUMN_ONLINE) val online: Int,
  @ColumnInfo(name = COLUMN_FUNCTION) override val function: SuplaFunction,
  @ColumnInfo(name = COLUMN_VISIBLE) override val visible: Int,
  @ColumnInfo(name = COLUMN_LOCATION_ID) override val locationId: Int,
  @ColumnInfo(name = COLUMN_ALT_ICON) override val altIcon: Int,
  @ColumnInfo(name = COLUMN_USER_ICON) override val userIcon: Int,
  @ColumnInfo(name = COLUMN_FLAGS) override val flags: Long,
  @ColumnInfo(name = COLUMN_TOTAL_VALUE) val totalValue: String?,
  @ColumnInfo(name = COLUMN_POSITION, defaultValue = "0") val position: Int,
  @ColumnInfo(name = COLUMN_PROFILE_ID) override val profileId: Long
) : ChannelBase {

  val groupTotalValues: List<GroupValue>
    get() = GroupTotalValue.parse(function.value, totalValue)

  companion object {
    const val TABLE_NAME = "channelgroup"
    const val COLUMN_ID = "_id"
    const val COLUMN_REMOTE_ID = "groupid"
    const val COLUMN_CAPTION = "caption"
    const val COLUMN_ONLINE = "online"
    const val COLUMN_FUNCTION = "func"
    const val COLUMN_VISIBLE = "visible"
    const val COLUMN_LOCATION_ID = "locatonid"
    const val COLUMN_ALT_ICON = "alticon"
    const val COLUMN_USER_ICON = "usericon"
    const val COLUMN_FLAGS = "flags"
    const val COLUMN_TOTAL_VALUE = "totalvalue"
    const val COLUMN_POSITION = "position"
    const val COLUMN_PROFILE_ID = "profileid"

    val SQL = arrayOf(
      """
        CREATE TABLE $TABLE_NAME
        (
          $COLUMN_ID INTEGER PRIMARY KEY,
          $COLUMN_REMOTE_ID INTEGER NOT NULL,
          $COLUMN_CAPTION TEXT NOT NULL,
          $COLUMN_ONLINE INTEGER NOT NULL,
          $COLUMN_FUNCTION INTEGER NOT NULL,
          $COLUMN_VISIBLE INTEGER NOT NULL,
          $COLUMN_LOCATION_ID INTEGER NOT NULL,
          $COLUMN_ALT_ICON INTEGER NOT NULL,
          $COLUMN_USER_ICON INTEGER NOT NULL,
          $COLUMN_FLAGS INTEGER NOT NULL,
          $COLUMN_TOTAL_VALUE TEXT,
          $COLUMN_POSITION INTEGER NOT NULL default 0,
          $COLUMN_PROFILE_ID INTEGER NOT NULL
        )
      """.trimIndent(),
      "CREATE INDEX ${TABLE_NAME}_${COLUMN_REMOTE_ID}_index ON $TABLE_NAME ($COLUMN_REMOTE_ID)",
      "CREATE INDEX ${TABLE_NAME}_${COLUMN_LOCATION_ID}_index ON $TABLE_NAME ($COLUMN_LOCATION_ID)",
      "CREATE INDEX ${TABLE_NAME}_${COLUMN_PROFILE_ID}_index ON $TABLE_NAME ($COLUMN_PROFILE_ID)"
    )

    const val ALL_COLUMNS = "$COLUMN_ID, $COLUMN_REMOTE_ID, $COLUMN_CAPTION, $COLUMN_ONLINE, " +
      "$COLUMN_FUNCTION, $COLUMN_VISIBLE, $COLUMN_LOCATION_ID, $COLUMN_ALT_ICON, $COLUMN_USER_ICON, " +
      "$COLUMN_FLAGS, $COLUMN_TOTAL_VALUE, $COLUMN_POSITION, $COLUMN_PROFILE_ID"
  }
}
