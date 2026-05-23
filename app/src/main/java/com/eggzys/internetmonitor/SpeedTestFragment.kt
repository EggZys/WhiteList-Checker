package com.eggzys.internetmonitor

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class SpeedTestFragment : Fragment() {

    private lateinit var gaugeView: SpeedTestGaugeView
    private lateinit var gaugeOuterRing: View
    private lateinit var speedValue: TextView
    private lateinit var speedUnit: TextView
    private lateinit var testStatus: TextView
    private lateinit var downloadValue: TextView
    private lateinit var uploadValue: TextView
    private lateinit var pingValue: TextView
    private lateinit var btnStartTest: MaterialButton

    private val engine = SpeedTestEngine()
    private var isTesting = false

    private var ringAnimator: ObjectAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_speedtest, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupCallbacks()
        startRingAnimation()

        btnStartTest.setOnClickListener { startTest() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ringAnimator?.cancel()
    }

    private fun initViews(view: View) {
        gaugeView = view.findViewById(R.id.gaugeView)
        gaugeOuterRing = view.findViewById(R.id.gaugeOuterRing)
        speedValue = view.findViewById(R.id.speedValue)
        speedUnit = view.findViewById(R.id.speedUnit)
        testStatus = view.findViewById(R.id.testStatus)
        downloadValue = view.findViewById(R.id.downloadValue)
        uploadValue = view.findViewById(R.id.uploadValue)
        pingValue = view.findViewById(R.id.pingValue)
        btnStartTest = view.findViewById(R.id.btnStartTest)
    }

    private fun setupCallbacks() {
        engine.onProgress = { phase, speedMbps ->
            activity?.runOnUiThread {
                testStatus.text = phase
                if (speedMbps > 0) {
                    speedValue.text = String.format("%.2f", speedMbps)
                    gaugeView.setSpeed(speedMbps)
                }
            }
        }

        engine.onPingResult = { pingMs ->
            activity?.runOnUiThread {
                pingValue.text = if (pingMs >= 0) "$pingMs ms" else "N/A"
            }
        }

        engine.onDownloadResult = { speedMbps ->
            activity?.runOnUiThread {
                downloadValue.text = String.format("%.2f Mbps", speedMbps)
                speedValue.text = String.format("%.2f", speedMbps)
                gaugeView.setSpeed(speedMbps.toFloat())
            }
        }

        engine.onUploadResult = { speedMbps ->
            activity?.runOnUiThread {
                uploadValue.text = String.format("%.2f Mbps", speedMbps)
            }
        }
    }

    private fun startRingAnimation() {
        ringAnimator = ObjectAnimator.ofFloat(gaugeOuterRing, "rotation", 0f, 360f).apply {
            duration = 8000
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
    }

    private fun startTest() {
        if (isTesting) return
        isTesting = true

        btnStartTest.isEnabled = false
        btnStartTest.text = "TESTING..."
        btnStartTest.setTextColor(ContextCompat.getColor(requireContext(), R.color.neon_red))

        gaugeView.reset()
        speedValue.text = "0.00"
        speedUnit.text = "Mbps"
        downloadValue.text = "-- Mbps"
        uploadValue.text = "-- Mbps"
        pingValue.text = "-- ms"
        testStatus.text = "Initializing..."

        val ringDrawable = gaugeOuterRing.background as? android.graphics.drawable.GradientDrawable
        ringDrawable?.setStroke(3, ContextCompat.getColor(requireContext(), R.color.neon_orange))

        lifecycleScope.launch {
            try {
                val result = engine.runTest()

                speedValue.text = String.format("%.2f", result.downloadMbps)
                testStatus.text = "COMPLETE"
                ringDrawable?.setStroke(3, ContextCompat.getColor(requireContext(), R.color.neon_cyan))

            } catch (e: Exception) {
                testStatus.text = "ERROR: ${e.message}"
                ringDrawable?.setStroke(3, ContextCompat.getColor(requireContext(), R.color.neon_red))
            } finally {
                isTesting = false
                btnStartTest.isEnabled = true
                btnStartTest.text = "INITIATE TEST"
                btnStartTest.setTextColor(ContextCompat.getColor(requireContext(), R.color.neon_cyan))
            }
        }
    }
}