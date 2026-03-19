package com.example.trennex.ui.category.subcategories

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.example.trennex.R
import com.example.trennex.databinding.FragmentMobileBinding
import com.example.trennex.ui.category.adapter.MobileCategoryAdapter
import com.example.trennex.ui.category.model.MobileCategoryModel

class MobileFragment : Fragment() {
    private var _binding : FragmentMobileBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMobileBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setMobileCategory()
    }

    private fun setMobileCategory(){
        val mobileCategoryList = listOf(
            MobileCategoryModel(R.drawable.iv_vivo),
            MobileCategoryModel(R.drawable.iv_realme),
            MobileCategoryModel(R.drawable.iv_samsung),
            MobileCategoryModel(R.drawable.iv_oppo),
            MobileCategoryModel(R.drawable.iv_poco),
            MobileCategoryModel(R.drawable.iv_nothing),
            MobileCategoryModel(R.drawable.iv_aj),
            MobileCategoryModel(R.drawable.iv_mi),
            MobileCategoryModel(R.drawable.iv_gpixel),
            MobileCategoryModel(R.drawable.iv_tecno),
            MobileCategoryModel(R.drawable.iv_alcatel),
            MobileCategoryModel(R.drawable.iv_itel),
            MobileCategoryModel(R.drawable.iv_infinix),
            MobileCategoryModel(R.drawable.iv_moto),
            MobileCategoryModel(R.drawable.iv_iphone),
        )

        binding.rvMobiles.apply {
            layoutManager = GridLayoutManager(requireContext(),3)
            adapter = MobileCategoryAdapter(mobileCategoryList)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}