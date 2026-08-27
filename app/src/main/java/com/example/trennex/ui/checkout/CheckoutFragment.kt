package com.example.trennex.ui.checkout

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.trennex.R
import com.example.trennex.ui.main.MainActivity
import com.example.trennex.ui.main.ToolBarType

class CheckoutFragment : Fragment(R.layout.fragment_checkout) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? MainActivity)?.showToolBar(ToolBarType.CART)
        (activity as? MainActivity)?.toggleCartProgress(true)
        (activity as? MainActivity)?.updateCartStep(2)
    }
}
