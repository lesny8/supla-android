package org.supla.android.features.details.windowdetail.base.ui.windowview.controls
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

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.supla.android.R
import org.supla.android.core.shared.data.model.lists.resource
import org.supla.android.core.shared.invoke
import org.supla.android.core.ui.theme.SuplaTheme
import org.supla.android.ui.lists.data.warning
import org.supla.core.shared.data.model.lists.ChannelIssueItem

@Composable
@SuppressLint("DefaultLocale")
fun PressTimeInfo(
  touchTime: Float?,
  modifier: Modifier = Modifier
) {
  touchTime?.let { time ->
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(painter = painterResource(id = R.drawable.ic_touch_hand), contentDescription = null)
      Text(text = String.format("%.1fs", time), style = MaterialTheme.typography.bodyMedium)
    }
  }
}

@Composable
fun IssuesView(issues: List<ChannelIssueItem>, modifier: Modifier = Modifier, smallScreen: Boolean = false) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.distance_tiny))
  ) {
    val rowHeight = dimensionResource(id = R.dimen.channel_warning_image_size)
    issues.forEach {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.distance_small)),
        modifier = Modifier
          .fillMaxWidth()
          .height(rowHeight)
      ) {
        Image(
          painter = painterResource(id = it.icon.resource),
          contentDescription = null,
          modifier = Modifier.size(rowHeight)
        )
        Text(text = it.message(LocalContext.current), style = MaterialTheme.typography.bodyMedium)
      }

      if (smallScreen) {
        return
      }
    }
  }
}

@Preview
@Composable
private fun Preview() {
  SuplaTheme {
    Column(
      verticalArrangement = Arrangement.spacedBy(8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .background(MaterialTheme.colorScheme.background)
        .padding(dimensionResource(id = R.dimen.distance_default))
    ) {
      PressTimeInfo(12.3f)
      IssuesView(
        issues = listOf(
          ChannelIssueItem.warning(R.string.roller_shutter_calibration_needed),
          ChannelIssueItem.warning(R.string.motor_problem)
        )
      )
    }
  }
}
