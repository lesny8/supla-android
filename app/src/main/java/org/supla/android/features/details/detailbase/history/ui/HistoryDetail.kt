@file:OptIn(ExperimentalMaterial3Api::class)

package org.supla.android.features.details.detailbase.history.ui
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

import androidx.annotation.DimenRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.supla.android.R
import org.supla.android.core.ui.BaseViewProxy
import org.supla.android.core.ui.theme.Distance
import org.supla.android.core.ui.theme.SuplaTheme
import org.supla.android.data.formatting.LocalDateFormatter
import org.supla.android.data.model.chart.ChannelChartSets
import org.supla.android.data.model.chart.ChartDataAggregation
import org.supla.android.data.model.chart.ChartEntryType
import org.supla.android.data.model.chart.ChartRange
import org.supla.android.data.model.chart.DateRange
import org.supla.android.data.model.chart.HistoryDataSet
import org.supla.android.data.model.chart.datatype.CombinedChartData
import org.supla.android.data.model.chart.datatype.LineChartData
import org.supla.android.data.model.chart.datatype.PieChartData
import org.supla.android.data.model.chart.singleLabel
import org.supla.android.data.model.chart.style.ChartStyle
import org.supla.android.data.model.chart.style.ThermometerChartStyle
import org.supla.android.data.model.general.RangeValueType
import org.supla.android.data.source.local.calendar.Hour
import org.supla.android.extensions.weekEnd
import org.supla.android.extensions.weekStart
import org.supla.android.features.details.detailbase.history.HistoryDetailViewState
import org.supla.android.images.ImageId
import org.supla.android.ui.dialogs.DatePickerDialog
import org.supla.android.ui.dialogs.TimePickerDialog
import org.supla.android.ui.views.Image
import org.supla.android.ui.views.SpinnerItem
import org.supla.android.ui.views.TextField
import org.supla.android.ui.views.TextSpinner
import org.supla.android.ui.views.buttons.IconButton
import org.supla.android.ui.views.charts.CombinedChart
import org.supla.android.ui.views.charts.PieChart
import org.supla.android.ui.views.tools.Shadow
import org.supla.android.ui.views.tools.ShadowOrientation
import org.supla.android.usecases.channel.valueformatter.HumidityValueFormatter
import org.supla.core.shared.data.model.general.SuplaFunction
import java.util.Date

interface HistoryDetailProxy : BaseViewProxy<HistoryDetailViewState> {
  fun refresh() {}
  fun showSelection(remoteId: Int, type: ChartEntryType) {}
  fun confirmSelection(spinnerItem: SpinnerItem?, checkboxItems: Set<CheckboxItem>?) {}
  fun hideSelection() {}
  fun changeFilter(spinnerItem: SpinnerItem) {}
  fun moveRangeLeft() {}
  fun moveRangeRight() {}
  fun moveToDataBegin() {}
  fun moveToDataEnd() {}
  fun updateChartPosition(scaleX: Float, scaleY: Float, x: Float, y: Float) {}
  fun customRangeEditDate(type: RangeValueType) {}
  fun customRangeEditHour(type: RangeValueType) {}
  fun customRangeEditDateDismiss() {}
  fun customRangeEditHourDismiss() {}
  fun customRangeEditDateSave(date: Date) {}
  fun customRangeEditHourSave(hour: Hour) {}
  fun chartStyle(): ChartStyle = ThermometerChartStyle
}

@Composable
fun HistoryDetail(viewModel: HistoryDetailProxy) {
  val viewState by viewModel.getViewState().collectAsState()

  if (viewState.editDate != null) {
    DatePickerDialog(
      selectedDate = viewState.editDateValue,
      onConfirmTap = { viewModel.customRangeEditDateSave(it) },
      onDismissTap = { viewModel.customRangeEditDateDismiss() },
      dateValidator = { viewState.editDayValidator(it) },
      yearRange = viewState.yearRange
    )
  }
  if (viewState.editHour != null) {
    TimePickerDialog(
      selectedHour = viewState.editHourValue,
      onConfirmTap = { viewModel.customRangeEditHourSave(it) },
      onDismissTap = { viewModel.customRangeEditHourDismiss() }
    )
  }
  viewState.chartDataSelectionDialogState?.let {
    ChartDataSelectionDialog(
      state = it,
      onDismiss = { viewModel.hideSelection() },
      onPositiveClick = viewModel::confirmSelection
    )
  }

  Column(
    modifier = Modifier.fillMaxSize()
  ) {
    DataSetsAndFilters(viewState = viewState, viewModel = viewModel)

    if (viewState.showHistory) {
      when (val data = viewState.chartData) {
        is CombinedChartData ->
          CombinedChart(
            data = data,
            channelFunction = viewState.channelFunction,
            emptyChartMessage = viewState.emptyChartMessage(LocalContext.current),
            withRightAxis = viewState.withRightAxis,
            withLeftAxis = viewState.withLeftAxis,
            maxLeftAxis = viewState.maxLeftAxis,
            maxRightAxis = viewState.maxRightAxis,
            chartParametersProvider = { viewState.chartParameters?.getOptional() },
            positionEvents = viewModel::updateChartPosition,
            chartStyle = viewModel.chartStyle(),
            modifier = Modifier
              .weight(1f)
              .padding(horizontal = Distance.tiny)
          )

        is PieChartData ->
          PieChart(
            data = data,
            emptyChartMessage = viewState.emptyChartMessage(LocalContext.current),
            chartStyle = viewModel.chartStyle(),
            modifier = Modifier
              .weight(1f)
              .padding(horizontal = Distance.tiny)
          )
      }

      if (viewState.filters.selectedRange == ChartRange.CUSTOM) {
        RangeSelection(viewState, viewModel)
      } else {
        BottomPagination(viewState = viewState, viewModel = viewModel)
      }
    } else {
      Text(
        text = stringResource(id = R.string.history_disabled),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
          .fillMaxWidth()
          .padding(all = Distance.default)
      )
    }
  }
}

