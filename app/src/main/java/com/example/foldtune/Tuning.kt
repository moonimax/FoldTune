package com.example.foldtune

import kotlin.math.pow
import kotlin.math.roundToInt

enum class Scale(val label: String, val steps: IntArray) {
    PENTATONIC("펜타토닉", intArrayOf(0, 2, 4, 7, 9)),
    MAJOR("메이저", intArrayOf(0, 2, 4, 5, 7, 9, 11)),
    MINOR("마이너", intArrayOf(0, 2, 3, 5, 7, 8, 10)),
    CHROMATIC("크로매틱", intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11))
}

object Tuning {

    /** 실제로 소리를 내는 각도 범위. 이보다 접으면 음소거. */
    const val MIN_ANGLE = 25f
    const val MAX_ANGLE = 180f

    private const val ROOT_MIDI = 48   // C3
    private const val OCTAVES = 3

    private val NOTE_NAMES = arrayOf(
        "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
    )

    /** 각도를 스케일 위의 MIDI 노트 번호로 스냅. */
    fun angleToMidi(angle: Float, scale: Scale): Int {
        val t = ((angle - MIN_ANGLE) / (MAX_ANGLE - MIN_ANGLE)).coerceIn(0f, 1f)
        val total = scale.steps.size * OCTAVES
        val idx = (t * (total - 1)).roundToInt().coerceIn(0, total - 1)
        return ROOT_MIDI + scale.steps[idx % scale.steps.size] + 12 * (idx / scale.steps.size)
    }

    fun midiToFreq(midi: Int): Double = 440.0 * 2.0.pow((midi - 69) / 12.0)

    fun noteName(midi: Int): String =
        NOTE_NAMES[((midi % 12) + 12) % 12] + (midi / 12 - 1)
}
