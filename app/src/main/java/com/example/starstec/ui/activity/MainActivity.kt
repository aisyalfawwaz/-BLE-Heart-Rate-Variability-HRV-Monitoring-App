package com.example.starstec.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.starstec.databinding.ActivityMainBinding
import com.example.starstec.ui.activity.ble.BleServiceHolder
import com.example.starstec.ui.fragment.DashboardFragment
import com.masselis.rxbluetoothkotlin.*
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val device by lazy { intent.getParcelableExtra<BluetoothDevice>(DEVICE_EXTRA)!! }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) states.onNext(States.Connecting)
            else finish()
        }
    private val states = BehaviorSubject.createDefault<States>(States.Connecting)

    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("SetTextI18n", "CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        states
            .distinctUntilChanged()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { state ->
                when (state) {
                    States.Connecting -> {
//                        binding.connectingGroup.visibility = View.VISIBLE
//                        binding.connectedGroup.visibility = View.GONE
                    }

                    is States.Connected -> {
//                        binding.connectingGroup.visibility = View.GONE
//                        binding.connectedGroup.visibility = View.VISIBLE
                    }
                }
            }


        states
            .filter { it is States.Connecting }
            .switchMapSingle { device.connectRxGatt() }
            .onErrorComplete {
                if (it is NeedBluetoothConnectPermission) {
                    permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    true
                } else
                    false
            }
            .switchMapMaybe { gatt -> gatt.whenConnectionIsReady().map { gatt } }
            .observeOn(AndroidSchedulers.mainThread())
//            .doOnSubscribe { binding.connectingGroup.visibility = View.VISIBLE }
//            .doFinally { binding.connectingGroup.visibility = View.INVISIBLE }
            .subscribe(
                {
                    Toast.makeText(this, "Starstec Connected", Toast.LENGTH_SHORT).show()
                    states.onNext(States.Connected(it))
                    BleServiceHolder.bleService = it
                },
                {
                    val message =
                        when (it) {
                            is BluetoothIsTurnedOff -> "Bluetooth is turned off"
                            is DeviceDisconnected -> "Unable to connect to the device"
                            else -> "Starstec Still Connected"
                        }
//                    AlertDialog.Builder(this).setMessage(message).show()
                }
            )

        // Handle more subscription blocks...

        // Example: Handle fragment transaction
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, DashboardFragment())
            .commit()
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        (states.value as? States.Connected)?.gatt?.source?.disconnect()
        super.onDestroy()
    }



    private sealed class States {
        object Connecting : States()
        class Connected(val gatt: RxBluetoothGatt) : States()
    }


    companion object {
        fun intent(context: Context, device: BluetoothDevice): Intent =
            Intent(context, MainActivity::class.java)
                .putExtra(DEVICE_EXTRA, device)

        private const val DEVICE_EXTRA = "DEVICE_EXTRA"
    }
}
