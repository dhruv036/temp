package io.stempedia.pictoblox.util

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

abstract class AbsAdapter<T, VH : RecyclerView.ViewHolder?>(val inflater: LayoutInflater, val rowLayout: Int) : RecyclerView.Adapter<VH>(),
    AbsDataListener<T> {
    private val dataList = mutableListOf<T>()

    override fun onDataChanged(operationType: OperationType, t: List<T>) {
        when (operationType) {
            OperationType.CONCAT -> {
                val start = dataList.size
                dataList.addAll(t)
                notifyItemRangeInserted(start, t.size)
            }

            OperationType.REPLACE -> {
                dataList.clear()
                dataList.addAll(t)
                notifyDataSetChanged()
            }
        }
    }
/*
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = inflater.inflate(rowLayout, parent, false)

    }*/
}


abstract class AbsVH(view: View) : RecyclerView.ViewHolder(view) {


}

abstract class AbsVHItemClick(view: View) : RecyclerView.ViewHolder(view) {


}

interface AbsDataListener<Type> {
    fun onDataChanged(operationType: OperationType, t: List<Type>)
}

enum class OperationType {
    CONCAT,
    REPLACE
}

