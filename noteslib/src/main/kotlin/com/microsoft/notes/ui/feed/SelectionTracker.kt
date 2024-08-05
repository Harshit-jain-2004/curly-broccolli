package com.microsoft.notes.ui.feed

import androidx.recyclerview.widget.RecyclerView
import com.microsoft.notes.ui.feed.recyclerview.feeditem.FeedItemViewHolder

class SelectionTracker(
    val adapter: RecyclerView.Adapter<FeedItemViewHolder>,
    val callback: Callbacks,
    private val isMultiSelectEnabled: Boolean
) {

    private var selectionEnabled: Boolean = false
    private var selectedItems: HashSet<String> = hashSetOf()

    private fun notifyItem(id: String) {
        val idx = callback.getIndexFromItemLocalId(id)
        adapter.notifyItemChanged(idx)
    }

    fun isSelectionEnabled() = selectionEnabled

    fun getSelectedItems() = selectedItems

    fun startSelection(id: String) {
        if (id.isEmpty()) return

        selectionEnabled = true
        selectedItems.clear()
        selectedItems.add(id)
        adapter.notifyDataSetChanged()
    }

    fun setSelectedItem(id: String) {
        if (id.isEmpty() || !selectionEnabled) return

        if (isMultiSelectEnabled) {
            if (!isItemSelected(id)) {
                selectedItems.add(id)
            } else {
                selectedItems.remove(id)
            }
            notifyItem(id)
        } else {
            /* Have only one selected item and notify the previous item */
            val selectedItem = selectedItems.toList()[0]
            selectedItems.remove(selectedItem)
            notifyItem(selectedItem)
            if (selectedItem != id) {
                selectedItems.add(id)
                notifyItem(id)
            }
        }
    }

    fun isItemSelected(id: String): Boolean {
        if (!selectionEnabled) return false
        return selectedItems.contains(id)
    }

    fun clearSelection() {
        selectionEnabled = false
        selectedItems.clear()
        adapter.notifyDataSetChanged()
    }

    interface Callbacks {
        fun getIndexFromItemLocalId(itemId: String?): Int
    }
}
