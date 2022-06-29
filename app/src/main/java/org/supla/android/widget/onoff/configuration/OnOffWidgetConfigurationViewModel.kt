package org.supla.android.widget.onoff.configuration
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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.supla.android.Preferences
import org.supla.android.data.source.ChannelRepository
import org.supla.android.db.AuthProfileItem
import org.supla.android.db.Channel
import org.supla.android.lib.SuplaConst
import org.supla.android.profile.ProfileManager
import org.supla.android.widget.WidgetConfiguration
import org.supla.android.widget.WidgetPreferences
import java.security.InvalidParameterException
import javax.inject.Inject

@HiltViewModel
class OnOffWidgetConfigurationViewModel @Inject constructor(
        private val preferences: Preferences,
        private val widgetPreferences: WidgetPreferences,
        private val profileManager: ProfileManager,
        private val channelRepository: ChannelRepository
) : ViewModel() {

    private val _userLoggedIn = MutableLiveData<Boolean>()
    val userLoggedIn: LiveData<Boolean> = _userLoggedIn

    private val _dataLoading = MutableLiveData<Boolean>()
    val dataLoading: LiveData<Boolean> = _dataLoading

    private val _confirmationResult = MutableLiveData<Result<Channel>>()
    val confirmationResult: LiveData<Result<Channel>> = _confirmationResult

    private val _profilesList = MutableLiveData<List<AuthProfileItem>>()
    val profilesList: LiveData<List<AuthProfileItem>> = _profilesList

    private val _channelsList = MutableLiveData<List<Channel>>()
    val channelsList: LiveData<List<Channel>> = _channelsList

    var selectedProfile: AuthProfileItem? = null
    var selectedChannel: Channel? = null
    var widgetId: Int? = null
    var displayName: String? = null

    init {
        _dataLoading.value = true
        triggerDataLoad()
    }

    fun confirmSelection() {
        when {
            widgetId == null -> {
                _confirmationResult.value = Result.failure(InvalidParameterException())
            }
            selectedChannel == null -> {
                _confirmationResult.value = Result.failure(NoItemSelectedException())
            }
            displayName == null || displayName?.isBlank() == true -> {
                _confirmationResult.value = Result.failure(EmptyDisplayNameException())
            }
            else -> {
                setWidgetConfiguration(widgetId!!, selectedChannel!!.channelId, displayName!!, selectedChannel!!.func, selectedChannel!!.color)
                _confirmationResult.value = Result.success(selectedChannel!!)
            }
        }
    }

    fun onDisplayNameChanged(s: CharSequence, `_`: Int, `__`: Int, `___`: Int) {
        displayName = s.toString()
    }

    fun changeProfile(profile: AuthProfileItem?) {
        if (profile == null) {
            return // nothing to do
        }
        _dataLoading.value = true
        selectedProfile = profile

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                loadSwitches()
                _dataLoading.postValue(false)
            }
        }
    }

    private fun triggerDataLoad() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val configSet = preferences.configIsSet()
                if (configSet) {
                    _profilesList.postValue(profileManager.getAllProfiles())
                    selectedProfile = profileManager.getCurrentProfile()

                    loadSwitches()
                }

                _dataLoading.postValue(false)
                _userLoggedIn.postValue(configSet)
            }
        }
    }

    private fun loadSwitches() {
        val switches = getAllChannels()
                .filter { it.isSwitch() || it.isRollerShutter() }
        _channelsList.postValue(switches)
        if (switches.isNotEmpty()) {
            selectedChannel = switches[0]
        }
    }

    private fun getAllChannels(): List<Channel> {
        channelRepository.getAllProfileChannels(selectedProfile?.id).use { cursor ->
            val channels = mutableListOf<Channel>()
            if (!cursor.moveToFirst()) {
                return channels
            }

            do {
                val channel = Channel()
                channel.AssignCursorData(cursor)
                channels.add(channel)
            } while (cursor.moveToNext())

            // As the widgets are stateless it is possible that user creates many widgets for the same channel id
            return channels
        }
    }

    private fun setWidgetConfiguration(widgetId: Int, channelId: Int, channelName: String,
                                       channelFunction: Int, channelColor: Int) {
        widgetPreferences.setWidgetConfiguration(widgetId, WidgetConfiguration(channelId, channelName, channelFunction, channelColor, selectedProfile!!.id, true))
    }
}

class NoItemSelectedException : RuntimeException() {}

class EmptyDisplayNameException : RuntimeException() {}

private fun Channel.isSwitch() =
        func == SuplaConst.SUPLA_CHANNELFNC_LIGHTSWITCH
                || func == SuplaConst.SUPLA_CHANNELFNC_DIMMER
                || func == SuplaConst.SUPLA_CHANNELFNC_DIMMERANDRGBLIGHTING
                || func == SuplaConst.SUPLA_CHANNELFNC_RGBLIGHTING
                || func == SuplaConst.SUPLA_CHANNELFNC_POWERSWITCH

private fun Channel.isRollerShutter() =
        func == SuplaConst.SUPLA_CHANNELFNC_CONTROLLINGTHEROLLERSHUTTER
                || func == SuplaConst.SUPLA_CHANNELFNC_CONTROLLINGTHEROOFWINDOW