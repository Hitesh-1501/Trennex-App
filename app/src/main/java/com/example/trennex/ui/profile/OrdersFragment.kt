package com.example.trennex.ui.profile

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.trennex.R
import com.example.trennex.ui.main.MainActivity
import com.example.trennex.ui.main.ToolBarType

class OrdersFragment : Fragment(R.layout.fragment_orders) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainActivity)?.showToolBar(ToolBarType.TITLE, "My Orders")
    }
}
