package org.supla.android.usecases.list.eventmappers
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

import org.supla.android.data.ValuesFormatter
import org.supla.android.data.source.local.entity.ChannelRelationType
import org.supla.android.extensions.guardLet
import org.supla.android.extensions.isHvacThermostat
import org.supla.android.extensions.toThermostatSlideableListItemData
import org.supla.android.ui.lists.data.SlideableListItemData
import org.supla.android.usecases.channel.ChannelWithChildren
import org.supla.android.usecases.list.CreateListItemUpdateEventDataUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelWithChildrenToThermostatUpdateEventMapper @Inject constructor(private val valuesFormatter: ValuesFormatter) :
  CreateListItemUpdateEventDataUseCase.Mapper {
  override fun handle(item: Any): Boolean {
    return (item as? ChannelWithChildren)?.channel?.isHvacThermostat() == true
  }

  override fun map(item: Any): SlideableListItemData {
    val (channel) = guardLet(item as? ChannelWithChildren) {
      throw IllegalArgumentException("Expected Channel but got $item")
    }

    val mainThermometerChild = channel.children.firstOrNull { it.relationType == ChannelRelationType.MAIN_THERMOMETER }
    return channel.channel.toThermostatSlideableListItemData(mainThermometerChild?.channel, valuesFormatter)
  }
}
