package com.example.starstec.ui.activity.ppgactivity

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.starstec.databinding.ActivityBloodPressureBinding
import com.example.starstec.ui.activity.MainActivity

class PpgSensorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBloodPressureBinding

    @SuppressLint("QueryPermissionsNeeded")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBloodPressureBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.btnback.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        binding.ppgcard.setOnClickListener {
            val youtubeUrl =
                "https://www.youtube.com/watch?v=vLLp5b6FlyM&ab_channel=AirlanggaTravel"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(youtubeUrl))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.setPackage("com.google.android.youtube")
            startActivity(intent)
        }
    }
}