@Composable
private fun DataSetsAndFilters(viewState: HistoryDetailViewState, viewModel: HistoryDetailProxy) {
  val pullToRefreshState = rememberPullToRefreshState()

  Box(
    modifier = Modifier
      .pullToRefresh(
        isRefreshing = viewState.loading,
        onRefresh = { viewModel.refresh() },
        state = pullToRefreshState,
        threshold = 58.dp
      )
      .fillMaxWidth()
  ) {
    // Vertical scroll is needed to make pull refresh working
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
      DataSetsRow {
        viewState.chartData.sets.forEach { data ->
          Column(
            modifier = Modifier
              .padding(start = Distance.default, end = Distance.default)
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                enabled = data.dataSets.size == 1,
                indication = ripple(),
                onClick = { viewModel.showSelection(data.remoteId, data.dataSets[0].type) }
              ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            data.typeName?.let {
              Text(text = it(LocalContext.current), style = MaterialTheme.typography.labelSmall)
            }
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              data.dataSets.forEach { set ->
                DataSetItems(
                  label = set.label,
                  active = set.active,
                  historyEnabled = viewState.showHistory,
                  clickEnabled = data.dataSets.size > 1,
                  onClick = { viewModel.showSelection(data.remoteId, set.type) }
                )
              }
            }
          }
          Box(
            modifier = Modifier
              .fillMaxHeight()
              .width(1.dp)
              .background(MaterialTheme.colorScheme.background)
          )
        }
      }
      Shadow(orientation = ShadowOrientation.STARTING_TOP)
      if (viewState.showHistory) {
        FiltersRow(viewState, viewModel)
      }
    }

    PullToRefreshDefaults.Indicator(
      state = pullToRefreshState,
      isRefreshing = viewState.loading,
      modifier = Modifier.align(Alignment.TopCenter),
      color = MaterialTheme.colorScheme.primary,
      threshold = 58.dp
    )
  }
}

@Composable
private fun FiltersRow(viewState: HistoryDetailViewState, viewModel: HistoryDetailProxy) =
  Row(modifier = Modifier.padding(top = Distance.tiny)) {
    val lastIdx = viewState.filters.values.size - 1
    viewState.filters.values.forEachIndexed { index, selectableList ->
      TextSpinner(
        options = selectableList,
        onOptionSelected = { viewModel.changeFilter(it) },
        modifier = Modifier
          .padding(
            start = if (index == 0) Distance.small else 0.dp,
            end = if (index == lastIdx) 4.dp else 0.dp
          )
      )

      if (viewState.filters.count() == 2 && index == 0) {
        Spacer(
          modifier = Modifier
            .weight(1f)
            .defaultMinSize(minWidth = dimensionResource(id = R.dimen.distance_small))
        )
      }
    }
  }

@Composable
private fun DataSetsRow(content: @Composable RowScope.() -> Unit) =
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState())
      .background(color = MaterialTheme.colorScheme.surface)
      .height(80.dp),
    verticalAlignment = Alignment.CenterVertically,
    content = content
  )

@Composable
private fun DataSetItems(
  label: HistoryDataSet.Label,
  active: Boolean,
  historyEnabled: Boolean,
  clickEnabled: Boolean,
  onClick: () -> Unit
) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .height(dimensionResource(id = R.dimen.button_small_height))
      .let {
        if (clickEnabled) {
          it.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(),
            onClick = onClick
          )
        } else {
          it
        }
      }

  ) {
    when (label) {
      is HistoryDataSet.Label.Single -> DataSetItem(
        value = label.value,
        showColor = label.value.presentColor,
        active = historyEnabled && active
      )

      is HistoryDataSet.Label.Multiple ->
        label.values.forEach {
          if (!it.justColor) {
            DataSetItem(value = it, showColor = it.presentColor, active = historyEnabled && active)
          }
        }
    }
  }
}

@Composable
private fun DataSetItem(value: HistoryDataSet.LabelData, showColor: Boolean, active: Boolean) {
  value.imageId?.let { DataSetIcon(imageId = it, value.iconSize) }
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    DataSetText(text = value.value)
    if (showColor) {
      Box(
        modifier = Modifier
          .width(50.dp)
          .height(4.dp)
          .let {
            if (active) {
              it.background(colorResource(id = value.color), shape = RoundedCornerShape(2.dp))
            } else {
              it
            }
          }
          .border(1.dp, colorResource(id = value.color), RoundedCornerShape(2.dp))
      )
    }
  }
}

