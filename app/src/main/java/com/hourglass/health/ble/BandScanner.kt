package com.hourglass.health.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.regex.Pattern

class BandScanner(private val context: Context) {

    companion object {
        private const val TAG = "BandScanner"
        private const val SCAN_DURATION_MS = 12000L

        // Xiaomi Smart Band 9 Pro name pattern
        val TARGET_NAME_PATTERN = Pattern.compile(
            "^Xiaomi Smart Band 9 Pro [0-9A-F]{4}$",
            Pattern.CASE_INSENSITIVE
        )
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
        manager.adapter
    }
    private val scanner: BluetoothLeScanner? by lazy { bluetoothAdapter?.bluetoothLeScanner }
    private val handler = Handler(Looper.getMainLooper())

    var onDeviceFound: ((android.bluetooth.BluetoothDevice) -> Unit)? = null
    var onScanFinished: (() -> Unit)? = null
    var onScanError: ((String) -> Unit)? = null

    private var isScanning = false

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name ?: ""
            Log.d(TAG, "Found: $name [${device.address}] RSSI=${result.rssi}")

            if (name.isNotEmpty() && TARGET_NAME_PATTERN.matcher(name).matches()) {
                Log.d(TAG, "✅ Mi Band 9 Pro found: $name")
                stopScan()
                onDeviceFound?.invoke(device)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            isScanning = false
            val msg = when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> "Scan already started"
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "App registration failed"
                SCAN_FAILED_FEATURE_UNSUPPORTED -> "BLE scan unsupported"
                SCAN_FAILED_INTERNAL_ERROR -> "Internal scan error"
                else -> "Unknown error: $errorCode"
            }
            Log.e(TAG, "Scan failed: $msg")
            onScanError?.invoke(msg)
            onScanFinished?.invoke()
        }
    }

    fun startScan() {
        if (isScanning) return
        if (scanner == null) {
            onScanError?.invoke("Bluetooth LE scanner not available")
            return
        }
        isScanning = true
        Log.d(TAG, "Starting BLE scan for Mi Band 9 Pro...")

        val filters = listOf(
            ScanFilter.Builder()
                // Not filtering by service UUID since Xiaomi band may vary
                .build()
        )
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()

        scanner?.startScan(filters, settings, scanCallback)

        // Auto-stop scan after timeout
        handler.postDelayed({
            if (isScanning) {
                stopScan()
                onScanFinished?.invoke()
            }
        }, SCAN_DURATION_MS)
    }

    fun stopScan() {
        if (isScanning) {
            scanner?.stopScan(scanCallback)
            isScanning = false
            Log.d(TAG, "Scan stopped")
        }
    }

    fun isScanning(): Boolean = isScanning
}
