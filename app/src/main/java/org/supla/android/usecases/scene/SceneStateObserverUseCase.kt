package org.supla.android.usecases.scene

import org.supla.android.data.source.SceneRepository
import org.supla.android.lib.SuplaClientMessageHandler
import org.supla.android.lib.SuplaClientMessageHandler.OnSuplaClientMessageListener
import org.supla.android.lib.SuplaClientMsg
import org.supla.android.scenes.ListsEventsManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SceneStateObserverUseCase @Inject constructor(
  private val listsEventsManager: ListsEventsManager,
  private val messageHandler: SuplaClientMessageHandler,
  private val sceneRepository: SceneRepository
) {

  private val listener: OnSuplaClientMessageListener = OnSuplaClientMessageListener { msg ->
    if (msg?.type == SuplaClientMsg.onSceneStateChanged) {
      val scene = sceneRepository.getScene(msg.sceneId) ?: return@OnSuplaClientMessageListener
      listsEventsManager.emitSceneChange(
        scene.sceneId,
        ListsEventsManager.State.Scene(scene.isExecuting(), scene.estimatedEndDate)
      )
    }
  }

  fun register() {
    messageHandler.registerMessageListener(listener)
  }

  fun unregister() {
    messageHandler.unregisterMessageListener(listener)
  }
}