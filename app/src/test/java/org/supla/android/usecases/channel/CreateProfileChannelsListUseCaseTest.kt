package org.supla.android.usecases.channel

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.reactivex.rxjava3.core.Single
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.supla.android.data.ValuesFormatter
import org.supla.android.data.source.ChannelRelationRepository
import org.supla.android.data.source.RoomChannelRepository
import org.supla.android.data.source.local.entity.ChannelRelationEntity
import org.supla.android.data.source.local.entity.complex.ChannelChildEntity
import org.supla.android.data.source.local.entity.complex.ChannelDataEntity
import org.supla.android.images.ImageId
import org.supla.android.lib.SuplaConst.SUPLA_CHANNELFNC_DEPTHSENSOR
import org.supla.android.lib.SuplaConst.SUPLA_CHANNELFNC_GENERAL_PURPOSE_MEASUREMENT
import org.supla.android.lib.SuplaConst.SUPLA_CHANNELFNC_GENERAL_PURPOSE_METER
import org.supla.android.lib.SuplaConst.SUPLA_CHANNELFNC_HUMIDITY
import org.supla.android.lib.SuplaConst.SUPLA_CHANNELFNC_HVAC_THERMOSTAT
import org.supla.android.lib.SuplaConst.SUPLA_CHANNEL_FLAG_HAS_PARENT
import org.supla.android.ui.lists.ListItem
import org.supla.android.ui.lists.data.IssueIconType
import org.supla.android.usecases.icon.GetChannelIconUseCase
import org.supla.android.usecases.location.CollapsedFlag

@RunWith(MockitoJUnitRunner::class)
class CreateProfileChannelsListUseCaseTest {

  @Mock
  private lateinit var channelRelationRepository: ChannelRelationRepository

  @Mock
  private lateinit var channelRepository: RoomChannelRepository

  @Mock
  private lateinit var getChannelCaptionUseCase: GetChannelCaptionUseCase

  @Mock
  private lateinit var getChannelIconUseCase: GetChannelIconUseCase

  @Mock
  private lateinit var getChannelValueStringUseCase: GetChannelValueStringUseCase

  @Mock
  private lateinit var valuesFormatter: ValuesFormatter

  @InjectMocks
  private lateinit var usecase: CreateProfileChannelsListUseCase

  @Test
  fun `should create list of channels and locations`() {
    // given
    val first = mockListEntity(11, 12)
    val second = mockListEntity(21, 12, function = SUPLA_CHANNELFNC_HVAC_THERMOSTAT)
    val third = mockListEntity(31, 32, locationCollapsed = true)
    val fourth = mockListEntity(41, 42, function = SUPLA_CHANNELFNC_DEPTHSENSOR)

    whenever(channelRepository.findList()).thenReturn(Single.just(listOf(first, second, third, fourth)))
    whenever(channelRelationRepository.findChildrenToParentsRelations()).thenReturn(Single.just(emptyMap()))

    // when
    val testObserver = usecase().test()

    // then
    testObserver.assertComplete()
    val context: Context = mockk()
    val list = testObserver.values()[0]

    assertThat(list).hasSize(6)
    assertThat(list[0]).isInstanceOf(ListItem.LocationItem::class.java)
    assertThat(list[1]).isInstanceOf(ListItem.ChannelItem::class.java)
    assertThat(list[2]).isInstanceOf(ListItem.HvacThermostatItem::class.java)
    assertThat(list[3]).isInstanceOf(ListItem.LocationItem::class.java)
    assertThat(list[4]).isInstanceOf(ListItem.LocationItem::class.java)
    assertThat(list[5]).isInstanceOf(ListItem.MeasurementItem::class.java)

    assertThat((list[1] as ListItem.ChannelItem).channelBase.remoteId).isEqualTo(11)
    assertThat((list[2] as ListItem.HvacThermostatItem).captionProvider(context)).isEqualTo("caption 21")
    assertThat((list[5] as ListItem.MeasurementItem).captionProvider(context)).isEqualTo("caption 41")

    assertThat((list[0] as ListItem.LocationItem).location.caption).isEqualTo("12")
    assertThat((list[3] as ListItem.LocationItem).location.caption).isEqualTo("32")
    assertThat((list[4] as ListItem.LocationItem).location.caption).isEqualTo("42")
  }

