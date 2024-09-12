package org.supla.android.features.details.switchdetail.switchdetail
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

import io.mockk.MockKAnnotations
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.Before
import org.junit.Test
import org.supla.android.R
import org.supla.android.core.BaseViewModelTest
import org.supla.android.core.infrastructure.DateProvider
import org.supla.android.core.ui.BitmapProvider
import org.supla.android.data.model.general.ChannelState
import org.supla.android.data.source.local.entity.ChannelExtendedValueEntity
import org.supla.android.data.source.local.entity.complex.ChannelDataEntity
import org.supla.android.data.source.local.entity.complex.ChannelGroupDataEntity
import org.supla.android.data.source.runtime.ItemType
import org.supla.android.events.DownloadEventsManager
import org.supla.android.features.details.detailbase.electricitymeter.ElectricityMeterGeneralStateHandler
import org.supla.android.features.details.switchdetail.general.SwitchGeneralViewEvent
import org.supla.android.features.details.switchdetail.general.SwitchGeneralViewModel
import org.supla.android.features.details.switchdetail.general.SwitchGeneralViewState
import org.supla.android.lib.SuplaChannelExtendedValue
import org.supla.android.lib.SuplaConst.SUPLA_CHANNELFNC_LIGHTSWITCH
import org.supla.android.lib.SuplaConst.SUPLA_CHANNELFNC_POWERSWITCH
import org.supla.android.lib.actions.ActionId
import org.supla.android.testhelpers.extensions.extract
import org.supla.android.testhelpers.extensions.extractResId
import org.supla.android.tools.SuplaSchedulers
import org.supla.android.usecases.channel.DownloadChannelMeasurementsUseCase
import org.supla.android.usecases.channel.GetChannelStateUseCase
import org.supla.android.usecases.channel.GetChannelValueUseCase
import org.supla.android.usecases.channel.ReadChannelByRemoteIdUseCase
import org.supla.android.usecases.channel.electricitymeter.LoadElectricityMeterMeasurementsUseCase
import org.supla.android.usecases.client.ExecuteSimpleActionUseCase
import org.supla.android.usecases.group.ReadChannelGroupByRemoteIdUseCase
import org.supla.android.usecases.icon.GetChannelIconUseCase
import java.util.Date

