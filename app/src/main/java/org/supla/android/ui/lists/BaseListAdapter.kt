package org.supla.android.ui.lists

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.supla.android.LocationCaptionEditor
import org.supla.android.Preferences
import org.supla.android.R
import org.supla.android.SuplaApp
import org.supla.android.databinding.LocationListItemBinding
import org.supla.android.db.Location

abstract class BaseListAdapter<T, D>(
  private val context: Context,
  private val preferences: Preferences
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

  protected val items: MutableList<T> = mutableListOf()
  protected abstract val callback: BaseListCallback
  protected val itemTouchHelper by lazy { ItemTouchHelper(callback) }

  var movementFinishedCallback: (items: List<D>) -> Unit = { }
  var swappedElementsCallback: (firstItem: D?, secondItem: D?) -> Unit = { _, _ -> }
  var reloadCallback: () -> Unit = { }
  var toggleLocationCallback: (location: Location) -> Unit = { }

  var leftButtonClickCallback: (id: Int) -> Unit = { _ -> }
  var rightButtonClickCallback: (id: Int) -> Unit = { _ -> }

  protected var movedItem: T? = null
  protected var replacedItem: T? = null

  override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
    super.onAttachedToRecyclerView(recyclerView)

    itemTouchHelper.attachToRecyclerView(recyclerView)
    callback.setup(recyclerView)
  }

  override fun getItemCount(): Int {
    return items.count()
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): RecyclerView.ViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    return when (viewType) {
      R.layout.location_list_item -> {
        val binding = LocationListItemBinding.inflate(inflater, parent, false)
        binding.tvSectionCaption.typeface = SuplaApp.getApp().typefaceQuicksandRegular
        LocationListItemViewHolder(binding)
      }
      else -> throw IllegalArgumentException("unsupported view type $viewType")
    }
  }

  fun setItems(items: List<T>) {
    this.items.clear()
    this.items.addAll(items)
    notifyDataSetChanged()
  }

  fun onLeftButtonClick(remoteId: Int) {
    if (preferences.isButtonAutohide) {
      callback.closeWhenSwiped()
    }
    leftButtonClickCallback(remoteId)
  }

  fun onRightButtonClick(remoteId: Int) {
    if (preferences.isButtonAutohide) {
      callback.closeWhenSwiped()
    }
    rightButtonClickCallback(remoteId)
  }

  protected fun swapInternally(fromPos: Int, toPos: Int) {
    if (movedItem == null) {
      movedItem = items[fromPos]
    }
    replacedItem = items[toPos]

    val buf = items[fromPos]
    items[fromPos] = items[toPos]
    items[toPos] = buf
  }

  protected fun cleanSwap() {
    movedItem = null
    replacedItem = null
  }

  protected fun changeLocationCaption(locationId: Int): Boolean {
    SuplaApp.Vibrate(context)
    val editor = LocationCaptionEditor(context)
    editor.captionChangedListener = reloadCallback
    editor.edit(locationId)

    return true
  }

  class LocationListItemViewHolder(val binding: LocationListItemBinding) :
    RecyclerView.ViewHolder(binding.root)
}