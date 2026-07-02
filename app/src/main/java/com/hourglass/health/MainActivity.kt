package com.hourglass.health

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.tabs.TabLayout
import com.hourglass.health.model.SleepData
import com.hourglass.health.sleep.SleepAnalyzer
import com.hourglass.health.water.WaterReminderService
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

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

        scrollContent.addView(contentInner)
        container.addView(scrollContent)
        setContentView(container)

        requestNotifPermission()
    }

    private fun buildHeader() {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(48), dp(20), dp(16))
        }
        header.addView(TextView(this).apply {
            text = "\u23f3 \u5065\u5eb7\u6c99\u6f0f"
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
            layoutParams = LinearLayout.LayoutParams(MP, WRAP_CONTENT)
            setSelectedTabIndicatorColor(0xFFD4A017.toInt())
            setTabTextColors(0xFF94A3B8.toInt(), 0xFFD4A017.toInt())
        }
        tabLayout.addTab(tabLayout.newTab().setText("\u7761\u7720\u5206\u6790"))
        tabLayout.addTab(tabLayout.newTab().setText("\u559d\u6c34\u63d0\u9192"))
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                sleepCard.visibility = if (tab?.position == 0) View.VISIBLE else View.GONE
                waterCard.visibility = if (tab?.position == 1) View.VISIBLE else View.GONE
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        container.addView(tabLayout)
    }

    private fun buildSleepSection(parent: LinearLayout) {
        parent.addView(sectionTitle("\u7761\u7720\u5206\u6790"))
        parent.addView(spacer(dp(8)))
        parent.addView(TextView(this).apply {
            text = "\u70b9\u51fb\u4e0b\u65b9\u6309\u94ae\u52a0\u8f7d\u793a\u4f8b\u6570\u636e"
            textSize = 13f
            setTextColor(0xFF94A3B8.toInt())
        })
        parent.addView(spacer(dp(20)))

        sleepScoreText = TextView(this).apply {
            gravity = android.view.Gravity.CENTER
            textSize = 48f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(MP, dp(80))
            text = "--"
        }
        parent.addView(sleepScoreText)
        parent.addView(spacer(dp(8)))

        sleepDetailText = TextView(this).apply {
            gravity = android.view.Gravity.CENTER
            textSize = 14f
            setTextColor(0xFF1E293B.toInt())
            text = "\u70b9\u51fb\u52a0\u8f7d\u7761\u7720\u6570\u636e"
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
        parent.addView(spacer(dp(16)))

        Button(this).apply {
            text = "\u52a0\u8f7d\u793a\u4f8b\u6570\u636e"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 14f
            setBackgroundColor(0xFFD4A017.toInt())
            layoutParams = LinearLayout.LayoutParams(MP, dp(44))
            setOnClickListener { loadDemo() }
        }.let { parent.addView(it) }
    }

    private fun buildWaterSection(parent: LinearLayout) {
        parent.addView(sectionTitle("\u559d\u6c34\u63d0\u9192"))
        parent.addView(spacer(dp(12)))

        val toggleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        toggleRow.addView(TextView(this).apply {
            text = "\u5f00\u542f\u559d\u6c34\u63d0\u9192"
            textSize = 16f
            setTextColor(0xFF1E293B.toInt())
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        })
        waterToggle = Switch(this).apply {
            isChecked = WaterReminderService.isServiceRunning(this@MainActivity)
            setOnCheckedChangeListener { _, isChecked -> toggleWater(isChecked) }
        }
        toggleRow.addView(waterToggle)
        parent.addView(toggleRow)
        parent.addView(spacer(dp(16)))

        parent.addView(TextView(this).apply {
            text = "\u63d0\u9192\u95f4\u9694"
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
                    val m = intervalVal(p)
                    waterIntervalLabel.text = intervalText(m)
                    WaterReminderService.setInterval(this@MainActivity, m)
                    if (waterToggle.isChecked) restartWater()
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        parent.addView(waterIntervalSeek)

        val initM = WaterReminderService.getInterval(this)
        waterIntervalSeek.progress = intervalProg(initM)
        waterIntervalLabel.text = intervalText(initM)
        parent.addView(spacer(dp(8)))
        parent.addView(TextView(this).apply {
            gravity = android.view.Gravity.CENTER
            text = "30m    1h    1.5h    2h    3h"
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
            text = "\u6bcf\u65e5\u996e\u6c34\u5efa\u8bae\uff1a\n- \u7537\u6027 2.5-3L / \u5973\u6027 2-2.5L\n- \u6bcf\u6b21\u559d 150-200ml\n- \u65e9\u8d77\u4e00\u676f\u6e29\u6c34\n- \u9910\u524d\u534a\u5c0f\u65f6\u4e00\u676f"
            textSize = 13f
            setTextColor(0xFF1E293B.toInt())
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = tipBg
        })
    }

    private fun loadDemo() {
        val cal = Calendar.getInstance()
        val bt = cal.apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 30) }.timeInMillis
        val wt = cal.apply { set(Calendar.HOUR_OF_DAY, 7); set(Calendar.MINUTE, 15) }.timeInMillis

        val demo = SleepData(
            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            bedTime = bt, wakeTime = wt,
            totalMinutes = 465, deepMinutes = 98,
            lightMinutes = 210, remMinutes = 112, awakeMinutes = 45,
            avgHeartRate = 58, avgSpo2 = 97, respiratoryRate = 16.5f
        )
        val analysis = SleepAnalyzer.analyze(demo)

        sleepScoreText.text = analysis.score.toString()
        sleepScoreText.setTextColor(scoreColor(analysis.score))

        val dpStr = "%.0f%%".format(analysis.deepPercent)
        val lpStr = "%.0f%%".format(analysis.lightPercent)
        val rpStr = "%.0f%%".format(analysis.remPercent)
        sleepDetailText.text = "%.1fh sleep\\nDeep $dpStr Light $lpStr REM $rpStr\\nRating: ${analysis.label}".format(analysis.totalHours)

        sleepAdviceText.text = "Bed: ${analysis.recommendedBedTime} -> ${analysis.recommendedWakeTime}\\n" +
            analysis.issues.joinToString("\\n") { "! $it" }

        sleepTipsList.removeAllViews()
        analysis.tips.forEach { tip ->
            sleepTipsList.addView(TextView(this).apply {
                text = tip
                textSize = 13f
                setTextColor(0xFF1E293B.toInt())
                setPadding(dp(8), dp(4), dp(8), dp(4))
            })
        }
    }

    private fun toggleWater(enabled: Boolean) {
        val intent = Intent(this, WaterReminderService::class.java)
        if (enabled) {
            intent.action = WaterReminderService.ACTION_START
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent)
            else startService(intent)
            waterStatusText.text = "Water reminder ON"
        } else {
            intent.action = WaterReminderService.ACTION_STOP
            stopService(intent)
            waterStatusText.text = "Water reminder OFF"
        }
    }

    private fun restartWater() {
        val i = Intent(this, WaterReminderService::class.java)
        stopService(i)
        i.action = WaterReminderService.ACTION_START
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i)
        else startService(i)
    }

    private fun requestNotifPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
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
        layoutParams = LinearLayout.LayoutParams(MP, h)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    private fun scoreColor(s: Int): Int = when { s >= 80 -> 0xFF16A34A.toInt(); s >= 60 -> 0xFF3B82F6.toInt(); s >= 40 -> 0xFFD97706.toInt(); else -> 0xFFDC2626.toInt() }
    private fun intervalText(m: Int): String = when { m < 60 -> m.toString()+"m"; m % 60 == 0 -> (m/60).toString()+"h"; else -> (m/60).toString()+".5h" }
    private fun intervalVal(p: Int): Int = when(p) { 0->30; 1->60; 2->90; 3->120; else->180 }
    private fun intervalProg(m: Int): Int = when { m<=30->0; m<=60->1; m<=90->2; m<=120->3; else->4 }

    companion object {
        private const val MP = LinearLayout.LayoutParams.MATCH_PARENT
        private const val WC = LinearLayout.LayoutParams.WRAP_CONTENT
        private const val WRAP_CONTENT = LinearLayout.LayoutParams.WRAP_CONTENT
    }
}
