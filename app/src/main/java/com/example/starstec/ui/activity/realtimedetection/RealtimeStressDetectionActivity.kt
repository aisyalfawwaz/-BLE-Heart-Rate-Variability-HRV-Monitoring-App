package com.example.starstec.ui.activity.realtimedetection

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.Layout.Alignment
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.starstec.R
import com.example.starstec.databinding.ActivityRealtimeStressDetectionBinding
import com.example.starstec.ui.activity.MainActivity
import com.example.starstec.ui.activity.ble.BleServiceHolder
import com.example.starstec.ui.activity.realtimeresult.RealTimeDetectionResultActivity
import com.masselis.rxbluetoothkotlin.findCharacteristic
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Maybe
import java.util.UUID

@Suppress("DEPRECATION")
class RealtimeStressDetectionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRealtimeStressDetectionBinding

    private lateinit var handler: Handler
    private lateinit var hrvTextView: TextView
    private lateinit var hrTextView: TextView
    private lateinit var meanTextView: TextView
    private lateinit var realTimeResult : TextView
    private lateinit var seeResult : TextView

    private var hrCollected = arrayListOf<Int>()
    private var hrvCollected = arrayListOf<Int>()
    private var scrCollected = arrayListOf<Int>()
    private var arrayResult = arrayListOf<String>()

    private var previousHRV: String = ""
    private var previousHR: String = ""
    private var previousMean: String = ""
    private var isDetecting: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRealtimeStressDetectionBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.btnback.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        hrvTextView = binding.hrv
        hrTextView = binding.hr
        meanTextView = binding.scr
        realTimeResult = binding.realtimeResult
        seeResult = binding.seeResult
        handler = Handler()

        previousHRV = ""
        previousHR = ""
        previousMean = ""
        isDetecting = false

        binding.btndetectstress.setOnClickListener {
            if (!isDetecting) {
                startDetection()
            }
        }

        seeResult.setOnClickListener{
            val intent = Intent(this, RealTimeDetectionResultActivity::class.java)
            intent.putExtra("arrayHR", hrCollected)
            intent.putExtra("arrayHRV", hrvCollected)
            intent.putExtra("arraySCR", scrCollected)
            intent.putExtra("arrayResult", arrayResult)
            startActivity(intent)
        }
    }

    @SuppressLint("CheckResult")
    private fun startDetection() {
        isDetecting = true
        binding.btndetectstress.isEnabled = false // Disable the button during detection
        animateHeartRateDetection(5 * 60 * 1000) // 5 minutes animation

        // Schedule data reading every 1 minute for 5 minutes
        val totalDetectionTime = 5 * 60 * 1000 // 5 minutes
        val interval = 60 * 1000 // 1 minute
        var elapsedTime = 0
        val readingRunnable = object : Runnable {
            override fun run() {
                if (elapsedTime < totalDetectionTime) {
                    startReadingHRVData()
                    handler.postDelayed(this, interval.toLong())
                    elapsedTime += interval
                } else {
                    // Detection finished
                    isDetecting = false
                    binding.btndetectstress.isEnabled = true // Enable the button
                    showToast("Stress detection finished")
                    seeResult.visibility = View.VISIBLE
                }
            }
        }
        handler.postDelayed(readingRunnable, interval.toLong())
    }

    private fun animateHeartRateDetection(animationDuration: Long) {
        val heartRateImage = binding.btndetectstress
        val rotation = ObjectAnimator.ofFloat(heartRateImage, "rotation", 0f, 360f)
        rotation.duration = animationDuration
        rotation.interpolator = LinearInterpolator()
        rotation.repeatCount = ObjectAnimator.INFINITE
        rotation.start()
    }

    @SuppressLint("CheckResult")
    private fun startReadingHRVData() {
        val gatt =
            BleServiceHolder.bleService // Retrieve the BluetoothGatt instance from BleServiceHolder

        // Check if the gatt instance is valid
        if (gatt != null) {
            Maybe.defer {
                if (gatt.source.services.isEmpty()) gatt.discoverServices()
                else Maybe.just(gatt.source.services)
            }
                // HRV characteristic
                .flatMap { gatt.read(gatt.source.findCharacteristic(UUID.fromString("00002A42-0000-1000-8000-00805F9B34FB"))!!) }
                .map { it[0].toInt() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { hrvValue ->
                        hrvCollected.add(hrvValue)
                        if (hrvValue.toString() != previousHRV) {
                            hrvTextView.text = hrvValue.toString()
                            previousHRV = hrvValue.toString()
                            if (evaluateStressFromHRV(hrvValue)) {
                                arrayResult.add("Stress")
                                realTimeResult.text = "You are currently under stress\n Slow down a little bit!"
                                realTimeResult.setTextColor(0xFF0000.toInt())
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    showStressNotification()
                                }
                            }else{
                                arrayResult.add("Non-Stress")
                                realTimeResult.text = "You are currently on a normal status\n (Non-Stress)"
                                realTimeResult.setTextColor(0xFF00FF00.toInt())
                            }
                        }
                    },
                    { error ->
                        // Handle error
                        showToast("Error reading HRV: ${error.message}")
                        arrayResult.add("null")
                    }
                )

            Maybe.defer {
                if (gatt.source.services.isEmpty()) gatt.discoverServices()
                else Maybe.just(gatt.source.services)
            }
                // HR characteristic (replace the UUID with the actual UUID of the HR characteristic)
                .flatMap { gatt.read(gatt.source.findCharacteristic(UUID.fromString("00002A37-0000-1000-8000-00805F9B34FB"))!!) }
                .map { it[0].toInt() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { hrValue ->
                        hrCollected.add(hrValue)
                        if (hrValue.toString() != previousHR) {
                            hrTextView.text = hrValue.toString()
                            previousHR = hrValue.toString()
                        }
                    },
                    { error ->
                        // Handle error
                        showToast("Error reading HR: ${error.message}")
                    }
                )

            Maybe.defer {
                if (gatt.source.services.isEmpty()) gatt.discoverServices()
                else Maybe.just(gatt.source.services)
            }
                // Mean characteristic (replace the UUID with the actual UUID of the Mean characteristic)
                .flatMap { gatt.read(gatt.source.findCharacteristic(UUID.fromString("00002A40-0000-1000-8000-00805F9B34FB"))!!) }
                .map { it[0].toInt() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { meanValue ->
                        scrCollected.add(meanValue)
                        if (meanValue.toString() != previousMean) {
                            meanTextView.text = meanValue.toString()
                            previousMean = meanValue.toString()
                        }
                    },
                    { error ->
                        // Handle error
                        showToast("Error reading Mean: ${error.message}")
                    }
                )
        } else {
            showToast("Starstec not connected")
        }
    }

    private fun evaluateStressFromHRV(hrvValue: Int): Boolean {
        // Replace these threshold values with your own based on literature
        val highStressThreshold = 90 // Example threshold for high stress
        val lowStressThreshold = 45 // Example threshold for low stress

        return hrvValue > highStressThreshold || hrvValue < lowStressThreshold
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun showStressNotification() {
        val notificationChannelId = "StressChannel"
        val notificationBuilder = NotificationCompat.Builder(this, notificationChannelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Stress Notification")
            .setContentText("You are indicated to be under stress.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)

        // Create a notification channel
        val channel = NotificationChannel(
            notificationChannelId,
            "Stress Notifications",
            NotificationManager.IMPORTANCE_HIGH
        )
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        // Show the notification
        with(NotificationManagerCompat.from(this)) {
            notify(1, notificationBuilder.build())
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
