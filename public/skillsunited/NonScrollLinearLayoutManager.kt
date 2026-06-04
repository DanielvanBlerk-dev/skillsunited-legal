package com.dkvb.skillswap

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class NonScrollLinearLayoutManager(context: Context) : LinearLayoutManager(context) {

    override fun canScrollVertically() = false

    override fun canScrollHorizontally() = false

    override fun isAutoMeasureEnabled() = true

    override fun onMeasure(
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State,
        widthSpec: Int,
        heightSpec: Int
    ) {
        val newHeightSpec = ViewGroup.LayoutParams.WRAP_CONTENT.let {
            android.view.View.MeasureSpec.makeMeasureSpec(
                android.view.View.MeasureSpec.getSize(heightSpec),
                android.view.View.MeasureSpec.UNSPECIFIED
            )
        }
        super.onMeasure(recycler, state, widthSpec, newHeightSpec)
    }
}