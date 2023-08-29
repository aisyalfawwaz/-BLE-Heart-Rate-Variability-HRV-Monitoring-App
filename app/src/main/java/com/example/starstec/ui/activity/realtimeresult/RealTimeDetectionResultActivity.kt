package com.example.starstec.ui.activity.realtimeresult

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.starstec.R
import com.example.starstec.databinding.ActivityRealTimeDetectionResultBinding
import com.example.starstec.ui.activity.MainActivity
import com.example.starstec.ui.activity.realtimedetection.RealtimeStressDetectionActivity

class RealTimeDetectionResultActivity : AppCompatActivity() {

    private lateinit var binding : ActivityRealTimeDetectionResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRealTimeDetectionResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnback.setOnClickListener {
            val intent = Intent(this, RealtimeStressDetectionActivity::class.java)
            startActivity(intent)
        }

        val testResult = intent.getStringArrayListExtra("arrayResult")
        if(testResult != null ){
            binding.firstMinute.text = testResult[0]
            binding.secondMinute.text = testResult[1]
            binding.thirdMinute.text = testResult[2]
            binding.fourthMinute.text = testResult[3]
            binding.fifthMinute.text = testResult[4]
        }

        val hrResult = intent.getIntegerArrayListExtra("arrayHR")
        if(hrResult != null){
            var sum = 0;
            hrResult.forEach { hr ->
                sum += hr
            }
            val meanHR = sum/4
            binding.hrRate.text = meanHR.toString()
        }

        val hrvResult = intent.getIntegerArrayListExtra("arrayHRV")
        if(hrvResult != null){
            var sum = 0;
            hrvResult.forEach { hrv ->
                sum += hrv
            }
            val meanHRV = sum/4
            binding.hrvRate.text = meanHRV.toString()
        }

        val scrResult = intent.getIntegerArrayListExtra("arraySCR")
        if(scrResult != null){
            var sum = 0;
            scrResult.forEach { hr ->
                sum += hr
            }
            val meanSCR = sum/4
            binding.hrRate.text = meanSCR.toString()
        }

    }
}