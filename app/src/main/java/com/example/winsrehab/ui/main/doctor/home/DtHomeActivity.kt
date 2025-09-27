package com.example.winsrehab.ui.main.doctor.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.winsrehab.databinding.ActivityDtHomeBinding
import com.example.winsrehab.ui.main.doctor.adapter.PatientAdapter
import com.example.winsrehab.ui.main.doctor.info.DtInfoActivity

class DtHomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDtHomeBinding
    private val viewModel: DtHomeVM by viewModels()
    var doctorCode: String=""
    var PtCount:Int=0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDtHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

         doctorCode = intent.getStringExtra("doctorCode") ?: ""
        Log.d("DtHomeActivity", "doctorCode: $doctorCode")

        val adapter= PatientAdapter()
//        让列表纵向排列
        binding.rvKeyPatients.layoutManager = LinearLayoutManager(this)
        //绑定adapter
        binding.rvKeyPatients.adapter = adapter
        //观察数据变化并更新UI
        viewModel.patients.observe(this) { list ->
            adapter.submitList(list)  //把最新的患者列表交给adapter，触发DiffUtil检查变化并刷新
            //更新患者总数
            PtCount=adapter.itemCount
            binding.tvTotalValue.text = PtCount.toString()
            Log.d("DtHomeActivity", "PtCount: $PtCount")
        }

        binding.btnInfoCenter.setOnClickListener {
            val intent = Intent(this, DtInfoActivity::class.java)
             intent.putExtra("doctorCode", doctorCode)
             intent.putExtra("totalCount", PtCount)
            startActivity(intent)
        }

        viewModel.loadPatients(doctorCode)
        binding.tvTotalValue.text=adapter.itemCount.toString()
    }
}

