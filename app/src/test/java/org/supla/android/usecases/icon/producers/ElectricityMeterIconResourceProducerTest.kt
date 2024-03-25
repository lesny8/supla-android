package org.supla.android.usecases.icon.producers
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

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.junit.MockitoJUnitRunner
import org.supla.android.R
import org.supla.android.data.model.general.ChannelState
import org.supla.android.lib.SuplaConst

@RunWith(MockitoJUnitRunner::class)
class ElectricityMeterIconResourceProducerTest : BaseIconResourceProducerTest() {

  @InjectMocks
  override lateinit var producer: ElectricityMeterIconResourceProducer

  @Test
  fun `should produce icon`() {
    test(
      state = ChannelState.Value.NOT_USED,
      function = SuplaConst.SUPLA_CHANNELFNC_ELECTRICITY_METER,
      expectedIcon = R.drawable.fnc_electricity_meter
    )
  }

  @Test
  fun `should produce icon (night mode)`() {
    test(
      state = ChannelState.Value.NOT_USED,
      function = SuplaConst.SUPLA_CHANNELFNC_ELECTRICITY_METER,
      expectedIcon = R.drawable.fnc_electricity_meter
    )
  }

  @Test
  fun `should produce icon (alt 1)`() {
    test(
      state = ChannelState.Value.NOT_USED,
      altIcon = 1,
      function = SuplaConst.SUPLA_CHANNELFNC_ELECTRICITY_METER,
      expectedIcon = R.drawable.powerstation
    )
  }
}