  @Test
  fun `should merge location with same name into one`() {
    // given
    val first = mockListEntity(11, 12, function = SUPLA_CHANNELFNC_GENERAL_PURPOSE_METER)
    val second = mockListEntity(21, 12, function = SUPLA_CHANNELFNC_GENERAL_PURPOSE_MEASUREMENT)
    val third = mockListEntity(31, 32, locationName = "12")
    val fourth = mockListEntity(41, 42)

    whenever(channelRepository.findList()).thenReturn(Single.just(listOf(first, second, third, fourth)))
    whenever(channelRelationRepository.findChildrenToParentsRelations()).thenReturn(Single.just(emptyMap()))

    // when
    val testObserver = usecase().test()

    // then
    testObserver.assertComplete()
    val list = testObserver.values()[0]

    assertThat(list).hasSize(6)
    assertThat(list[0]).isInstanceOf(ListItem.LocationItem::class.java)
    assertThat(list[1]).isInstanceOf(ListItem.GeneralPurposeMeterItem::class.java)
    assertThat(list[2]).isInstanceOf(ListItem.GeneralPurposeMeasurementItem::class.java)
    assertThat(list[3]).isInstanceOf(ListItem.ChannelItem::class.java)
    assertThat(list[4]).isInstanceOf(ListItem.LocationItem::class.java)
    assertThat(list[5]).isInstanceOf(ListItem.ChannelItem::class.java)

    assertThat((list[1] as ListItem.GeneralPurposeMeterItem).value).isEqualTo("value 11")
    assertThat((list[2] as ListItem.GeneralPurposeMeasurementItem).value).isEqualTo("value 21")
    assertThat((list[3] as ListItem.ChannelItem).channelBase.remoteId).isEqualTo(31)
    assertThat((list[5] as ListItem.ChannelItem).channelBase.remoteId).isEqualTo(41)

    assertThat((list[0] as ListItem.LocationItem).location.caption).isEqualTo("12")
    assertThat((list[4] as ListItem.LocationItem).location.caption).isEqualTo("42")
  }

  @Test
  fun `should load children`() {
    // given
    val first = mockListEntity(11, 12)
    val second = mockListEntity(21, 12, channelFlags = SUPLA_CHANNEL_FLAG_HAS_PARENT)
    val third = mockListEntity(31, 12)

    whenever(channelRepository.findList()).thenReturn(Single.just(listOf(first, second, third)))
    val childrenRelation = mockk<ChannelRelationEntity> { every { channelId } returns 21 }
    whenever(channelRelationRepository.findChildrenToParentsRelations()).thenReturn(
      Single.just(mapOf(11 to listOf(childrenRelation)))
    )

    // when
    val testObserver = usecase().test()

    // then
    testObserver.assertComplete()
    val list = testObserver.values()[0]

    assertThat(list).hasSize(3)
    assertThat(list[0]).isInstanceOf(ListItem.LocationItem::class.java)
    assertThat(list[1]).isInstanceOf(ListItem.ChannelItem::class.java)
    assertThat(list[2]).isInstanceOf(ListItem.ChannelItem::class.java)
    assertThat((list[1] as ListItem.ChannelItem).children).isEqualTo(listOf(ChannelChildEntity(childrenRelation, second)))

    assertThat((list[1] as ListItem.ChannelItem).channelBase.remoteId).isEqualTo(11)
    assertThat((list[2] as ListItem.ChannelItem).channelBase.remoteId).isEqualTo(31)

    assertThat((list[0] as ListItem.LocationItem).location.caption).isEqualTo("12")
  }

  private fun mockListEntity(
    channelRemoteId: Int,
    locationRemoteId: Int,
    locationName: String = "$locationRemoteId",
    channelFlags: Long = 0,
    locationCollapsed: Boolean = false,
    function: Int = SUPLA_CHANNELFNC_HUMIDITY
  ): ChannelDataEntity = mockk {
    every { remoteId } returns channelRemoteId
    every { locationEntity } returns mockk {
      every { remoteId } returns locationRemoteId
      every { caption } returns locationName
      every { isCollapsed(CollapsedFlag.CHANNEL) } returns locationCollapsed
      every { toLegacyLocation() } returns mockk {
        every { caption } returns locationName
      }
    }
    every { channelEntity } returns mockk {
      every { flags } returns channelFlags
      every { this@mockk.function } returns function
      every { remoteId } returns channelRemoteId
    }
    every { getLegacyChannel() } returns mockk {
      every { remoteId } returns channelRemoteId
    }
    every { channelValueEntity } returns mockk {
      every { online } returns true
      if (function == SUPLA_CHANNELFNC_HVAC_THERMOSTAT) {
        every { asThermostatValue() } returns mockk {
          every { getSetpointText(valuesFormatter) } returns "setpoint text"
          every { getIndicatorIcon() } returns 123
          every { getIssueIconType() } returns IssueIconType.WARNING
          every { getIssueMessage() } returns 456
        }
      }
    }

    if (function == SUPLA_CHANNELFNC_HVAC_THERMOSTAT) {
      every { channelExtendedValueEntity } returns mockk {
        every { getSuplaValue() } returns null
      }
    }

    whenever(getChannelCaptionUseCase.invoke(this)).thenReturn { "caption $channelRemoteId" }
    whenever(getChannelValueStringUseCase.invoke(this)).thenReturn("value $channelRemoteId")
    whenever(getChannelIconUseCase.invoke(this)).thenReturn(ImageId(channelRemoteId))
  }
}
