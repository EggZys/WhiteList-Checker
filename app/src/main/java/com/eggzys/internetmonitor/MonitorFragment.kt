package com.eggzys.internetmonitor

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class MonitorFragment : Fragment() {

    private lateinit var prefs: SharedPreferences
    private lateinit var checker: InternetStateChecker

    // Views
    private lateinit var statusCard: MaterialCardView
    private lateinit var outerRing: View
    private lateinit var statusIndicatorBg: View
    private lateinit var statusEmoji: TextView
    private lateinit var statusName: TextView
    private lateinit var statusDescription: TextView
    private lateinit var lastCheckTime: TextView
    private lateinit var connectionStatusBar: LinearLayout
    private lateinit var statusDot: View
    private lateinit var connectionStatusText: TextView
    private lateinit var connectionTypeLabel: TextView
    private lateinit var btnToggle: MaterialButton
    private lateinit var btnCheckNow: MaterialButton
    private lateinit var settingsCard: MaterialCardView
    private lateinit var settingsHeader: LinearLayout
    private lateinit var settingsArrow: TextView
    private lateinit var settingsBody: LinearLayout
    private lateinit var etGlobalUrls: EditText
    private lateinit var etRussiaUrls: EditText
    private lateinit var etWhitelistUrls: EditText
    private lateinit var btnSaveUrls: MaterialButton
    private lateinit var header: LinearLayout
    private lateinit var buttonRow: LinearLayout

    private var isRunning = false
    private var settingsExpanded = false

    // Animators
    private var pulseAnimator: ObjectAnimator? = null
    private var ringAnimator: ObjectAnimator? = null
    private var dotAnimator: ObjectAnimator? = null
    private var connectionPulse: ValueAnimator? = null

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateUI()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_monitor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = requireContext().getSharedPreferences(MonitorService.PREFS_NAME, Context.MODE_PRIVATE)
        checker = InternetStateChecker()

        initViews(view)
        loadSettings()
        updateUI()

        btnToggle.setOnClickListener { toggleMonitoring() }
        btnCheckNow.setOnClickListener { checkNow() }
        settingsHeader.setOnClickListener { toggleSettings() }
        btnSaveUrls.setOnClickListener { saveSettings() }

        playEntranceAnimations()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
        startAnimations()
        val filter = IntentFilter(MonitorService.ACTION_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(stateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            requireContext().registerReceiver(stateReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        try { requireContext().unregisterReceiver(stateReceiver) } catch (_: Exception) {}
        stopAnimations()
    }

    private fun initViews(view: View) {
        header = view.findViewById(R.id.header)
        statusCard = view.findViewById(R.id.statusCard)
        outerRing = view.findViewById(R.id.outerRing)
        statusIndicatorBg = view.findViewById(R.id.statusIndicatorBg)
        statusEmoji = view.findViewById(R.id.statusEmoji)
        statusName = view.findViewById(R.id.statusName)
        statusDescription = view.findViewById(R.id.statusDescription)
        lastCheckTime = view.findViewById(R.id.lastCheckTime)
        connectionStatusBar = view.findViewById(R.id.connectionStatusBar)
        statusDot = view.findViewById(R.id.statusDot)
        connectionStatusText = view.findViewById(R.id.connectionStatusText)
        connectionTypeLabel = view.findViewById(R.id.connectionTypeLabel)
        btnToggle = view.findViewById(R.id.btnToggle)
        btnCheckNow = view.findViewById(R.id.btnCheckNow)
        settingsCard = view.findViewById(R.id.settingsCard)
        settingsHeader = view.findViewById(R.id.settingsHeader)
        settingsArrow = view.findViewById(R.id.settingsArrow)
        settingsBody = view.findViewById(R.id.settingsBody)
        etGlobalUrls = view.findViewById(R.id.etGlobalUrls)
        etRussiaUrls = view.findViewById(R.id.etRussiaUrls)
        etWhitelistUrls = view.findViewById(R.id.etWhitelistUrls)
        btnSaveUrls = view.findViewById(R.id.btnSaveUrls)
        buttonRow = view.findViewById(R.id.buttonRow)
    }

    // ===== ANIMATIONS =====

    private fun playEntranceAnimations() {
        val views = listOf(header, statusCard, connectionStatusBar, buttonRow, settingsCard)
        views.forEachIndexed { i, view ->
            view.alpha = 0f
            view.translationY = 60f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay((i * 100).toLong())
                .setDuration(500)
                .setInterpolator(android.view.animation.DecelerateInterpolator(1.5f))
                .start()
        }
    }

    private fun startAnimations() {
        pulseAnimator = ObjectAnimator.ofFloat(statusIndicatorBg, "alpha", 0.3f, 1f).apply {
            duration = 1500
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        ringAnimator = ObjectAnimator.ofFloat(outerRing, "rotation", 0f, 360f).apply {
            duration = 8000
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }

        dotAnimator = ObjectAnimator.ofFloat(statusDot, "alpha", 0.3f, 1f).apply {
            duration = 1200
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        if (isRunning) {
            startConnectionPulse()
        }
    }

    private fun startConnectionPulse() {
        connectionPulse?.cancel()
        connectionPulse = ValueAnimator.ofFloat(1f, 0.5f, 1f).apply {
            duration = 2500
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                connectionStatusText.alpha = it.animatedValue as Float
            }
            start()
        }
    }

    private fun stopAnimations() {
        pulseAnimator?.cancel()
        ringAnimator?.cancel()
        dotAnimator?.cancel()
        connectionPulse?.cancel()
    }

    // ===== UI UPDATE =====

    private fun updateUI() {
        isRunning = prefs.getBoolean(MonitorService.KEY_IS_RUNNING, false)

        val stateName = prefs.getString(MonitorService.KEY_LAST_STATE, InternetState.UNKNOWN.name)
        val state = try {
            InternetState.valueOf(stateName!!)
        } catch (_: Exception) {
            InternetState.UNKNOWN
        }

        val statusColor = ContextCompat.getColor(requireContext(), state.colorRes)

        statusEmoji.text = state.emoji
        statusName.text = state.displayName.uppercase()
        statusName.setTextColor(statusColor)
        statusDescription.text = state.notificationText

        val ringDrawable = statusIndicatorBg.background as? android.graphics.drawable.GradientDrawable
        ringDrawable?.setStroke(3, statusColor)
        val outerDrawable = outerRing.background as? android.graphics.drawable.GradientDrawable
        outerDrawable?.setStroke(3, statusColor)

        val checkTime = prefs.getLong(MonitorService.KEY_LAST_CHECK_TIME, 0)
        lastCheckTime.text = if (checkTime > 0) {
            timeFormat.format(Date(checkTime))
        } else {
            "--:--:--"
        }

        if (isRunning) {
            btnToggle.text = "DISENGAGE"
            btnToggle.setTextColor(ContextCompat.getColor(requireContext(), R.color.neon_red))
            connectionTypeLabel.text = "ONLINE"
            connectionTypeLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.neon_green))
            connectionStatusText.text = "MONITORING ACTIVE"
            statusDot.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.neon_green))
            startConnectionPulse()
        } else {
            btnToggle.text = "ENGAGE"
            btnToggle.setTextColor(ContextCompat.getColor(requireContext(), R.color.neon_cyan))
            connectionTypeLabel.text = "OFFLINE"
            connectionTypeLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.neon_cyan))
            connectionStatusText.text = "MONITORING OFFLINE"
            statusDot.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.neon_gray))
            connectionPulse?.cancel()
            connectionStatusText.alpha = 1f
        }
    }

    // ===== ACTIONS =====

    private fun toggleMonitoring() {
        if (isRunning) {
            MonitorService.stop(requireContext())
            prefs.edit().putBoolean(MonitorService.KEY_IS_RUNNING, false).apply()
            isRunning = false
            Toast.makeText(requireContext(), "Monitoring stopped", Toast.LENGTH_SHORT).show()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
                    Toast.makeText(requireContext(), "Please grant notification permission", Toast.LENGTH_LONG).show()
                    return
                }
            }
            MonitorService.start(requireContext())
            isRunning = true
            Toast.makeText(requireContext(), "Monitoring started - check notification bar", Toast.LENGTH_SHORT).show()
        }
        updateUI()
    }

    private fun checkNow() {
        btnCheckNow.isEnabled = false
        btnCheckNow.text = "SCANNING..."

        lifecycleScope.launch {
            try {
                val urlGroups = loadUrlGroups()
                val state = withContext(Dispatchers.IO) { checker.checkState(urlGroups) }

                val oldState = prefs.getString(MonitorService.KEY_LAST_STATE, InternetState.UNKNOWN.name)
                prefs.edit()
                    .putString(MonitorService.KEY_LAST_STATE, state.name)
                    .putLong(MonitorService.KEY_LAST_CHECK_TIME, System.currentTimeMillis())
                    .apply()

                val isRunningNow = prefs.getBoolean(MonitorService.KEY_IS_RUNNING, false)
                if (isRunningNow && oldState != state.name) {
                    NotificationHelper(requireContext()).sendStateAlert(state)
                }

                updateUI()
                Toast.makeText(requireContext(), "${state.emoji} ${state.displayName}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "SCAN ERROR: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                btnCheckNow.isEnabled = true
                btnCheckNow.text = "SCAN"
            }
        }
    }

    private fun toggleSettings() {
        settingsExpanded = !settingsExpanded
        settingsBody.visibility = if (settingsExpanded) View.VISIBLE else View.GONE
        settingsArrow.text = if (settingsExpanded) "-" else "+"
    }

    private fun loadSettings() {
        val defaults = UrlGroups.defaults()
        etGlobalUrls.setText(prefs.getString(MonitorService.KEY_GLOBAL_URLS, null) ?: defaults.globalUrls.joinToString("\n"))
        etRussiaUrls.setText(prefs.getString(MonitorService.KEY_RUSSIA_URLS, null) ?: defaults.russiaUrls.joinToString("\n"))
        etWhitelistUrls.setText(prefs.getString(MonitorService.KEY_WHITELIST_URLS, null) ?: defaults.whitelistUrls.joinToString("\n"))
    }

    private fun saveSettings() {
        prefs.edit()
            .putString(MonitorService.KEY_GLOBAL_URLS, etGlobalUrls.text.toString().trim())
            .putString(MonitorService.KEY_RUSSIA_URLS, etRussiaUrls.text.toString().trim())
            .putString(MonitorService.KEY_WHITELIST_URLS, etWhitelistUrls.text.toString().trim())
            .apply()
        Toast.makeText(requireContext(), "CONFIG SAVED", Toast.LENGTH_SHORT).show()
    }

    private fun loadUrlGroups(): UrlGroups {
        val defaults = UrlGroups.defaults()
        return UrlGroups(
            globalUrls = prefs.getString(MonitorService.KEY_GLOBAL_URLS, null)?.split("\n")?.filter { it.isNotBlank() } ?: defaults.globalUrls,
            russiaUrls = prefs.getString(MonitorService.KEY_RUSSIA_URLS, null)?.split("\n")?.filter { it.isNotBlank() } ?: defaults.russiaUrls,
            whitelistUrls = prefs.getString(MonitorService.KEY_WHITELIST_URLS, null)?.split("\n")?.filter { it.isNotBlank() } ?: defaults.whitelistUrls
        )
    }
}