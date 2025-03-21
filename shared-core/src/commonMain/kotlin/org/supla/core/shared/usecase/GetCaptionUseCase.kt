package org.supla.core.shared.usecase
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

import org.supla.core.shared.data.model.general.BaseData
import org.supla.core.shared.data.model.general.Channel
import org.supla.core.shared.data.model.general.Group
import org.supla.core.shared.data.model.general.Scene
import org.supla.core.shared.data.model.general.SuplaFunction
import org.supla.core.shared.extensions.localized
import org.supla.core.shared.infrastructure.LocalizedString
import org.supla.core.shared.usecase.channel.GetChannelDefaultCaptionUseCase

class GetCaptionUseCase(
  private val getChannelDefaultCaptionUseCase: GetChannelDefaultCaptionUseCase
) {

  operator fun invoke(data: BaseData): LocalizedString {
    return when (data) {
      is Channel -> getCaption(data.caption, data.function)
      is Group -> getCaption(data.caption, data.function)
      is Scene -> data.caption.localized()
    }
  }

  private fun getCaption(caption: String, function: SuplaFunction): LocalizedString {
    return if (caption.trim().isEmpty()) {
      getChannelDefaultCaptionUseCase(function)
    } else {
      caption.localized()
    }
  }
}
