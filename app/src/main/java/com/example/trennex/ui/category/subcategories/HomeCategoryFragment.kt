package com.example.trennex.ui.category.subcategories

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.example.trennex.R
import com.example.trennex.databinding.FragmentHomeCategoryBinding
import com.example.trennex.ui.category.adapter.HomeCategoryAdapter
import com.example.trennex.ui.category.adapter.HomeCategoryAdapter.Companion.TYPE_HOME_BANNER
import com.example.trennex.ui.category.adapter.HomeCategoryAdapter.Companion.TYPE_HOME_HEADER
import com.example.trennex.ui.category.adapter.HomeCategoryAdapter.Companion.TYPE_HOME_ITEM
import com.example.trennex.ui.category.model.HomeCategoryModel


class HomeCategoryFragment : Fragment() {

    private var _binding: FragmentHomeCategoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val list = bundleList()
        val grid = GridLayoutManager(requireContext(),3)
        grid.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup(){
            override fun getSpanSize(position: Int): Int{
                return if(list[position].Type == TYPE_HOME_ITEM) 1 else 3
            }
        }
        binding.rvHome.apply {
            adapter = HomeCategoryAdapter(list)
            layoutManager = grid
        }
    }

    private fun bundleList(): List<HomeCategoryModel>{
        val list = mutableListOf<HomeCategoryModel>()
        list.add(HomeCategoryModel(TYPE_HOME_BANNER))
        list.add(HomeCategoryModel(TYPE_HOME_HEADER, title = "Kitchen Items"))
        list.add(HomeCategoryModel(TYPE_HOME_ITEM, R.drawable.kitchen_acc,"Gas,stoves & Accessories"))
        list.add(HomeCategoryModel(TYPE_HOME_ITEM, R.drawable.kitchen_cookware,"Cookware Essentials"))
        list.add(HomeCategoryModel(TYPE_HOME_ITEM, R.drawable.kitchen_dinning,"Dinning Serveware"))
        list.add(HomeCategoryModel(TYPE_HOME_ITEM, R.drawable.kitchen_tools,"Kitchen Storage"))
        list.add(HomeCategoryModel(TYPE_HOME_ITEM, R.drawable.kitchen_tools,"Kitchen Tools"))


        list.add(HomeCategoryModel(TYPE_HOME_HEADER, title = "Home Furnishing"))

        list.add(HomeCategoryModel(TYPE_HOME_ITEM, R.drawable.home_bedsheets,"Bedsheets"))
        list.add(HomeCategoryModel(TYPE_HOME_ITEM, R.drawable.home_curtains,"Curtains"))
        list.add(HomeCategoryModel(TYPE_HOME_ITEM, R.drawable.home_blankets,"Blankets"))
        list.add(HomeCategoryModel(TYPE_HOME_ITEM, R.drawable.home_sofa,"Sofa Covers"))
        list.add(HomeCategoryModel(TYPE_HOME_ITEM, R.drawable.home_mats,"Mats"))
        list.add(HomeCategoryModel(TYPE_HOME_ITEM, R.drawable.home_protector,"Mattress Protector"))
        list.add(HomeCategoryModel(TYPE_HOME_ITEM, R.drawable.home_pillow,"Pillows"))
        list.add(HomeCategoryModel(TYPE_HOME_ITEM, R.drawable.home_towel,"Towels & Bath Linen"))
        list.add(HomeCategoryModel(TYPE_HOME_ITEM, R.drawable.home_mosquito,"Mosquito Nets"))


        list.add(HomeCategoryModel(TYPE_HOME_HEADER, title = "Home Improvement Tools"))

        list.add(HomeCategoryModel(TYPE_HOME_ITEM, R.drawable.home_handtools,"Hand Tools"))
        list.add(HomeCategoryModel(TYPE_HOME_ITEM, R.drawable.home_bath,"Bath & Kitchen Fitting"))
        list.add(HomeCategoryModel(TYPE_HOME_ITEM, R.drawable.home_utilities,"Home Utilities"))
        list.add(HomeCategoryModel(TYPE_HOME_ITEM, R.drawable.home_gardening,"Gardening Essentials"))
        list.add(HomeCategoryModel(TYPE_HOME_ITEM, R.drawable.home_electrical,"Electrical Hardware"))


        list.add(HomeCategoryModel(TYPE_HOME_HEADER, title = "Decor & Lighting"))

        list.add(HomeCategoryModel(TYPE_HOME_ITEM, R.drawable.home_showpieces,"Showpieces"))
        list.add(HomeCategoryModel(TYPE_HOME_ITEM, R.drawable.home_walldecor,"Wall Decor"))
        list.add(HomeCategoryModel(TYPE_HOME_ITEM, R.drawable.home_wallclock,"Wall Clock"))
        list.add(HomeCategoryModel(TYPE_HOME_ITEM, R.drawable.home_cellinglight,"Celling Light"))
        list.add(HomeCategoryModel(TYPE_HOME_ITEM, R.drawable.home_lightning,"Lighting"))


        return list
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}