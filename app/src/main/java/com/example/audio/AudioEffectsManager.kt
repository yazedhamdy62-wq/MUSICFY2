package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** One equalizer profile: 5 band gains (dB, -10..10) plus the three extra effects (0..100). */
data class EqProfile(
    val name: String,
    val bands: List<Int>,
    val bass: Int = 0,
    val spatial: Int = 0,
    val loudness: Int = 0,
    val builtIn: Boolean = true
)

/**
 * Real audio effects on top of the MediaPlayer's audio session (system Equalizer, BassBoost,
 * Virtualizer, LoudnessEnhancer). The 5 UI bands are mapped onto whatever bands the device offers.
 * Every effect is created defensively: a device that lacks one just skips it.
 */
class AudioEffectsManager(context: Context) {
    private val prefs = context.getSharedPreferences("musicfy_eq", Context.MODE_PRIVATE)

    private val _enabled = MutableStateFlow(prefs.getBoolean("enabled", true))
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _bands = MutableStateFlow(loadBands())
    val bands: StateFlow<List<Int>> = _bands.asStateFlow()

    private val _bass = MutableStateFlow(prefs.getInt("bass", 0))
    val bass: StateFlow<Int> = _bass.asStateFlow()

    private val _spatial = MutableStateFlow(prefs.getInt("spatial", 0))
    val spatial: StateFlow<Int> = _spatial.asStateFlow()

    private val _loudness = MutableStateFlow(prefs.getInt("loudness", 0))
    val loudness: StateFlow<Int> = _loudness.asStateFlow()

    private val _activeProfile = MutableStateFlow(prefs.getString("profile", "Flat") ?: "Flat")
    val activeProfile: StateFlow<String> = _activeProfile.asStateFlow()

    private val _custom = MutableStateFlow(loadCustom())
    val customProfiles: StateFlow<List<EqProfile>> = _custom.asStateFlow()

    private var eq: Equalizer? = null
    private var boost: BassBoost? = null
    private var virt: Virtualizer? = null
    private var loud: LoudnessEnhancer? = null

    val allProfiles: List<EqProfile> get() = BUILT_IN + _custom.value

    // ---- attach / release -------------------------------------------------------------------

    fun attach(player: MediaPlayer) {
        release()
        val session = try { player.audioSessionId } catch (_: Exception) { return }
        if (session == 0) return
        eq = try { Equalizer(0, session) } catch (_: Throwable) { null }
        boost = try { BassBoost(0, session) } catch (_: Throwable) { null }
        virt = try { Virtualizer(0, session) } catch (_: Throwable) { null }
        loud = try { LoudnessEnhancer(session) } catch (_: Throwable) { null }
        applyAll()
    }

    fun release() {
        listOf<android.media.audiofx.AudioEffect?>(eq, boost, virt, loud).forEach {
            try { it?.release() } catch (_: Throwable) { }
        }
        eq = null; boost = null; virt = null; loud = null
    }

    // ---- user actions -----------------------------------------------------------------------

    fun setEnabled(value: Boolean) {
        _enabled.value = value
        prefs.edit().putBoolean("enabled", value).apply()
        applyAll()
    }

    fun setBand(index: Int, db: Int) {
        val list = _bands.value.toMutableList()
        if (index !in list.indices) return
        list[index] = db.coerceIn(-10, 10)
        _bands.value = list
        markCustom()
        applyEq()
    }

    fun setBass(value: Int) { _bass.value = value.coerceIn(0, 100); markCustom(); applyBass() }
    fun setSpatial(value: Int) { _spatial.value = value.coerceIn(0, 100); markCustom(); applySpatial() }
    fun setLoudness(value: Int) { _loudness.value = value.coerceIn(0, 100); markCustom(); applyLoudness() }

    fun selectProfile(name: String) {
        val p = allProfiles.firstOrNull { it.name == name } ?: return
        _bands.value = p.bands
        _bass.value = p.bass
        _spatial.value = p.spatial
        _loudness.value = p.loudness
        _activeProfile.value = p.name
        persist()
        applyAll()
    }

    /** Saves the current sliders as a user profile (replaces one with the same name). */
    fun saveCurrentAs(name: String) {
        val clean = name.trim().take(24)
        if (clean.isEmpty() || BUILT_IN.any { it.name.equals(clean, true) }) return
        val profile = EqProfile(clean, _bands.value, _bass.value, _spatial.value, _loudness.value, builtIn = false)
        _custom.value = _custom.value.filter { !it.name.equals(clean, true) } + profile
        _activeProfile.value = clean
        persist()
    }

    fun deleteProfile(name: String) {
        val removed = _custom.value.firstOrNull { it.name == name } ?: return
        _custom.value = _custom.value - removed
        if (_activeProfile.value == name) selectProfile("Flat") else persist()
    }

    // ---- applying ---------------------------------------------------------------------------

    private fun markCustom() {
        _activeProfile.value = "Custom"
        persist()
    }