@Composable
private fun DataSetIcon(imageId: ImageId, @DimenRes iconSize: Int?) =
  Image(
    imageId = imageId,
    contentDescription = null,
    alignment = Alignment.Center,
    modifier = Modifier.size(dimensionResource(id = iconSize ?: R.dimen.button_small_height))
  )

@Composable
private fun DataSetText(text: String) =
  Text(
    text = text,
    fontSize = 16.sp,
    fontFamily = FontFamily(Font(R.font.quicksand_regular)),
  )

@Composable
private fun BottomPagination(viewState: HistoryDetailViewState, viewModel: HistoryDetailProxy) =
  viewState.rangeText?.let {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(0.dp),
      modifier = Modifier
        .padding(horizontal = dimensionResource(id = R.dimen.distance_tiny))
        .height(80.dp)
    ) {
      if (viewState.allowNavigation) {
        PaginationIcon(
          onClick = { viewModel.moveToDataBegin() },
          icon = R.drawable.ic_double_arrow_right,
          enabled = viewState.shiftLeftEnabled,
          rotate = true
        )
        PaginationIcon(
          onClick = { viewModel.moveRangeLeft() },
          enabled = viewState.shiftLeftEnabled,
          rotate = true
        )
      }
      Text(
        text = it,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.weight(1f)
      )
      if (viewState.allowNavigation) {
        PaginationIcon(
          onClick = { viewModel.moveRangeRight() },
          enabled = viewState.shiftRightEnabled
        )
        PaginationIcon(
          onClick = { viewModel.moveToDataEnd() },
          enabled = viewState.shiftRightEnabled,
          icon = R.drawable.ic_double_arrow_right
        )
      }
    }
  }

@Composable
private fun PaginationIcon(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: Int = R.drawable.ic_arrow_right,
  enabled: Boolean = true,
  rotate: Boolean = false
) =
  IconButton(icon = icon, onClick = onClick, modifier = modifier, enabled = enabled, rotate = rotate)

@Composable
private fun RangeSelection(viewState: HistoryDetailViewState, viewModel: HistoryDetailProxy) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .padding(horizontal = dimensionResource(id = R.dimen.distance_default))
      .height(80.dp)
  ) {
    Spacer(modifier = Modifier.weight(0.01f))
    DateTextField(date = viewState.range?.start) { viewModel.customRangeEditDate(RangeValueType.START) }
    HourTextField(date = viewState.range?.start) { viewModel.customRangeEditHour(RangeValueType.START) }
    Text(text = "-", modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.distance_tiny)))
    DateTextField(date = viewState.range?.end) { viewModel.customRangeEditDate(RangeValueType.END) }
    HourTextField(date = viewState.range?.end) { viewModel.customRangeEditHour(RangeValueType.END) }
    Spacer(modifier = Modifier.weight(0.01f))
  }
}

context(RowScope)
@Composable
fun DateTextField(date: Date?, onClick: () -> Unit) {
  TextField(
    value = LocalDateFormatter.current.getDateString(date) ?: "",
    modifier = Modifier
      .weight(0.3f)
      .height(36.dp),
    contentPadding = PaddingValues(8.dp),
    textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
    readOnly = true,
    onClicked = onClick
  )
}

context(RowScope)
@Composable
fun HourTextField(date: Date?, onClick: () -> Unit) {
  TextField(
    value = LocalDateFormatter.current.getHourString(date) ?: "",
    modifier = Modifier
      .weight(0.18f)
      .height(36.dp),
    contentPadding = PaddingValues(8.dp),
    textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
    readOnly = true,
    onClicked = onClick
  )
}

@Preview
@Composable
private fun Preview() {
  SuplaTheme {
    Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
      HistoryDetail(PreviewProxy())
    }
  }
}

private class PreviewProxy : HistoryDetailProxy {
  override fun getViewState(): StateFlow<HistoryDetailViewState> {
    val set = HistoryDataSet(
      type = ChartEntryType.HUMIDITY,
      label = singleLabel(
        imageId = ImageId(R.drawable.fnc_electricity_meter),
        value = "Label",
        color = R.color.light_red
      ),
      valueFormatter = HumidityValueFormatter(),
    )
    return MutableStateFlow(
      value = HistoryDetailViewState(
        chartData = LineChartData(
          dateRange = DateRange(Date().weekStart(), Date().weekEnd()),
          chartRange = ChartRange.WEEK,
          aggregation = ChartDataAggregation.MINUTES,
          sets = listOf(
            ChannelChartSets(
              1,
              SuplaFunction.UNKNOWN,
              { "" },
              ChartDataAggregation.MINUTES,
              listOf(set, set.copy(active = false)),
              null,
              { it.getString(R.string.details_em_reverse_reactive_energy) }
            )
          )
        ),
        showHistory = true
      )
    )
  }
}
