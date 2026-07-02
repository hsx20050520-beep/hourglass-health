package com.hourglass.health.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.hourglass.health.model.BandStatus
import com.hourglass.health.model.HealthData
import com.hourglass.health.model.SleepData
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Manages BLE connection to Xiaomi Smart Band 9 Pro.
 *
 * Reference protocol: Gadgetbridge's Xiaomi implementation
 * (https://github.com/Freeyourgadget/Gadgetbridge)
 *
 * Xiaomi Band 9 Pro BLE Service UUIDs:
 * - Xiaomi Service: 0000FEE0-0000-1000-8000-00805F9B34FB
 * - Xiaomi Auth Service: 0000FEE1-0000-1000-8000-00805F9B34FB
 * - Current Time Service: 00001805-0000-1000-8000-00805F9B34FB
 * - Device Information: 0000180A-0000-1000-8000-00805F9B34FB
 * - Battery Service: 0000180F-0000-1000-8000-00805F9B34FB
 */
class BandConnectionManager(private val context: Context) {

    companion object {
        private const val TAG = "BandConnection"
        val XIAOMI_SERVICE = UUID.fromString("0000FEE0-0000-1000-8000-00805F9B34FB")
        val XIAOMI_AUTH_SERVICE = UUID.fromString("0000FEE1-0000-1000-8000-00805F9B34FB")
        val BATTERY_SERVICE = UUID.fromString("0000180F-0000-1000-8000-00805F9B34FB")
        val BATTERY_LEVEL_CHAR = UUID.fromString("00002A19-0000-1000-8000-00805F9B34FB")
        val DEVICE_INFO_SERVICE = UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB")
        val CURRENT_TIME_SERVICE = UUID.fromString("00001805-0000-1000-8000-00805F9B34FB")

        // Xiaomi characteristic UUIDs (from Gadgetbridge)
        val XIAOMI_CHAR_WRITE = UUID.fromString("0000FED0-0000-1000-8000-00805F9B34FB")
        val XIAOMI_CHAR_NOTIFY = UUID.fromString("0000FED1-0000-1000-8000-00805F9B34FB")
        val XIAOMI_CHAR_READ = UUID.fromString("0000FED2-0000-1000-8000-00805F9B34FB")
        val XIAOMI_AUTH_CHAR = UUID.fromString("0000FED3-0000-1000-8000-00805F9B34FB")

        // Xiaomi command constants (from Gadgetbridge protocol analysis)
        private const val CMD_SEND_TIME = 0x03
        private const val CMD_FETCH_DATA = 0x06
        private const val CMD_HEART_RATE = 0x08
        private const val CMD_SLEEP_DATA = 0x09
        private const val CMD_ACTIVITY_DATA = 0x0A
        private const val CMD_BATTERY = 0x0C
        private const val CMD_DEVICE_INFO = 0x10
        private const val DATA_TYPE_SLEEP = 0x02
        private const val DATA_TYPE_HEART_RATE = 0x03
        private const val DATA_TYPE_ACTIVITY = 0x01

        private val TARGET_BAND_NAME_PATTERN = Regex("^Xiaomi Smart Band 9 Pro", RegexOption.IGNORE_CASE)
    }

    var statusCallback: ((BandStatus) -> Unit)? = null
    var healthDataCallback: ((HealthData) -> Unit)? = null
    var sleepDataCallback: ((SleepData) -> Unit)? = null

    private var bluetoothGatt: BluetoothGatt? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var deviceAddress: String = ""

    // Characteristic references
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null
    private var authCharacteristic: BluetoothGattCharacteristic? = null

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Connected to ${gatt.device.name}")
                    bluetoothGatt = gatt
                    // Set MTU for larger data packets
                    gatt.requestMtu(512)
                    statusCallback?.invoke(BandStatus(connected = true, deviceName = gatt.device.name ?: "", deviceAddress = gatt.device.address))
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected")
                    bluetoothGatt = null
                    statusCallback?.invoke(BandStatus())
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Log.d(TAG, "MTU changed to $mtu")
            gatt.discoverServices()
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Service discovery failed: $status")
                return
            }
            Log.d(TAG, "Services discovered")

            // Find Xiaomi service characteristics
            val xiaomiService = gatt.getService(XIAOMI_SERVICE)
            val xiaomiAuthService = gatt.getService(XIAOMI_AUTH_SERVICE)

            writeCharacteristic = xiaomiService?.getCharacteristic(XIAOMI_CHAR_WRITE)
            notifyCharacteristic = xiaomiService?.getCharacteristic(XIAOMI_CHAR_NOTIFY)
            authCharacteristic = xiaomiAuthService?.getCharacteristic(XIAOMI_AUTH_CHAR)
                ?: xiaomiService?.getCharacteristic(XIAOMI_AUTH_CHAR)

            // Enable notifications
            notifyCharacteristic?.let { char ->
                gatt.setCharacteristicNotification(char, true)
                char.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805F9B34FB"))?.let { desc ->
                    desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(desc)
                }
            }

            authCharacteristic?.let { char ->
                gatt.setCharacteristicNotification(char, true)
                char.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805F9B34FB"))?.let { desc ->
                    desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(desc)
                }
            }

            // Request battery level
            readBatteryLevel(gatt)

            // Start health data sync after a short delay
            mainHandler.postDelayed({ syncAllHealthData() }, 2000)
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val uuid = characteristic.uuid
                val value = characteristic.value
                if (uuid == BATTERY_LEVEL_CHAR && value.isNotEmpty()) {
                    val level = value[0].toInt() and 0xFF
                    Log.d(TAG, "Battery level: $level%")
                    statusCallback?.invoke(BandStatus(
                        connected = true,
                        deviceName = gatt.device.name ?: "",
                        deviceAddress = gatt.device.address,
                        batteryLevel = level
                    ))
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val value = characteristic.value ?: return
            parseNotificationData(value)
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            Log.d(TAG, "Write result: $status for ${characteristic.uuid}")
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            Log.d(TAG, "Descriptor write: $status")
        }
    }

    fun connect(device: BluetoothDevice) {
        deviceAddress = device.address
        Log.d(TAG, "Connecting to ${device.name} at $deviceAddress")
        device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    fun isConnected(): Boolean = bluetoothGatt != null

    private fun readBatteryLevel(gatt: BluetoothGatt) {
        val batteryService = gatt.getService(BATTERY_SERVICE)
        val batteryChar = batteryService?.getCharacteristic(BATTERY_LEVEL_CHAR)
        if (batteryChar != null) {
            gatt.readCharacteristic(batteryChar)
        }
    }

    fun syncAllHealthData() {
        val gatt = bluetoothGatt ?: return
        // Request health data sync via Xiaomi protocol
        // Format: command byte + data type + params
        val syncCommand = byteArrayOf(CMD_FETCH_DATA.toByte(), DATA_TYPE_SLEEP.toByte(), 0x01, 0x00)
        writeCharacteristic?.let { char ->
            char.value = syncCommand
            gatt.writeCharacteristic(char)
        }
    }

    private fun parseNotificationData(data: ByteArray) {
        if (data.isEmpty()) return
        val cmd = data[0].toInt() and 0xFF
        when (cmd) {
            CMD_SLEEP_DATA -> parseSleepResponse(data)
            CMD_HEART_RATE -> parseHeartRateResponse(data)
            0x05 -> Log.d(TAG, "Health data response received (len=${data.size})")
            else -> Log.d(TAG, "Unknown notification cmd=0x${cmd.toString(16)} len=${data.size}")
        }
    }

    private fun parseSleepResponse(data: ByteArray) {
        // Xiaomi sleep data format (simplified):
        // Byte 0: cmd (0x09)
        // Byte 1-4: timestamp
        // Byte 5: total hours
        // Byte 6: deep sleep hours
        // Byte 7: light sleep hours
        // ...
        Log.d(TAG, "Sleep data received: ${data.toHexString()}")
        // Parse based on Gadgetbridge protocol
        if (data.size >= 10) {
            val totalH = data[5].toInt() and 0xFF
            val totalM = data[6].toInt() and 0xFF
            val deepH = data[7].toInt() and 0xFF
            val deepM = data[8].toInt() and 0xFF
            val totalMinutes = totalH * 60 + totalM
            val deepMinutes = deepH * 60 + deepM
            val lightMinutes = totalMinutes - deepMinutes

            sleepDataCallback?.invoke(SleepData(
                date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
                bedTime = System.currentTimeMillis() - totalMinutes * 60 * 1000L,
                wakeTime = System.currentTimeMillis(),
                totalMinutes = totalMinutes,
                deepMinutes = deepMinutes.coerceAtLeast(0),
                lightMinutes = lightMinutes.coerceAtLeast(0),
                remMinutes = 0,
                awakeMinutes = 0
            ))
        }
    }

    private fun parseHeartRateResponse(data: ByteArray) {
        if (data.size >= 3) {
            val hr = data[1].toInt() and 0xFF
            Log.d(TAG, "Heart rate: $hr")
            healthDataCallback?.invoke(HealthData(heartRate = hr))
        }
    }

    fun requestHeartRate() {
        val gatt = bluetoothGatt ?: return
        val cmd = byteArrayOf(CMD_HEART_RATE.toByte(), 0x01)
        writeCharacteristic?.let { char ->
            char.value = cmd
            gatt.writeCharacteristic(char)
        }
    }

    companion object {
        fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }
    }
}
