package com.example.foldtune

import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

/**
 * 사인파(+배음)를 실시간 합성해서 스피커로 내보내는 엔진.
 *
 * targetFreq / amplitude 는 아무 스레드에서나 바꿔도 되고,
 * 오디오 스레드가 매 샘플마다 부드럽게 따라갑니다.
 */
class ToneEngine(
    private val sampleRate: Int = 48_000
) {
    /** 도달하고 싶은 주파수(Hz). 센서 콜백에서 갱신. */
    @Volatile var targetFreq: Double = 220.0

    /** 0.0 ~ 1.0. 0으로 두면 무음. */
    @Volatile var amplitude: Double = 0.0

    /**
     * 목표 음으로 미끄러지는 속도. 클수록 즉각 반응, 작을수록
     * 오토튠 특유의 포르타멘토가 강해집니다. (0.0005 ~ 0.02 권장)
     */
    @Volatile var glide: Double = 0.003

    /** 배음 섞는 양. 0이면 순수 사인파, 0.4쯤이면 오르간 느낌. */
    @Volatile var harmonics: Double = 0.25

    /** 화면에 표시할 용도로 현재 실제 울리고 있는 주파수를 노출. */
    @Volatile var currentFreq: Double = 220.0
        private set

    private var thread: Thread? = null
    @Volatile private var running = false

    fun start() {
        if (running) return
        running = true

        thread = Thread({
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_FLOAT
            ).coerceAtLeast(2048)

            val track = AudioTrack.Builder()
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            val buf = FloatArray(256)
            var phase = 0.0
            var f = targetFreq
            var a = 0.0

            track.play()
            while (running) {
                val tf = targetFreq
                val ta = amplitude
                val g = glide
                val h = harmonics

                for (i in buf.indices) {
                    // 주파수 글라이드
                    f += (tf - f) * g
                    // 진폭 램프 (급변할 때 '틱' 노이즈 방지)
                    a += (ta - a) * 0.0012

                    phase += 2.0 * PI * f / sampleRate
                    if (phase > 2.0 * PI) phase -= 2.0 * PI

                    val s = sin(phase) +
                            h * sin(2.0 * phase) +
                            h * 0.4 * sin(3.0 * phase)

                    buf[i] = (s / (1.0 + h * 1.4) * a).toFloat()
                }
                currentFreq = f
                track.write(buf, 0, buf.size, AudioTrack.WRITE_BLOCKING)
            }

            // 페이드 아웃 후 정리
            track.stop()
            track.release()
        }, "ToneEngine").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    fun stop() {
        running = false
        thread?.join(500)
        thread = null
    }
}
