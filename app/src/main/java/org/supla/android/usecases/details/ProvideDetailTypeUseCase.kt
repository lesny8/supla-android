package org.supla.android.usecases.details

import org.supla.android.db.Channel
import org.supla.android.db.ChannelBase
import org.supla.android.features.standarddetail.DetailPage
import org.supla.android.lib.SuplaChannelValue
import org.supla.android.lib.SuplaConst
import org.supla.android.lib.SuplaConst.SUPLA_CHANNEL_FLAG_COUNTDOWN_TIMER_SUPPORTED
import java.io.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProvideDetailTypeUseCase @Inject constructor() {

  operator fun invoke(channelBase: ChannelBase): DetailType? = when (channelBase.func) {
    SuplaConst.SUPLA_CHANNELFNC_DIMMER,
    SuplaConst.SUPLA_CHANNELFNC_DIMMERANDRGBLIGHTING,
    SuplaConst.SUPLA_CHANNELFNC_RGBLIGHTING ->
      LegacyDetailType.RGBW
    SuplaConst.SUPLA_CHANNELFNC_CONTROLLINGTHEROLLERSHUTTER,
    SuplaConst.SUPLA_CHANNELFNC_CONTROLLINGTHEROOFWINDOW ->
      LegacyDetailType.RS
    SuplaConst.SUPLA_CHANNELFNC_LIGHTSWITCH,
    SuplaConst.SUPLA_CHANNELFNC_POWERSWITCH,
    SuplaConst.SUPLA_CHANNELFNC_STAIRCASETIMER -> {
      StandardDetailType(getSwitchDetailPages(channelBase))
    }
    SuplaConst.SUPLA_CHANNELFNC_ELECTRICITY_METER ->
      LegacyDetailType.EM
    SuplaConst.SUPLA_CHANNELFNC_IC_ELECTRICITY_METER,
    SuplaConst.SUPLA_CHANNELFNC_IC_GAS_METER,
    SuplaConst.SUPLA_CHANNELFNC_IC_WATER_METER,
    SuplaConst.SUPLA_CHANNELFNC_IC_HEAT_METER ->
      LegacyDetailType.IC
    SuplaConst.SUPLA_CHANNELFNC_THERMOMETER ->
      LegacyDetailType.TEMPERATURE
    SuplaConst.SUPLA_CHANNELFNC_HUMIDITYANDTEMPERATURE ->
      LegacyDetailType.TEMPERATURE_HUMIDITY
    SuplaConst.SUPLA_CHANNELFNC_THERMOSTAT_HEATPOL_HOMEPLUS ->
      LegacyDetailType.THERMOSTAT_HP
    SuplaConst.SUPLA_CHANNELFNC_DIGIGLASS_VERTICAL,
    SuplaConst.SUPLA_CHANNELFNC_DIGIGLASS_HORIZONTAL ->
      LegacyDetailType.DIGIGLASS
    else -> null
  }

  private fun getSwitchDetailPages(channelBase: ChannelBase): List<DetailPage> {
    return if (channelBase is Channel) {
      val list = mutableListOf(DetailPage.GENERAL)
      if (channelBase.value?.subValueType == SuplaChannelValue.SUBV_TYPE_IC_MEASUREMENTS.toShort()) {
        list.add(DetailPage.HISTORY_IC)
      } else if (channelBase.value?.subValueType == SuplaChannelValue.SUBV_TYPE_ELECTRICITY_MEASUREMENTS.toShort()) {
        list.add(DetailPage.HISTORY_EM)
      }
      if (channelBase.flags.and(SUPLA_CHANNEL_FLAG_COUNTDOWN_TIMER_SUPPORTED) > 0) {
        list.add(DetailPage.TIMER)
      }
      list
    } else {
      listOf(DetailPage.GENERAL)
    }
  }
}

sealed interface DetailType : Serializable

enum class LegacyDetailType : DetailType {
  RGBW,
  RS,
  IC,
  EM,
  TEMPERATURE,
  TEMPERATURE_HUMIDITY,
  THERMOSTAT_HP,
  DIGIGLASS
}

data class StandardDetailType(
  val pages: List<DetailPage>
) : DetailType
