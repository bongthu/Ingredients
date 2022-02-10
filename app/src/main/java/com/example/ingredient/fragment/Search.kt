package com.example.ingredient.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ingredient.R
import com.example.ingredient.SearchAdapter
import com.example.ingredient.databinding.FragmentSearchBinding
import com.google.android.material.chip.Chip
import com.google.firebase.firestore.FirebaseFirestore

class Search : Fragment() {
    private lateinit var adapter: SearchAdapter
    private lateinit var database: FirebaseFirestore
    private var strList = mutableListOf<String>()
    private var recipeList = mutableListOf<Array<Any>>()

    private var _binding : FragmentSearchBinding? = null
    private val binding get()  = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        database = FirebaseFirestore.getInstance()
        _binding = FragmentSearchBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapterConnect(recipeList)
    }
    override fun onStart() {
        super.onStart()
        // 엔터키로 검색 실행 기능
        binding.findwindow.setOnEditorActionListener { v, actionId, event ->
            var handled = false
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                binding.searchBtn.performClick()
                handled = true
            }
            handled
        }

        binding.searchBtn.setOnClickListener {
            // 검색창에 입력한 재료들 리스트화
            var str = binding.findwindow.text.toString().split(",")
            binding.findwindow.setText("")


            if (str.size > 0) {
                var check = true
                // 검색한 재료의 좌우 빈칸 제거 ex) " 감자" -> "감자"
                for (element in str) {
                    check = true
                    for (i in 0 until strList.size) {
                        if (strList[i] == element) {
                            check = false
                            break
                        }
                    }
                    // 검색 재료 중복 체크
                    if (element.isNullOrEmpty()) check = false
                    if(check) {
                        strList.add(element.trim())
                        // chip 생성
                        binding.chipGroup.addView(Chip(context).apply {
                            text = element.trim() // chip 텍스트 설정
                            isCloseIconVisible = true // chip에서 X 버튼 보이게 하기
                            setOnCloseIconClickListener {
                                binding.chipGroup.removeView(this)
                                strList.remove(text)
                                // chip 삭제 반영
                                if (strList.isNullOrEmpty()) for (i in 0 until recipeList.size) adapter.nullItem(i)
                                else SearchQuery(database, strList)
                            }
                        })
                    }
                }
                if (check) {
                    // null, 중복 없다면 쿼리 실행
                    SearchQuery(database, strList)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
    fun SearchQuery(database:FirebaseFirestore, strList:MutableList<String>):Unit {
        val refs = database.collection("users")
        // 검색 통해 나온 레시피명을 담는 리스트
        recipeList = mutableListOf<Array<Any>>()
        refs.whereArrayContainsAny("ingredients", strList).get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    Log.d("MainTest : ", document.toString())
                    // 레시피 검색해서 나온 이름, 재료, 시간 저장
                    var int_str: String = document.get("ingredients").toString()
                    // 재료들을 포함하는 리스트
                    int_str = int_str.substring(1..int_str.length - 2)
                    recipeList.add(
                        arrayOf(
                            document.get("name").toString(),
                            int_str,
                            document.get("time").toString()
                        )
                    )
                }
                adapterConnect(recipeList)
            }
    }
    fun adapterConnect(recipeList: MutableList<Array<Any>>) {
        adapter = SearchAdapter(recipeList)
        binding.FindrecyclerView.layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        binding.FindrecyclerView.itemAnimator = DefaultItemAnimator()
        binding.FindrecyclerView.adapter = adapter
    }
}