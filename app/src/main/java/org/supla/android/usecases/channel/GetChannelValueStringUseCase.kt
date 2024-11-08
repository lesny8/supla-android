package org.supla.android.usecases.channel
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

import org.supla.android.Trace
import org.supla.android.data.ValuesFormatter
import org.supla.android.data.source.local.entity.complex.ChannelDataEntity
import org.supla.android.extensions.TAG
import org.supla.android.usecases.channel.stringvalueprovider.DepthSensorValueStringProvider
import org.supla.android.usecases.channel.stringvalueprovider.DistanceSensorValueStringProvider
import org.supla.android.usecases.channel.stringvalueprovider.ElectricityMeterValueStringProvider
import org.supla.android.usecases.channel.stringvalueprovider.GpmValueStringProvider
import org.supla.android.usecases.channel.stringvalueprovider.HumidityAndTemperatureValueStringProvider
import org.supla.android.usecases.channel.stringvalueprovider.ImpulseCounterValueStringProvider
import org.supla.android.usecases.channel.stringvalueprovider.NoValueStringProvider
import org.supla.android.usecases.channel.stringvalueprovider.SwitchWithElectricityMeterValueStringProvider
import org.supla.android.usecases.channel.stringvalueprovider.ThermometerValueStringProvider
import org.supla.core.shared.data.SuplaChannelFunction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetChannelValueStringUseCase @Inject constructor(
  thermometerValueProvider: ThermometerValueStringProvider,
  humidityAndTemperatureValueProvider: HumidityAndTemperatureValueStringProvider,
  depthSensorValueProvider: DepthSensorValueStringProvider,
  generalPurposeMeasurementValueProvider: GpmValueStringProvider,
  distanceSensorValueStringProvider: DistanceSensorValueStringProvider,
  electricityMeterValueStringProvider: ElectricityMeterValueStringProvider,
  switchWithElectricityMeterValueStringProvider: SwitchWithElectricityMeterValueStringProvider,
  impulseCounterValueStringProvider: ImpulseCounterValueStringProvider
) {

  private val providers = listOf(
    thermometerValueProvider,
    humidityAndTemperatureValueProvider,
    depthSensorValueProvider,
    generalPurposeMeasurementValueProvider,
    distanceSensorValueStringProvider,
    electricityMeterValueStringProvider,
    switchWithElectricityMeterValueStringProvider,
    impulseCounterValueStringProvider,
    NoValueStringProvider(SuplaChannelFunction.STAIRCASE_TIMER),
    NoValueStringProvider(SuplaChannelFunction.POWER_SWITCH),
    NoValueStringProvider(SuplaChannelFunction.LIGHTSWITCH)
  )

  operator fun invoke(channel: ChannelDataEntity, valueType: ValueType = ValueType.FIRST, withUnit: Boolean = true): String {
    return valueOrNull(channel, valueType, withUnit) ?: ValuesFormatter.NO_VALUE_TEXT
  }

  fun valueOrNull(channel: ChannelDataEntity, valueType: ValueType = ValueType.FIRST, withUnit: Boolean = true): String? {
    providers.firstOrNull { it.handle(channel) }?.let {
      if (channel.channelValueEntity.online.not()) {
        return ValuesFormatter.NO_VALUE_TEXT
      }

      return it.value(channel, valueType, withUnit)
    }

    Trace.e(TAG, "No value formatter for channel function `${channel.function}`")
    return null
  }
}

enum class ValueType {
  FIRST, SECOND
}

interface ChannelValueStringProvider {
  fun handle(channelData: ChannelDataEntity): Boolean
  fun value(channelData: ChannelDataEntity, valueType: ValueType, withUnit: Boolean = true): String?
}
