package org.supla.android.data.source
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

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import org.supla.android.data.source.local.dao.LocationDao
import org.supla.android.data.source.local.entity.LocationEntity
import org.supla.android.usecases.captionchange.CaptionChangeUseCase
import org.supla.android.usecases.developerinfo.CountProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
  private val locationDao: LocationDao
) : CountProvider, CaptionChangeUseCase.Updater {

  fun findByRemoteId(remoteId: Int) = locationDao.findByRemoteId(remoteId)

  fun updateLocation(locationEntity: LocationEntity) = locationDao.updateLocation(locationEntity)

  override fun count(): Observable<Int> = locationDao.count()

  override fun updateCaption(caption: String, remoteId: Int, profileId: Long): Completable =
    locationDao.updateCaption(caption, remoteId, profileId)
}
