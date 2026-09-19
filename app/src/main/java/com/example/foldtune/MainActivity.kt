package com.example.foldtune

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.platform.LocalConfiguration
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class MainActivity : ComponentActivity() {

    private lateinit var sensorManager: SensorManager
    private var hingeSensor: Sensor? = null
    private val engine = ToneEngine()

    /** 센서 떨림을 없애기 위한 1차 저역 필터 상태 */
    private var smoothed = Float.NaN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        hingeSensor = findHingeSensor()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                var angle by remember { mutableFloatStateOf(180f) }
                var scale by remember { mutableStateOf(Scale.PENTATONIC) }
                var glide by remember { mutableFloatStateOf(0.003f) }

                // 각도/스케일이 바뀔 때마다 엔진에 반영
                val midi = Tuning.angleToMidi(angle, scale)
                engine.targetFreq = Tuning.midiToFreq(midi)
                engine.amplitude = if (angle > Tuning.MIN_ANGLE) 0.28 else 0.0
                engine.glide = glide.toDouble()

                val sensor = hingeSensor
                if (sensor != null) {
                    DisposableEffect(sensor) {
                        val listener = object : SensorEventListener {
                            override fun onSensorChanged(e: SensorEvent) {
                                val raw = e.values[0]
                                smoothed =
                                    if (smoothed.isNaN()) raw
                                    else smoothed + (raw - smoothed) * 0.25f
                                angle = smoothed
                            }

                            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
                        }
                        sensorManager.registerListener(
                            listener, sensor, SensorManager.SENSOR_DELAY_GAME
                        )
                        onDispose { sensorManager.unregisterListener(listener) }
                    }
                }

                Screen(
                    angle = angle,
                    noteName = Tuning.noteName(midi),
                    freq = Tuning.midiToFreq(midi),
                    scale = scale,
                    onScaleChange = { scale = it },
                    glide = glide,
                    onGlideChange = { glide = it },
                    hasSensor = sensor != null,
                    onManualAngle = { angle = it }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        engine.start()
    }

    override fun onStop() {
        engine.amplitude = 0.0
        engine.stop()
        super.onStop()
    }

    /**
     * 표준 TYPE_HINGE_ANGLE 이 없으면 제조사 커스텀 센서 이름으로 한 번 더 찾아봅니다.
     */
    private fun findHingeSensor(): Sensor? {
        sensorManager.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE)?.let { return it }
        return sensorManager.getSensorList(Sensor.TYPE_ALL).firstOrNull {
            it.name.contains("hinge", ignoreCase = true) ||
                it.stringType.contains("hinge", ignoreCase = true)
        }
    }
}

@Composable
private fun Screen(
    angle: Float,
    noteName: String,
    freq: Double,
    scale: Scale,
    onScaleChange: (Scale) -> Unit,
    glide: Float,
    onGlideChange: (Float) -> Unit,
    hasSensor: Boolean,
    onManualAngle: (Float) -> Unit
) {
    val muted = angle <= Tuning.MIN_ANGLE
    val isWide = LocalConfiguration.current.screenWidthDp > 600

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0F)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isWide) 64.dp else 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HingeGauge(angle = angle, muted = muted)

            Spacer(Modifier.height(24.dp))

            Text(
                text = if (muted) "—" else noteName,
                fontSize = 84.sp,
                fontWeight = FontWeight.Bold,
                color = if (muted) Color(0xFF3A3A46) else Color(0xFF7FE3C0)
            )
            Text(
                text = "${angle.roundToInt()}°   ·   ${"%.1f".format(freq)} Hz",
                fontSize = 16.sp,
                color = Color(0xFF8A8A9A)
            )

            Spacer(Modifier.height(32.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Scale.entries.forEach { s ->
                    FilterChip(
                        selected = s == scale,
                        onClick = { onScaleChange(s) },
                        label = { Text(s.label, fontSize = 13.sp) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text("글라이드  ${"%.4f".format(glide)}", color = Color(0xFF8A8A9A), fontSize = 13.sp)
            Slider(
                value = glide,
                onValueChange = onGlideChange,
                valueRange = 0.0005f..0.03f,
                modifier = Modifier.fillMaxWidth()
            )

            if (!hasSensor) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "힌지 센서 없음 — 슬라이더로 테스트",
                    color = Color(0xFFE0A35C),
                    fontSize = 13.sp
                )
                Slider(
                    value = angle,
                    onValueChange = onManualAngle,
                    valueRange = 0f..180f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** 현재 각도를 그대로 보여주는 간단한 힌지 그래픽 */
@Composable
private fun HingeGauge(angle: Float, muted: Boolean) {
    val color = if (muted) Color(0xFF3A3A46) else Color(0xFF7FE3C0)
    Canvas(
        Modifier
            .width(260.dp)
            .height(150.dp)
    ) {
        val pivot = Offset(size.width / 2f, size.height * 0.85f)
        val len = size.height * 0.72f
        val half = Math.toRadians((angle / 2f).toDouble())

        val dx = (sin(half) * len).toFloat()
        val dy = (cos(half) * len).toFloat()

        drawLine(
            color = color,
            start = pivot,
            end = Offset(pivot.x - dx, pivot.y - dy),
            strokeWidth = 14f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = pivot,
            end = Offset(pivot.x + dx, pivot.y - dy),
            strokeWidth = 14f,
            cap = StrokeCap.Round
        )
        drawCircle(color = color, radius = 10f, center = pivot)
    }
}
