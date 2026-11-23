package io.github.frailty9.blehr.data.settings

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal val OverlayThresholdRange = 1..250
internal const val OverlayWarningRedRgb = 0xFF0000

data class HeartRateColorRule(val minBpm: Int, val foregroundRgb: Int)

internal object HeartRateColorRules {
    fun validate(rules: List<HeartRateColorRule>) {
        require(rules.all { it.minBpm in OverlayThresholdRange && it.foregroundRgb in 0..0xFFFFFF })
        require(rules.distinctBy { it.minBpm }.size == rules.size)
    }

    fun encode(rules: List<HeartRateColorRule>): String = JSONArray().apply {
        for (rule in rules) {
            put(JSONObject().put("minBpm", rule.minBpm).put("foregroundRgb", rule.foregroundRgb))
        }
    }.toString()

    fun decode(value: String?): List<HeartRateColorRule> {
        if (value == null) return emptyList()
        return try {
            val array = JSONArray(value)
            List(array.length()) { index ->
                val rule = array.getJSONObject(index)
                HeartRateColorRule(
                    minBpm = rule.get("minBpm") as? Int ?: throw JSONException("Expected integer minBpm"),
                    foregroundRgb = rule.get("foregroundRgb") as? Int ?: throw JSONException("Expected integer foregroundRgb"),
                )
            }.also(::validate)
        } catch (_: JSONException) {
            emptyList()
        } catch (_: IllegalArgumentException) {
            emptyList()
        }
    }
}
