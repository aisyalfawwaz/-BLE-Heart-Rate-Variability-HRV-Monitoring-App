package com.example.starstec.ui.activity.realtimedetection

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.starstec.databinding.ActivityRealtimeStressDetectionBinding
import com.example.starstec.ui.activity.MainActivity
import okhttp3.*
import java.io.IOException

class RealtimeStressDetectionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRealtimeStressDetectionBinding

    private lateinit var client: OkHttpClient
    private lateinit var handler: Handler
    private lateinit var hrTextView: TextView
    private lateinit var hrvTextView: TextView
    private lateinit var scrTextView: TextView

    private lateinit var previousHR: String
    private lateinit var previousHRV: String
    private lateinit var previousSCR: String

    private var isToastDisplayed: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRealtimeStressDetectionBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.btnback.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        hrTextView = binding.hr
        hrvTextView = binding.hrv
        scrTextView = binding.scr

        client = OkHttpClient()
        handler = Handler()

        previousHR = ""
        previousHRV = ""
        previousSCR = ""

        binding.btndetectstress.setOnClickListener {
            startRequestingData()
        }
    }

    private fun startRequestingData() {
        val request = Request.Builder()
            .url("http://192.168.192.32") // Ganti dengan URL API yang sesuai
            .build()

        handler.post(object : Runnable {
            override fun run() {
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        // Tangani jika terjadi kegagalan saat melakukan request
                        e.printStackTrace()
                        if (!isToastDisplayed) {
                            showToast("Gelang tidak terhubung")
                            isToastDisplayed = true
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val responseData = response.body()?.string()

                        // Tangani data yang diterima dari API
                        handler.post(object : Runnable {
                            override fun run() {
                                handleResponseData(responseData)
                            }
                        })
                    }
                })

                // Set delay waktu antara request berikutnya
                handler.postDelayed(this, 1000) // Ganti dengan delay yang sesuai (dalam milidetik)
            }
        })
    }

    private fun handleResponseData(responseData: String?) {
        // Pastikan responseData tidak null
        responseData?.let {
            // Parsing data dari responseData
            val data = it.split("\n")
            var hr = ""
            var hrv = ""
            var scr = ""

            for (item in data) {
                val keyValue = item.split(": ")
                if (keyValue.size == 2) {
                    val key = keyValue[0]
                    val value = keyValue[1]

                    when (key) {
                        "Heart Rate" -> hr = value
                        "Heart Rate Variability (HRV)" -> hrv = value
                        "EDA" -> scr = value
                    }
                }
            }

            // Update TextView dengan data yang telah diparsing
            runOnUiThread {
                if (hr != previousHR) {
                    hrTextView.text = "$hr"
                    previousHR = hr
                }
                if (hrv != previousHRV) {
                    hrvTextView.text = hrv.take(2)
                    previousHRV = hrv
                }
                if (scr != previousSCR) {
                    scrTextView.text = "$scr"
                    previousSCR = scr
                }

                if (!isToastDisplayed && hr.isEmpty() && hrv.isEmpty() && scr.isEmpty()) {
                    showToast("Gelang tidak terhubung")
                    isToastDisplayed = true
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        handler.removeCallbacksAndMessages(null)
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }
}
