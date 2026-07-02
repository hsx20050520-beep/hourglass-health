package com.hourglass.health

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayout
import com.hourglass.health.health.HealthConnectManager
import com.hourglass.health.model.SleepData
import com.hourglass.health.sleep.SleepAnalyzer
import com.hourglass.health.water.WaterReminderService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var healthManager: HealthConnectManager
    private lateinit var container: LinearLayout

    // Sleep section
    private lateinit var sleepScoreText: TextView
    private lateinit var sleepDetailText: TextView
    private lateinit var sleepAdviceText: TextView
    private lateinit var sleepTipsList: LinearLayout
    private lateinit var sleepCard: LinearLayout
    private lateinit var connectStatusText: TextView
    private lateinit var healthConnectBtn: Button

    // Water section
    private lateinit var waterToggle: Switch
    private lateinit var waterIntervalSeek: SeekBar
    private lateinit var waterIntervalLabel: TextView
    private lateinit var waterStatusText: TextView
    private lateinit var waterCard: LinearLayout

    // Health data section
    private lateinit var hrValueText: TextView
    private lateinit var stepsValueText: TextView

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refreshData() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        val healthCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        buildHealthDataSection(healthCard)
        contentInner.addView(healthCard)

        scrollContent.addView(contentInner)
        container.addView(scrollContent)
        setContentView(container)

        healthManager = HealthConnectManager(this)

        requestNotificationPermission()
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

    private fun refreshData() {
        lifecycleScope.launch {
            val sleep = healthManager.readTodaySleep()
            if (sleep != null) updateSleepUI(sleep)

            val hr = healthManager.readLatestHeartRate()
            if (hr > 0) hrValueText.text = hr.toString()

            val steps = healthManager.readTodaySteps()
            stepsValueText.text = steps.toString()
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
        tabLayout.addTab(tabLayout.newTab().setText("\ud83d\udcc8 健康数据"))
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> { sleepCard.visibility = View.VISIBLE; waterCard.visibility = View.GONE }
                    1 -> { sleepCard.visibility = View.GONE; waterCard.visibility = View.VISIBLE }
                    2 -> { sleepCard.visibility = View.GONE; waterCard.visibility = View.GONE }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        container.addView(tabLayout)
    }

    private fun buildSleepSection(parent: LinearLayout) {
        connectStatusText = TextView(this).apply {
            textSize = 13f
            setTextColor(0xFF94A3B8.toInt())
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xFFFFF8DC.toInt())
                cornerRadius = dp(2).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                setMargins(dp(16), dp(8), dp(16), dp(16))
            }
        }
        updateConnectStatus()
        parent.addView(connectStatusText)

        healthConnectBtn = Button(this).apply {
            text = "\ud83d\udd0d 连接 Health Connect"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 14f
            setBackgroundColor(0xFF3B82F6.toInt())
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(44))
        }
        healthConnectBtn.setOnClickListener {
            permissionLauncher.launch(healthManager.getPermissionIntent())
        }
        parent.addView(healthConnectBtn)

        parent.addView(spacer(dp(16)))

        parent.addView(sectionTitle("\ud83d\udca4 昨晚睡眠分析"))
        parent.addView(spacer(dp(12)))

        sleepScoreText = TextView(this).apply {
            gravity = android.view.Gravity.CENTER
            textSize = 48f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(80))
            text = "--"
        }
        parent.addView(sleepScoreText)
        parent.addView(spacer(dp(8)))

        sleepDetailText = TextView(this).apply {
            gravity = android.view.Gravity.CENTER
            textSize = 14f
            setTextColor(0xFF1E293B.toInt())
            text = "\ud83d\udca1 打开 Health Connect 授权后自动读取"
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
        }
        parent.addView(sleepAdviceText)
        parent.addView(spacer(dp(12)))

        sleepTipsList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        parent.addView(sleepTipsList)

        parent.addView(spacer(dp(12)))

        // Refresh button
        Button(this).apply {
            text = "\ud83d\udd04 刷新数据"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 14f
            setBackgroundColor(0xFFD4A017.toInt())
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(44))
            setOnClickListener { refreshData() }
        }.let { parent.addView(it) }
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
            max = 4; progress = 1
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
            text = "30分钟    1小时    1.5小时    2小时    3小时"
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
            text = "\ud83d\udca1 每日饮水建议：\n\u2022 男性 2.5-3L / 女性 2-2.5L\n\u2022 每次喝 150-200ml\n\u2022 早起一杯温水\n\u2022 餐前半小时一杯"
            textSize = 13f
            setTextColor(0xFF1E293B.toInt())
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = tipBg
        })
    }

    private fun buildHealthDataSection(parent: LinearLayout) {
        parent.addView(sectionTitle("\ud83d\udcc8 今日健康概览"))
        parent.addView(spacer(dp(12)))

        val cardBg = android.graphics.drawable.GradientDrawable().apply {
            setColor(0xFFFFFEF5.toInt())
            cornerRadius = dp(8).toFloat()
            setStroke(dp(1), 0xFFE2E8F0.toInt())
        }

        // Heart Rate row
        val hrRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = cardBg
        }
        hrRow.addView(TextView(this).apply {
            text = "\ud83d\udc93 心率"
            textSize = 16f
            setTextColor(0xFF1E293B.toInt())
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        })
        hrValueText = TextView(this).apply {
            textSize = 24f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(0xFFEF4444.toInt())
            text = "-- bpm"
        }
        hrRow.addView(hrValueText)
        parent.addView(hrRow)
        parent.addView(spacer(dp(8)))

        // Steps row
        val stepRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = cardBg
        }
        stepRow.addView(TextView(this).apply {
            text = "\ud83d\udeb6 步数"
            textSize = 16f
            setTextColor(0xFF1E293B.toInt())
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        })
        stepsValueText = TextView(this).apply {
            textSize = 24f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(0xFF16A34A.toInt())
            text = "--"
        }
        stepRow.addView(stepsValueText)
        parent.addView(stepRow)

        // Info text
        parent.addView(spacer(dp(16)))
        parent.addView(TextView(this).apply {
            text = "\ud83d\udca1 数据来自 Health Connect，需先在 Mi Fitness 中开启 Health Connect 同步"
            textSize = 12f
            setTextColor(0xFF94A3B8.toInt())
            gravity = android.view.Gravity.CENTER
        })
    }

    private fun updateConnectStatus() {
        connectStatusText.text = "\u24c2 数据源: Health Connect\n\u2460 确保 Mi Fitness 已同步到手环"
    }

    private fun updateSleepUI(sleep: SleepData) {
        val analysis = SleepAnalyzer.analyze(sleep)

        sleepScoreText.text = analysis.score.toString()
        sleepScoreText.setTextColor(scoreColor(analysis.score))

        val deepPct = "%.0f".format(analysis.deepPercent)
        val lightPct = "%.0f".format(analysis.lightPercent)
        val remPct = "%.0f".format(analysis.remPercent)
        sleepDetailText.text = "总睡眠 %.1f 小时\n深睡 %s%% - 浅睡 %s%% - REM %s%%\n质量: %s".format(
            analysis.totalHours, deepPct, lightPct, remPct, analysis.label
        )

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

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
            }
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
        minutes < 60 -> "$minutes分钟"
        minutes % 60 == 0 -> "${minutes / 60}小时"
        else -> "${minutes / 60}.5小时"
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