    private fun applyAll() { applyEq(); applyBass(); applySpatial(); applyLoudness() }

    private fun applyEq() {
        val e = eq ?: return
        try {
            e.enabled = _enabled.value
            val range = e.bandLevelRange // millibels
            val bandCount = e.numberOfBands.toInt()
            val ui = _bands.value
            for (b in 0 until bandCount) {
                val centerHz = e.getCenterFreq(b.toShort()) / 1000f
                val gainDb = interpolate(centerHz, ui)
                val mb = (gainDb * 100f).toInt().coerceIn(range[0].toInt(), range[1].toInt())
                e.setBandLevel(b.toShort(), mb.toShort())
            }
        } catch (_: Throwable) { }
    }

    private fun applyBass() {
        val b = boost ?: return
        try {
            b.enabled = _enabled.value && _bass.value > 0
            if (b.strengthSupported) b.setStrength((_bass.value * 10).toShort())
        } catch (_: Throwable) { }
    }

    private fun applySpatial() {
        val v = virt ?: return
        try {
            v.enabled = _enabled.value && _spatial.value > 0
            if (v.strengthSupported) v.setStrength((_spatial.value * 10).toShort())
        } catch (_: Throwable) { }
    }

    private fun applyLoudness() {
        val l = loud ?: return
        try {
            l.enabled = _enabled.value && _loudness.value > 0
            l.setTargetGain(_loudness.value * 15) // 0..1500 mB (up to +15 dB)
        } catch (_: Throwable) { }
    }

    /** Linear interpolation of the 5 UI bands (on a log-frequency axis) at a device band centre. */
    private fun interpolate(hz: Float, ui: List<Int>): Float {
        if (ui.size != UI_CENTERS.size) return 0f
        if (hz <= UI_CENTERS.first()) return ui.first().toFloat()
        if (hz >= UI_CENTERS.last()) return ui.last().toFloat()
        for (i in 0 until UI_CENTERS.size - 1) {
            val lo = UI_CENTERS[i]; val hi = UI_CENTERS[i + 1]
            if (hz in lo..hi) {
                val t = (Math.log10(hz.toDouble()) - Math.log10(lo.toDouble())) /
                    (Math.log10(hi.toDouble()) - Math.log10(lo.toDouble()))
                return (ui[i] + (ui[i + 1] - ui[i]) * t).toFloat()
            }
        }
        return 0f
    }

    // ---- persistence ------------------------------------------------------------------------

    private fun persist() {
        val arr = JSONArray()
        _custom.value.forEach {
            arr.put(JSONObject().put("name", it.name).put("bands", JSONArray(it.bands))
                .put("bass", it.bass).put("spatial", it.spatial).put("loudness", it.loudness))
        }
        prefs.edit()
            .putString("bands", _bands.value.joinToString(","))
            .putInt("bass", _bass.value).putInt("spatial", _spatial.value).putInt("loudness", _loudness.value)
            .putString("profile", _activeProfile.value)
            .putString("custom", arr.toString())
            .apply()
    }

    private fun loadBands(): List<Int> {
        val parsed = prefs.getString("bands", null)?.split(",")?.mapNotNull { it.toIntOrNull() }
        return if (parsed != null && parsed.size == 5) parsed else listOf(0, 0, 0, 0, 0)
    }

    private fun loadCustom(): List<EqProfile> = try {
        val arr = JSONArray(prefs.getString("custom", "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val b = o.getJSONArray("bands")
            EqProfile(o.getString("name"), (0 until b.length()).map { b.getInt(it) },
                o.optInt("bass"), o.optInt("spatial"), o.optInt("loudness"), builtIn = false)
        }
    } catch (_: Exception) { emptyList() }

    companion object {
        val UI_CENTERS = listOf(60f, 230f, 910f, 3600f, 14000f)

        val BUILT_IN = listOf(
            EqProfile("Flat", listOf(0, 0, 0, 0, 0)),
            EqProfile("Bass Boost", listOf(8, 6, 2, 0, -2), bass = 60),
            EqProfile("Vocal & Bass", listOf(4, 5, 2, 4, 6), bass = 30),
            EqProfile("Rock", listOf(5, 3, -1, 3, 6)),
            EqProfile("Electronic", listOf(7, 5, 0, 4, 7), spatial = 40),
            EqProfile("Pop", listOf(-1, 3, 5, 3, -1)),
            EqProfile("Hip-Hop", listOf(6, 4, 0, 2, 3), bass = 45),
            EqProfile("Jazz", listOf(3, 2, -2, 2, 4)),
            EqProfile("Classical", listOf(4, 2, -1, 2, 4), spatial = 25),
            EqProfile("Acoustic", listOf(4, 3, 2, 3, 3)),
            EqProfile("Vocal Boost", listOf(-2, 1, 5, 4, 1)),
            EqProfile("Treble Boost", listOf(-2, -1, 0, 5, 8)),
            EqProfile("Late Night", listOf(-3, -1, 2, 1, -2), loudness = 35)
        )
    }
}
