package org.supla.android.features.standarddetail.timersdetail

import android.os.Bundle
import android.os.CountDownTimer
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import org.supla.android.R
import org.supla.android.Trace
import org.supla.android.core.ui.BaseFragment
import org.supla.android.core.ui.BaseViewModel
import org.supla.android.databinding.FragmentTimersDetailBinding
import org.supla.android.extensions.TAG
import org.supla.android.extensions.visibleIf
import org.supla.android.images.ImageCache
import org.supla.android.lib.SuplaClientMsg
import java.util.*

private const val ARG_REMOTE_ID = "ARG_REMOTE_ID"

@AndroidEntryPoint
class TimersDetailFragment : BaseFragment<TimersDetailViewState, TimersDetailViewEvent>(R.layout.fragment_timers_detail) {

  private val viewModel: TimersDetailViewModel by viewModels()
  private val binding by viewBinding(FragmentTimersDetailBinding::bind)

  private val remoteId: Int by lazy { arguments!!.getInt(ARG_REMOTE_ID) }
  private var timer: CountDownTimer? = null
  private var leftTimeInSecs: Int = 0

  // Not always timer.cancel() stops timer, this flag is to handle this problem
  private var timerActive = false

  override fun getViewModel(): BaseViewModel<TimersDetailViewState, TimersDetailViewEvent> = viewModel

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    binding.detailsTimerConfiguration.onStartClickListener = { timeInSeconds, action ->
      viewModel.startTimer(remoteId, action == TimerTargetAction.TURN_ON, timeInSeconds)
    }
    binding.detailsTimerConfiguration.onEditCancelClickListener = { viewModel.cancelEditMode() }
    binding.detailsTimerStopButton.setOnClickListener { viewModel.stopTimer(remoteId) }
    binding.detailsTimerCancelButton.setOnClickListener { viewModel.cancelTimer(remoteId) }
    binding.detailsTimerEditTime.setOnClickListener {
      binding.detailsTimerConfiguration.timeInSeconds = leftTimeInSecs
      viewModel.startEditMode()
    }
  }

  override fun onStart() {
    super.onStart()
    viewModel.loadData(remoteId)
  }

  override fun onStop() {
    timer?.cancel()
    super.onStop()
  }

  override fun handleEvents(event: TimersDetailViewEvent) {
    when (event) {
      TimersDetailViewEvent.ShowInvalidTimeToast -> Toast.makeText(context, R.string.details_timer_wrong_time, Toast.LENGTH_LONG).show()
    }
  }

  override fun handleViewState(state: TimersDetailViewState) {
    state.channel?.let {
      binding.detailsTimerConfiguration.deviceStateOn = it.value.hiValue()
      binding.detailsTimerState.deviceStateIcon.setImageBitmap(ImageCache.getBitmap(requireContext(), it.imageIdx))
      binding.detailsTimerState.deviceStateValue.text = if (it.value.hiValue()) {
        getString(R.string.details_timer_device_on)
      } else {
        getString(R.string.details_timer_device_off)
      }
    }

    val showTimer = state.timerData != null
    binding.detailsTimerConfiguration.visibleIf(showTimer.not() || state.editMode)
    binding.detailsTimerConfiguration.editMode = state.editMode
    binding.detailsTimerProgress.visibleIf(showTimer)

    handleTimerState(state)
  }

  private fun handleTimerState(state: TimersDetailViewState) {
    Trace.d(TAG, "Handling timer state: ${state.timerData}")
    if (state.timerData != null) {
      setupTimer(state.timerData.startTime, state.timerData.endTime)
      setTimerValues(state.timerData.startTime, state.timerData.endTime)
      binding.detailsTimerProgress.indeterminate = state.timerData.indeterminate

      val formatString = getString(R.string.hour_string_format)
      binding.detailsTimerProgressEndHour.text = getString(
        R.string.details_timer_end_hour,
        DateFormat.format(formatString, state.timerData.endTime)
      )
      binding.detailsTimerStopButton.text = getString(
        R.string.details_timer_leave_it,
        resources.getQuantityString(if (state.timerData.timerValue == TimerValue.ON) R.plurals.details_timer_info_on else R.plurals.details_timer_info_off, 1)
      )
      binding.detailsTimerCancelButton.text = getString(
        R.string.details_timer_cancel_and,
        resources.getQuantityString(if (state.timerData.timerValue == TimerValue.ON) R.plurals.details_timer_info_off else R.plurals.details_timer_info_on, 3)
      )
    } else {
      timer?.cancel()
      timer = null
    }
  }

  private fun setTimerValues(startTime: Date, endTime: Date) {
    val data = viewModel.calculateProgressViewData(startTime, endTime)
    binding.detailsTimerProgress.progress = data.progress
    binding.detailsTimerProgressTime.text = getString(
      R.string.details_timer_format,
      data.leftTimeValues.hours,
      data.leftTimeValues.minutes,
      data.leftTimeValues.seconds
    )
  }

  private fun setupTimer(startTime: Date, endTime: Date) {
    timer?.cancel()

    val leftTime = endTime.time - Date().time
    if (leftTime > 0) {
      timerActive = true
      timer = object : CountDownTimer(leftTime, 100) {
        override fun onTick(millisUntilFinished: Long) {
          leftTimeInSecs = millisUntilFinished.div(1000).toInt()
          if (timerActive) {
            setTimerValues(startTime, endTime)
          }
        }

        override fun onFinish() {
          timer?.cancel()
          timer = null
          viewModel.loadData(remoteId)
        }
      }.start()
    }
  }

  override fun onSuplaMessage(message: SuplaClientMsg) {
    super.onSuplaMessage(message)
    when (message.type) {
      SuplaClientMsg.onDataChanged -> if (message.channelId == remoteId) {
        Trace.i(TAG, "Detail got data changed event")
        timer?.cancel()
        timerActive = false
        viewModel.loadData(remoteId)
      }
    }
  }

  companion object {
    fun bundle(remoteId: Int) = bundleOf(
      ARG_REMOTE_ID to remoteId
    )
  }
}
