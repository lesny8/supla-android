package org.supla.android.data.source.local.dao.measurements
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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import org.supla.android.data.source.local.entity.measurements.HumidityLogEntity
import org.supla.android.data.source.local.entity.measurements.HumidityLogEntity.Companion.ALL_COLUMNS
import org.supla.android.data.source.local.entity.measurements.HumidityLogEntity.Companion.COLUMN_CHANNEL_ID
import org.supla.android.data.source.local.entity.measurements.HumidityLogEntity.Companion.COLUMN_HUMIDITY
import org.supla.android.data.source.local.entity.measurements.HumidityLogEntity.Companion.COLUMN_PROFILE_ID
import org.supla.android.data.source.local.entity.measurements.HumidityLogEntity.Companion.TABLE_NAME

@Dao
interface HumidityLogDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insert(entity: List<HumidityLogEntity>): Completable

  @Query("SELECT MIN(date) FROM $TABLE_NAME WHERE $COLUMN_CHANNEL_ID = :channelId AND $COLUMN_PROFILE_ID = :profileId")
  fun findMinTimestamp(channelId: Int, profileId: Long): Single<Long>

  @Query("SELECT MAX(date) FROM $TABLE_NAME WHERE $COLUMN_CHANNEL_ID = :channelId AND $COLUMN_PROFILE_ID = :profileId")
  fun findMaxTimestamp(channelId: Int, profileId: Long): Single<Long>

  @Query(
    """
      SELECT $ALL_COLUMNS FROM $TABLE_NAME 
      WHERE $COLUMN_CHANNEL_ID = :channelId AND $COLUMN_PROFILE_ID = :profileId 
      ORDER BY date DESC 
      LIMIT 1
    """
  )
  fun findOldestEntity(channelId: Int, profileId: Long): Maybe<HumidityLogEntity>

  @Query("SELECT COUNT($COLUMN_HUMIDITY) FROM $TABLE_NAME WHERE $COLUMN_CHANNEL_ID = :channelId AND $COLUMN_PROFILE_ID = :profileId")
  fun findCount(channelId: Int, profileId: Long): Maybe<Int>

  @Query("DELETE FROM $TABLE_NAME WHERE $COLUMN_CHANNEL_ID = :channelId AND $COLUMN_PROFILE_ID = :profileId")
  fun delete(channelId: Int, profileId: Long): Completable

  @Query(
    """
      SELECT $ALL_COLUMNS FROM $TABLE_NAME
      WHERE $COLUMN_CHANNEL_ID = :channelId AND $COLUMN_PROFILE_ID = :profileId AND date >= :startDate AND date <= :endDate
      ORDER BY date asc
    """
  )
  fun findMeasurements(channelId: Int, profileId: Long, startDate: Long, endDate: Long): Observable<List<HumidityLogEntity>>
}