@Suppress("UnusedLambdaExpressionBody")
class SwitchGeneralViewModelTest :
  BaseViewModelTest<SwitchGeneralViewState, SwitchGeneralViewEvent, SwitchGeneralViewModel>(MockSchedulers.MOCKK) {

  @MockK
  override lateinit var schedulers: SuplaSchedulers

  @MockK
  private lateinit var readChannelByRemoteIdUseCase: ReadChannelByRemoteIdUseCase

  @MockK
  private lateinit var readChannelGroupByRemoteIdUseCase: ReadChannelGroupByRemoteIdUseCase

  @MockK
  private lateinit var getChannelStateUseCase: GetChannelStateUseCase

  @MockK
  private lateinit var executeSimpleActionUseCase: ExecuteSimpleActionUseCase

  @MockK
  private lateinit var dateProvider: DateProvider

  @MockK
  private lateinit var getChannelIconUseCase: GetChannelIconUseCase

  @MockK
  private lateinit var loadElectricityMeterMeasurementsUseCase: LoadElectricityMeterMeasurementsUseCase

  @MockK
  private lateinit var downloadEventsManager: DownloadEventsManager

  @MockK
  private lateinit var downloadChannelMeasurementsUseCase: DownloadChannelMeasurementsUseCase

  @MockK
  private lateinit var getChannelValueUseCase: GetChannelValueUseCase

  @MockK
  private lateinit var electricityMeterGeneralStateHandler: ElectricityMeterGeneralStateHandler

  @InjectMockKs
  override lateinit var viewModel: SwitchGeneralViewModel

  @Before
  override fun setUp() {
    MockKAnnotations.init(this)
    super.setUp()
  }

  @Test
  fun `should load channel`() {
    // given
    val remoteId = 123
    val function = SUPLA_CHANNELFNC_POWERSWITCH
    val channelData: ChannelDataEntity = mockChannelData(remoteId, function)
    val stateIcon: BitmapProvider = mockk()
    val onIcon: BitmapProvider = mockk()
    val offIcon: BitmapProvider = mockk()

    every { readChannelByRemoteIdUseCase.invoke(remoteId) } returns Maybe.just(channelData)
    every { getChannelStateUseCase.invoke(channelData) } returns mockk { every { isActive() } returns true }
    every { getChannelIconUseCase.getIconProvider(channelData) } returns stateIcon
    every { getChannelIconUseCase.getIconProvider(channelData, channelStateValue = ChannelState.Value.ON) } returns onIcon
    every { getChannelIconUseCase.getIconProvider(channelData, channelStateValue = ChannelState.Value.OFF) } returns offIcon
    every { dateProvider.currentDate() } returns Date()
    every { electricityMeterGeneralStateHandler.updateState(any(), any(), any()) } answers { firstArg() }

    // when
    viewModel.loadData(remoteId, ItemType.CHANNEL)

    // then
    assertThat(events).isEmpty()
    assertThat(states)
      .extracting(
        { it.online },
        { it.deviceStateLabel.extractResId() },
        { it.deviceStateIcon },
        { it.deviceStateValue },
        { it.onIcon },
        { it.offIcon },
        { it.electricityMeterState }
      )
      .containsExactly(
        tuple(true, R.string.details_timer_state_label, stateIcon, R.string.details_timer_device_on, onIcon, offIcon, null)
      )

    verify {
      readChannelByRemoteIdUseCase.invoke(remoteId)
      getChannelStateUseCase.invoke(channelData)
      getChannelIconUseCase.getIconProvider(channelData)
      getChannelIconUseCase.getIconProvider(channelData, channelStateValue = ChannelState.Value.ON)
      getChannelIconUseCase.getIconProvider(channelData, channelStateValue = ChannelState.Value.OFF)
      dateProvider.currentDate()
    }
    confirmVerified(
      readChannelByRemoteIdUseCase,
      getChannelStateUseCase,
      getChannelIconUseCase,
      dateProvider,
      loadElectricityMeterMeasurementsUseCase,
      readChannelGroupByRemoteIdUseCase
    )
  }

  @Test
  fun `should load group`() {
    // given
    val remoteId = 123
    val function = SUPLA_CHANNELFNC_POWERSWITCH
    val group: ChannelGroupDataEntity = mockk {
      every { this@mockk.function } returns function
      every { this@mockk.remoteId } returns remoteId
      every { isOnline() } returns true
    }
    val stateIcon: BitmapProvider = mockk()
    val onIcon: BitmapProvider = mockk()
    val offIcon: BitmapProvider = mockk()

    every { readChannelGroupByRemoteIdUseCase.invoke(remoteId) } returns Maybe.just(group)
    every { getChannelStateUseCase.invoke(group) } returns mockk { every { isActive() } returns true }
    every { getChannelIconUseCase.getIconProvider(group) } returns stateIcon
    every { getChannelIconUseCase.getIconProvider(group, channelStateValue = ChannelState.Value.ON) } returns onIcon
    every { getChannelIconUseCase.getIconProvider(group, channelStateValue = ChannelState.Value.OFF) } returns offIcon

    // when
    viewModel.loadData(remoteId, ItemType.GROUP)

    // then
    assertThat(events).isEmpty()
    assertThat(states)
      .extracting(
        { it.online },
        { it.deviceStateLabel.extractResId() },
        { it.deviceStateIcon },
        { it.deviceStateValue },
        { it.onIcon },
        { it.offIcon },
        { it.electricityMeterState }
      )
      .containsExactly(
        tuple(true, R.string.details_timer_state_label, stateIcon, R.string.details_timer_device_on, onIcon, offIcon, null)
      )

    verify {
      readChannelGroupByRemoteIdUseCase.invoke(remoteId)
      getChannelStateUseCase.invoke(group)
      getChannelIconUseCase.getIconProvider(group)
      getChannelIconUseCase.getIconProvider(group, channelStateValue = ChannelState.Value.ON)
      getChannelIconUseCase.getIconProvider(group, channelStateValue = ChannelState.Value.OFF)
    }
    confirmVerified(
      readChannelByRemoteIdUseCase,
      getChannelStateUseCase,
      getChannelIconUseCase,
      dateProvider,
      loadElectricityMeterMeasurementsUseCase,
      readChannelGroupByRemoteIdUseCase
    )
  }

  @Test
  fun `should turn on channel`() {
    // given
    val remoteId = 123
    val itemType = ItemType.CHANNEL

    every { executeSimpleActionUseCase(ActionId.TURN_ON, itemType.subjectType, remoteId) } returns Completable.complete()

    // when
    viewModel.turnOn(remoteId, itemType)

    // then
    verify {
      executeSimpleActionUseCase(ActionId.TURN_ON, itemType.subjectType, remoteId)
    }
    confirmVerified(
      readChannelByRemoteIdUseCase,
      getChannelStateUseCase,
      getChannelIconUseCase,
      dateProvider,
      loadElectricityMeterMeasurementsUseCase,
      readChannelGroupByRemoteIdUseCase
    )
  }

  @Test
  fun `should turn off group`() {
    // given
    val remoteId = 123
    val itemType = ItemType.GROUP

    every { executeSimpleActionUseCase(ActionId.TURN_OFF, itemType.subjectType, remoteId) } returns Completable.complete()

    // when
    viewModel.turnOff(remoteId, itemType)

    // then
    verify {
      executeSimpleActionUseCase(ActionId.TURN_OFF, itemType.subjectType, remoteId)
    }
    confirmVerified(
      readChannelByRemoteIdUseCase,
      getChannelStateUseCase,
      getChannelIconUseCase,
      dateProvider,
      loadElectricityMeterMeasurementsUseCase,
      readChannelGroupByRemoteIdUseCase
    )
  }

  @Test
  fun `should load estimated count down end time`() {
    // given
    val remoteId = 123
    val function = SUPLA_CHANNELFNC_LIGHTSWITCH
    val stateIcon: BitmapProvider = mockk()
    val onIcon: BitmapProvider = mockk()
    val offIcon: BitmapProvider = mockk()

    val estimatedEndDate = Date(1000)
    every { dateProvider.currentDate() } returns Date(100)

    val channelData: ChannelDataEntity = mockChannelData(remoteId, function, estimatedEndDate)

    every { readChannelByRemoteIdUseCase.invoke(remoteId) } returns Maybe.just(channelData)
    every { getChannelStateUseCase.invoke(channelData) } returns mockk { every { isActive() } returns true }
    every { getChannelIconUseCase.getIconProvider(channelData) } returns stateIcon
    every { getChannelIconUseCase.getIconProvider(channelData, channelStateValue = ChannelState.Value.ON) } returns onIcon
    every { getChannelIconUseCase.getIconProvider(channelData, channelStateValue = ChannelState.Value.OFF) } returns offIcon
    every { electricityMeterGeneralStateHandler.updateState(any(), any(), any()) } answers { firstArg() }

    // when
    viewModel.loadData(remoteId, ItemType.CHANNEL)

    // then
    assertThat(events).isEmpty()
    assertThat(states)
      .extracting(
        { it.online },
        { it.deviceStateLabel.extract() },
        { it.deviceStateIcon },
        { it.deviceStateValue },
        { it.onIcon },
        { it.offIcon },
        { it.electricityMeterState }
      )
      .containsExactly(
        tuple(
          true,
          listOf(R.string.hour_string_format, R.string.details_timer_state_label_for_timer),
          stateIcon,
          R.string.details_timer_device_on,
          onIcon,
          offIcon,
          null
        )
      )

    verify {
      readChannelByRemoteIdUseCase.invoke(remoteId)
      getChannelStateUseCase.invoke(channelData)
      getChannelIconUseCase.getIconProvider(channelData)
      getChannelIconUseCase.getIconProvider(channelData, channelStateValue = ChannelState.Value.ON)
      getChannelIconUseCase.getIconProvider(channelData, channelStateValue = ChannelState.Value.OFF)
      dateProvider.currentDate()
    }
    confirmVerified(
      readChannelByRemoteIdUseCase,
      getChannelStateUseCase,
      getChannelIconUseCase,
      dateProvider,
      loadElectricityMeterMeasurementsUseCase,
      readChannelGroupByRemoteIdUseCase
    )
  }

  @Test
  fun `shouldn't load estimated countdown end time when time elapsed`() {
    // given
    val remoteId = 123
    val function = SUPLA_CHANNELFNC_LIGHTSWITCH
    val stateIcon: BitmapProvider = mockk()
    val onIcon: BitmapProvider = mockk()
    val offIcon: BitmapProvider = mockk()

    val estimatedEndDate = Date(1000)
    every { dateProvider.currentDate() } returns Date(1003)

    val channelData: ChannelDataEntity = mockChannelData(remoteId, function, estimatedEndDate)
    every { readChannelByRemoteIdUseCase.invoke(remoteId) } returns Maybe.just(channelData)
    every { getChannelStateUseCase.invoke(channelData) } returns mockk { every { isActive() } returns true }
    every { getChannelIconUseCase.getIconProvider(channelData) } returns stateIcon
    every { getChannelIconUseCase.getIconProvider(channelData, channelStateValue = ChannelState.Value.ON) } returns onIcon
    every { getChannelIconUseCase.getIconProvider(channelData, channelStateValue = ChannelState.Value.OFF) } returns offIcon
    every { electricityMeterGeneralStateHandler.updateState(any(), any(), any()) } answers { firstArg() }

    // when
    viewModel.loadData(remoteId, ItemType.CHANNEL)

    // then
    assertThat(events).isEmpty()
    assertThat(states)
      .extracting(
        { it.online },
        { it.deviceStateLabel.extractResId() },
        { it.deviceStateIcon },
        { it.deviceStateValue },
        { it.onIcon },
        { it.offIcon },
        { it.electricityMeterState }
      )
      .containsExactly(
        tuple(true, R.string.details_timer_state_label, stateIcon, R.string.details_timer_device_on, onIcon, offIcon, null)
      )

    verify {
      readChannelByRemoteIdUseCase.invoke(remoteId)
      getChannelStateUseCase.invoke(channelData)
      getChannelIconUseCase.getIconProvider(channelData)
      getChannelIconUseCase.getIconProvider(channelData, channelStateValue = ChannelState.Value.ON)
      getChannelIconUseCase.getIconProvider(channelData, channelStateValue = ChannelState.Value.OFF)
      dateProvider.currentDate()
    }
    confirmVerified(
      readChannelByRemoteIdUseCase,
      getChannelStateUseCase,
      getChannelIconUseCase,
      dateProvider,
      loadElectricityMeterMeasurementsUseCase,
      readChannelGroupByRemoteIdUseCase
    )
  }

  private fun mockTimerState(date: Date): ChannelExtendedValueEntity {
    val suplaExtendedValue: SuplaChannelExtendedValue = mockk()
    every { suplaExtendedValue.timerEstimatedEndDate } returns date

    val extendedValue: ChannelExtendedValueEntity = mockk()
    every { extendedValue.getSuplaValue() } returns suplaExtendedValue

    return extendedValue
  }

  private fun mockChannelData(remoteId: Int, function: Int, estimatedEndDate: Date? = null): ChannelDataEntity {
    return mockk {
      every { this@mockk.function } returns function
      every { this@mockk.remoteId } returns remoteId
      every { isOnline() } returns true
      every { channelExtendedValueEntity } returns estimatedEndDate?.let { mockTimerState(estimatedEndDate) }
      every { channelValueEntity } returns mockk {
        every { subValueType } returns 0
      }
      every { channelEntity } returns mockk {
        every { this@mockk.function } returns function
      }
    }
  }
}
