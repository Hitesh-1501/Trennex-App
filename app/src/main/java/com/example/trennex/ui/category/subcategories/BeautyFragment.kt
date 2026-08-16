package com.example.trennex.ui.category.subcategories

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.example.trennex.R
import com.example.trennex.databinding.FragmentBeautyBinding
import com.example.trennex.ui.category.adapter.BeautyCategoryAdapter
import com.example.trennex.ui.category.adapter.BeautyCategoryAdapter.Companion.TYPE_BEAUTY_BANNER
import com.example.trennex.ui.category.adapter.BeautyCategoryAdapter.Companion.TYPE_BEAUTY_HEADER
import com.example.trennex.ui.category.adapter.BeautyCategoryAdapter.Companion.TYPE_BEAUTY_ITEM
import com.example.trennex.ui.category.model.BeautyCategoryModel


class BeautyFragment : Fragment() {
    private var _binding: FragmentBeautyBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentBeautyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val list = bundleList()
        val grid = GridLayoutManager(requireContext(),3)
        grid.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup(){
            override fun getSpanSize(position: Int): Int{
                return if(list[position].Type == TYPE_BEAUTY_ITEM) 1 else 3
            }
        }
        binding.rvBeauty.apply {
            adapter = BeautyCategoryAdapter(list)
            layoutManager = grid
        }
    }

    private fun bundleList(): List<BeautyCategoryModel>{
        val list = mutableListOf<BeautyCategoryModel>()
        list.add(BeautyCategoryModel(TYPE_BEAUTY_BANNER))
        list.add(BeautyCategoryModel(TYPE_BEAUTY_HEADER, title = "Skin Care"))
        list.add(BeautyCategoryModel(TYPE_BEAUTY_ITEM, R.drawable.face_wash,"Face Wash"))
        list.add(BeautyCategoryModel(TYPE_BEAUTY_ITEM, R.drawable.sunscreen,"SunScreen"))
        list.add(BeautyCategoryModel(TYPE_BEAUTY_ITEM, R.drawable.face_cream,"Face Cream"))
        list.add(BeautyCategoryModel(TYPE_BEAUTY_ITEM, R.drawable.lotions,"Lotions"))
        list.add(BeautyCategoryModel(TYPE_BEAUTY_ITEM, R.drawable.scrub,"Scrub"))


        list.add(BeautyCategoryModel(TYPE_BEAUTY_HEADER, title = "Hair Care"))

        list.add(BeautyCategoryModel(TYPE_BEAUTY_ITEM, R.drawable.shampoo,"Shampoo"))
        list.add(BeautyCategoryModel(TYPE_BEAUTY_ITEM, R.drawable.hair_oil,"Hair Oil"))
        list.add(BeautyCategoryModel(TYPE_BEAUTY_ITEM, R.drawable.hair_serum,"Hair Serum"))
        list.add(BeautyCategoryModel(TYPE_BEAUTY_ITEM, R.drawable.conditioners,"Conditioners"))
        list.add(BeautyCategoryModel(TYPE_BEAUTY_ITEM, R.drawable.hair_color,"Hair Color"))


        list.add(BeautyCategoryModel(TYPE_BEAUTY_HEADER, title = "Fragrance"))

        list.add(BeautyCategoryModel(TYPE_BEAUTY_ITEM, R.drawable.fk_exclusive,"FK Exclusive"))
        list.add(BeautyCategoryModel(TYPE_BEAUTY_ITEM, R.drawable.gift_set,"Gift Set"))
        list.add(BeautyCategoryModel(TYPE_BEAUTY_ITEM, R.drawable.attar,"Attar"))
        list.add(BeautyCategoryModel(TYPE_BEAUTY_ITEM, R.drawable.deodorants,"Deodorants"))
        list.add(BeautyCategoryModel(TYPE_BEAUTY_ITEM, R.drawable.roll_ons,"Roll Ons"))
        list.add(BeautyCategoryModel(TYPE_BEAUTY_ITEM, R.drawable.perfume,"Perfume"))


        list.add(BeautyCategoryModel(TYPE_BEAUTY_HEADER, title = "Beauty"))

        list.add(BeautyCategoryModel(TYPE_BEAUTY_ITEM, R.drawable.foundation,"Foundation"))
        list.add(BeautyCategoryModel(TYPE_BEAUTY_ITEM, R.drawable.lipstick,"Lipstick"))
        list.add(BeautyCategoryModel(TYPE_BEAUTY_ITEM, R.drawable.kajal,"Kajal"))
        list.add(BeautyCategoryModel(TYPE_BEAUTY_ITEM, R.drawable.meakup_kit,"Makeup Kit"))
        list.add(BeautyCategoryModel(TYPE_BEAUTY_ITEM, R.drawable.compact,"Compact"))
        list.add(BeautyCategoryModel(TYPE_BEAUTY_ITEM, R.drawable.mascara,"Mascara"))

        list.add(BeautyCategoryModel(TYPE_BEAUTY_HEADER, title = "Daily Essentials"))

        list.add(BeautyCategoryModel(TYPE_BEAUTY_ITEM, R.drawable.bath_spa,"Bath & Spa"))
        list.add(BeautyCategoryModel(TYPE_BEAUTY_ITEM, R.drawable.oral_care,"Oral Care"))
        list.add(BeautyCategoryModel(TYPE_BEAUTY_ITEM, R.drawable.shaving_essentials,"Shaving Essentials"))
        list.add(BeautyCategoryModel(TYPE_BEAUTY_ITEM, R.drawable.soaps,"Soaps"))


        return list
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}