package org.supla.android.features.channellist

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import org.supla.android.R
import org.supla.android.SuplaApp
import org.supla.android.core.networking.suplaclient.SuplaClientProvider
import org.supla.android.core.ui.BaseFragment
import org.supla.android.core.ui.BaseViewModel
import org.supla.android.databinding.FragmentChannelListBinding
import org.supla.android.ui.dialogs.exceededAmperageDialog
import org.supla.android.ui.dialogs.valveAlertDialog
import org.supla.android.usecases.channel.ButtonType
import javax.inject.Inject

@AndroidEntryPoint
class ChannelListFragment : BaseFragment<ChannelListViewState, ChannelListViewEvent>(R.layout.fragment_channel_list) {

  private val viewModel: ChannelListViewModel by viewModels()
  private val binding by viewBinding(FragmentChannelListBinding::bind)

  @Inject
  lateinit var adapter: ChannelsAdapter

  @Inject
  lateinit var suplaClientProvider: SuplaClientProvider

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    binding.channelsList.adapter = adapter
    setupAdapter()
  }

  override fun onStart() {
    super.onStart()
    viewModel.loadChannels()
  }

  override fun getViewModel(): BaseViewModel<ChannelListViewState, ChannelListViewEvent> = viewModel

  override fun handleEvents(event: ChannelListViewEvent) {
    val suplaClient = suplaClientProvider.provide()
    when (event) {
      is ChannelListViewEvent.ShowValveDialog -> valveAlertDialog(event.remoteId, suplaClient).show()
      is ChannelListViewEvent.ShowAmperageExceededDialog -> exceededAmperageDialog(event.remoteId, suplaClient).show()
    }
  }

  override fun handleViewState(state: ChannelListViewState) {
    adapter.setItems(state.channels)
  }

  private fun setupAdapter() {
    adapter.leftButtonClickCallback = {
      SuplaApp.Vibrate(context)
      viewModel.performAction(it, ButtonType.LEFT)
    }
    adapter.rightButtonClickCallback = {
      SuplaApp.Vibrate(context)
      viewModel.performAction(it, ButtonType.RIGHT)
    }
    adapter.swappedElementsCallback = { first, second -> viewModel.swapItems(first, second) }
    adapter.reloadCallback = { viewModel.loadChannels() }
    adapter.toggleLocationCallback = { viewModel.toggleLocationCollapsed(it) }
  }
}