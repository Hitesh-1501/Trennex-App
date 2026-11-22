package com.example.trennex.ui.category

import android.view.GestureDetector
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView
import androidx.fragment.app.Fragment

internal fun Fragment.openCategoryProducts(slug: String, title: String, tile: String? = null) {
    (requireParentFragment() as CategoryFragment).openProducts(slug, title, tile)
}

/** Lets the existing category tile adapters retain their layouts while opening a shared listing. */
internal fun RecyclerView.onTileTap(onTap: (Int) -> Unit) {
    val detector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val child = findChildViewUnder(e.x, e.y) ?: return false
            val position = getChildAdapterPosition(child)
            if (position != RecyclerView.NO_POSITION) onTap(position)
            return false
        }
    })
    addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
        override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
            detector.onTouchEvent(e)
            return false
        }
    })
}
