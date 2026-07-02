package com.hourglass.health

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.tabs.TabLayout
import com.hourglass.health.ble.BandConnectionManager
import com.hourglass.health.model.BandStatus
import com.hourglass.health.model.SleepData
import com.hourglass.health.model.WaterReminderConfig
import com.hourglass.health.sleep.SleepAnalyzer
import com.hourglass.health.water.WaterReminderService
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var bandManager: BandConnectionManager
    private lateinit var bandStatusText: TextView
    private lateinit var bandBatteryText: TextView
    private lateinit var connectButton: Button
    private lateinit var container: LinearLayout
    private lateinit var sleepScoreText: TextView
    private lateinit var sleepDetailText: TextView
    private lateinit var sleepAdviceText: TextView
    private lateinit var sleepTipsList: LinearLayout
    private lateinit var sleepCard: LinearLayout
    private lateinit var waterToggle: Switch
    private lateinit var waterIntervalSeek: SeekBar
    private lateinit var waterIntervalLabel: TextView
    private lateinit var waterStatusText: TextView
    private lateinit var waterCard: LinearLayout
    private lateinit var hrValueText: TextView
    private lateinit var hrStatusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions()

        container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFFAF6F0.toInt())
        }

        buildHeader()
        buildTabLayout()

        val scrollContent = ScrollView(this)
        val contentInner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(32))
        }

        sleepCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.VISIBLE
        }
        buildSleepSection(sleepCard)
        contentInner.addView(sleepCard)

        waterCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        buildWaterSection(waterCard)
        contentInner.addView(waterCard)

        val bandCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        buildBandSection(bandCard)
        contentInner.addView(bandCard)

        scrollContent.addView(contentInner)
        container.addView(scrollContent)
        setContentView(container)

        bandManager = BandConnectionManager(this) { status ->
            runOnUiThread { updateBandStatus(status) }
        }
    }

    private fun buildHeader() {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(48), dp(20), dp(16))
        }
        header.addView(TextView(this).apply {
            text = "\u23f3 健康沙漏"
            textSize = 26f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(0xFF1E293B.toInt())
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        })
        header.addView(TextView(this).apply {
            text = "v1.0.0"
            textSize = 12f
            setTextColor(0xFF94A3B8.toInt())
        })
        container.addView(header)
    }

    private fun buildTabLayout() {
        val tabLayout = TabLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            setSelectedTabIndicatorColor(0xFFD4A017.toInt())
            setTabTextColors(0xFF94A3B8.toInt(), 0xFFD4A017.toInt())
        }
        tabLayout.addTab(tabLayout.newTab().setText("\ud83d\udca4 睡眠分析"))
        tabLayout.addTab(tabLayout.newTab().setText("\ud83d\udca7 喝水提醒"))
        tabLayout.addTab(tabLayout.newTab().setText("\u231a 手环数据"))
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val pos = tab?.position ?: 0
                sleepCard.visibility = if (pos == 0) View.VISIBLE else View.GONE
                waterCard.visibility = if (pos == 1) View.VISIBLE else View.GONE
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        container.addView(tabLayout)

        val statusBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(12))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xFFFFF8DC.toInt())
                cornerRadius = dp(2).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                setMargins(dp(16), 0, dp(16), 0)
            }
        }
        statusBar.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(10), dp(10)).apply { marginEnd = dp(8) }
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(0xFF94A3B8.toInt())
            }
        })
        bandStatusText = TextView(this).apply {
            text = "手环未连接"
            textSize = 13f
            setTextColor(0xFF94A3B8.toInt())
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }
        statusBar.addView(bandStatusText)
        bandBatteryText = TextView(this).apply {
            text = ""
            textSize = 12f
            setTextColor(0xFF94A3B8.toInt())
            visibility = View.GONE
        }
        statusBar.addView(bandBatteryText)
        container.addView(statusBar)
    }

    private fun buildSleepSection(parent: LinearLayout) {
        parent.addView(sectionTitle("\ud83d\udca4 昨晚睡眠分析"))
        parent.addView(spacer(dp(12)))

        sleepScoreText = TextView(this).apply {
            gravity = android.view.Gravity.CENTER
            textSize = 48f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(80))
        }
        parent.addView(sleepScoreText)
        parent.addView(spacer(dp(8)))

        sleepDetailText = TextView(this).apply {
            gravity = android.view.Gravity.CENTER
            textSize = 14f
            setTextColor(0xFF1E293B.toInt())
            text = "暂无睡眠数据\n连接手环后自动同步"
        }
        parent.addView(sleepDetailText)
        parent.addView(spacer(dp(16)))

        val adviceBg = android.graphics.drawable.GradientDrawable().apply {
            setColor(0xFFFFFEF5.toInt())
            cornerRadius = dp(8).toFloat()
            setStroke(dp(1), 0xFFE2E8F0.toInt())
        }
        sleepAdviceText = TextView(this).apply {
            textSize = 13f
            setTextColor(0xFF1E293B.toInt())
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = adviceBg
            text = "暂无数据，连接手环后查看分析建议"
        }
        parent.addView(sleepAdviceText)
        parent.addView(spacer(dp(12)))

        sleepTipsList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        parent.addView(sleepTipsList)
        parent.addView(spacer(dp(16)))

        val demoBtn = Button(this).apply {
            text = "\ud83d\udcca 加载示例数据"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 14f
            setBackgroundColor(0xFFD4A017.toInt())
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(44))
        }
        demoBtn.setOnClickListener { loadDemoData() }
        parent.addView(demoBtn)
    }

    private fun buildWaterSection(parent: LinearLayout) {
        parent.addView(sectionTitle("\ud83d\udca7 喝水提醒"))
        parent.addView(spacer(dp(12)))

        val toggleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        toggleRow.addView(TextView(this).apply {
            text = "开启喝水提醒"
            textSize = 16f
            setTextColor(0xFF1E293B.toInt())
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        })
        waterToggle = Switch(this).apply {
            isChecked = WaterReminderService.isServiceRunning(this@MainActivity)
            setOnCheckedChangeListener { _, isChecked -> toggleWaterReminder(isChecked) }
        }
        toggleRow.addView(waterToggle)
        parent.addView(toggleRow)
        parent.addView(spacer(dp(16)))

        parent.addView(TextView(this).apply {
            text = "提醒间隔"
            textSize = 14f
            setTextColor(0xFF1E293B.toInt())
        })
        parent.addView(spacer(dp(8)))

        waterIntervalLabel = TextView(this).apply {
            gravity = android.view.Gravity.CENTER
            textSize = 36f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(0xFF3B82F6.toInt())
        }
        parent.addView(waterIntervalLabel)

        waterIntervalSeek = SeekBar(this).apply {
            max = 4
            progress = 1
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    val min = intervalFromProgress(p)
                    waterIntervalLabel.text = formatInterval(min)
                    WaterReminderService.setInterval(this@MainActivity, min)
                    if (waterToggle.isChecked) restartWaterReminder()
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        parent.addView(waterIntervalSeek)

        val initMin = WaterReminderService.getInterval(this)
        waterIntervalSeek.progress = progressFromInterval(initMin)
        waterIntervalLabel.text = formatInterval(initMin)

        parent.addView(spacer(dp(8)))
        parent.addView(TextView(this).apply {
            gravity = android.view.Gravity.CENTER
            text = "30\u5206\u949f    1\u5c0f\u65f6    1.5\u5c0f\u65f6    2\u5c0f\u65f6    3\u5c0f\u65f6"
            textSize = 11f
            setTextColor(0xFF94A3B8.toInt())
        })
        parent.addView(spacer(dp(16)))

        waterStatusText = TextView(this).apply {
            gravity = android.view.Gravity.CENTER
            textSize = 13f
            setTextColor(0xFF94A3B8.toInt())
        }
        parent.addView(waterStatusText)
        parent.addView(spacer(dp(16)))

        val tipBg = android.graphics.drawable.GradientDrawable().apply {
            setColor(0xFFDBEAFE.toInt())
            cornerRadius = dp(8).toFloat()
        }
        parent.addView(TextView(this).apply {
            text = "\ud83d\udca1 每日饮水建议：\n\u2022 男性约 2.5-3L / 女性约 2-2.5L\n\u2022 每次喝 150-200ml\n\u2022 早起一杯温水\n\u2022 餐前半小时一杯"
            textSize = 13f
            setTextColor(0xFF1E293B.toInt())
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = tipBg
        })
    }

    private fun buildBandSection(parent: LinearLayout) {
        parent.addView(sectionTitle("\u231a 手环连接"))
        parent.addView(spacer(dp(12)))

        connectButton = Button(this).apply {
            text = "\ud83d\udd0d 搜索并连接手环"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 15f
            setBackgroundColor(0xFF3B82F6.toInt())
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(50))
        }
        connectButton.setOnClickListener {
            if (bandManager.isConnected()) bandManager.disconnect()
            else bandManager.startScan()
        }
        parent.addView(connectButton)
        parent.addView(spacer(dp(16)))

        hrValueText = TextView(this).apply {
            gravity = android.view.Gravity.CENTER
            textSize = 48f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(0xFFEF4444.toInt())
            text = "--"
        }
        parent.addView(hrValueText)
        hrStatusText = TextView(this).apply {
            gravity = android.view.Gravity.CENTER
            textSize = 13f
            setTextColor(0xFF94A3B8.toInt())
            text = "\u2764 心率 (bpm)"
        }
        parent.addView(hrStatusText)
    }

    private fun updateBandStatus(status: BandStatus) {
        bandStatusText.text = if (status.connected) "\u2705 " + status.deviceName + " 已连接" else "手环未连接"
        connectButton.text = if (status.connected) "断开连接" else "\ud83d\udd0d 搜索并连接手环"
        if (status.batteryLevel >= 0) {
            bandBatteryText.text = "\ud83d\udd0b " + status.batteryLevel + "%"
            bandBatteryText.visibility = View.VISIBLE
        }
    }

    private fun loadDemoData() {
        val cal = Calendar.getInstance()
        val bedTime = cal.apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 30) }.timeInMillis
        val wakeTime = cal.apply { set(Calendar.HOUR_OF_DAY, 7); set(Calendar.MINUTE, 15) }.timeInMillis

        val demo = SleepData(
            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            bedTime = bedTime, wakeTime = wakeTime,
            totalMinutes = 465, deepMinutes = 98,
            lightMinutes = 210, remMinutes = 112, awakeMinutes = 45,
            avgHeartRate = 58, avgSpo2 = 97, respiratoryRate = 16.5f
        )
        val analysis = SleepAnalyzer.analyze(demo)

        sleepScoreText.text = "${analysis.score}"
        sleepScoreText.setTextColor(scoreColor(analysis.score))

        val deepPct = "%.0f".format(analysis.deepPercent)
        val lightPct = "%.0f".format(analysis.lightPercent)
        val remPct = "%.0f".format(analysis.remPercent)
        sleepDetailText.text = "总睡眠 %.1f 小时\n深睡 $deepPct%% - 浅睡 $lightPct%% - REM $remPct%%\n质量评级: ${analysis.label}".format(analysis.totalHours)

        sleepAdviceText.text = buildString {
            append("\ud83d\ude0c 建议作息：${analysis.recommendedBedTime} \u2192 ${analysis.recommendedWakeTime}\n")
            if (analysis.issues.isNotEmpty()) {
                append("\n\u26a0\ufe0f 问题：\n")
                analysis.issues.forEach { append("\u2022 $it\n") }
            }
        }

        sleepTipsList.removeAllViews()
        analysis.tips.forEach { tip ->
            sleepTipsList.addView(TextView(this).apply {
                text = "\u2705 $tip"
                textSize = 13f
                setTextColor(0xFF1E293B.toInt())
                setPadding(dp(8), dp(6), dp(8), dp(6))
            })
        }
    }

    private fun toggleWaterReminder(enabled: Boolean) {
        val intent = Intent(this, WaterReminderService::class.java)
        if (enabled) {
            intent.action = WaterReminderService.ACTION_START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
            else startService(intent)
            waterStatusText.text = "\u2705 喝水提醒已开启"
        } else {
            intent.action = WaterReminderService.ACTION_STOP
            stopService(intent)
            waterStatusText.text = "喝水提醒已关闭"
        }
    }

    private fun restartWaterReminder() {
        val intent = Intent(this, WaterReminderService::class.java)
        stopService(intent)
        intent.action = WaterReminderService.ACTION_START
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
        else startService(intent)
    }

    private fun requestPermissions() {
        val perms = listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS
        ).filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (perms.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, perms.toTypedArray(), 100)
        }
    }

    private fun sectionTitle(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 18f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        setTextColor(0xFF1E293B.toInt())
    }

    private fun spacer(h: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, h)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun scoreColor(s: Int): Int = when {
        s >= 80 -> 0xFF16A34A.toInt()
        s >= 60 -> 0xFF3B82F6.toInt()
        s >= 40 -> 0xFFD97706.toInt()
        else -> 0xFFDC2626.toInt()
    }

    private fun formatInterval(minutes: Int): String = when {
        minutes < 60 -> minutes.toString() + "分钟"
        minutes % 60 == 0 -> (minutes / 60).toString() + "小时"
        else -> (minutes / 60).toString() + ".5小时"
    }

    private fun intervalFromProgress(p: Int): Int = when (p) {
        0 -> 30; 1 -> 60; 2 -> 90; 3 -> 120; else -> 180
    }

    private fun progressFromInterval(m: Int): Int = when {
        m <= 30 -> 0; m <= 60 -> 1; m <= 90 -> 2; m <= 120 -> 3; else -> 4
    }

    companion object {
        private const val MATCH_PARENT = LinearLayout.LayoutParams.MATCH_PARENT
        private const val WRAP_CONTENT = LinearLayout.LayoutParams.WRAP_CONTENT
    }
}
