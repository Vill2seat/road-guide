package com.roadguide.app.data.source

import android.content.Context
import android.net.ConnectivityManager
import okhttp3.OkHttpClient
import okhttp3.Request
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

/** Open-Meteo weather client (current weather + hourly forecast). */
class OpenMeteoWeatherSource(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val timeoutMs: Int = 10_000,
    private val maxRetries: Int = 3,
    private val retryDelayMs: Int = 2_000
) {

    private val dateFormat: ThreadLocal<SimpleDateFormat> = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    private val TAG = "OpenMeteoWeatherSource"

    private val networkChecker = object : ConnectivityManager.Provider {
        private val cm: android.content.ConnectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.content.ConnectivityManager
        override fun activeNetworkInfo(): android.net.NetworkInfo? = cm.activeNetworkInfo
    }

    /** True if network is available. */
    private fun isNetworkAvailable(): Boolean = activeNetworkInfo?.isConnected == true

    /** Suspends for the given number of milliseconds. */
    private suspend fun delayMs(ms: Long) {
        kotlinx.coroutines.delay(ms)
    }

    /** Fetches current weather and hourly forecast. */
    suspend fun fetch(lat: Double, lon: Double): OpenMeteoWeather? {
        if (!isNetworkAvailable()) {
            Log.w(TAG, "network unavailable")
            return null
        }

        var lastException: Exception? = null
        for (attempt in 1..maxRetries) {
            try {
                return withTimeout(timeoutMs.toLong()) {
                    val url = buildUrl(lat, lon)
                    val response = httpGet(url)
                    parseOpenMeteo(response, lat, lon)
                }
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries) {
                    delayMs(retryDelayMs.toLong())
                }
            }
        }
        Log.e(TAG, "failed after $maxRetries attempts for lat=$lat, lon=$lon", lastException)
        return null
    }

    /** Builds the weather API URL. */
    private fun buildUrl(lat: Double, lon: Double): String {
        val now = System.currentTimeMillis()
        val today = dateFormat.get().format(now)
        return "https://api.open-meteo.com/v1/forecast?" +
            "latitude=$lat&longitude=$lon&" +
            "current=temperature_2m,apparent_temperature,relative_humidity_2m," +
            "wind_speed_10m,wind_direction_10m,precipitation,rain,cloud_cover,uv_index&" +
            "hourly=temperature_2m,relative_humidity_2m,wind_speed_10m,wind_direction_10m,precipitation,rain,cloud_cover,uv_index,pm2_5,pm10,nitrogen_dioxide,ozone,sulphur_dioxide,carbon_monoxide,european_aqi,us_aqi&" +
            "wind_speed_unit=ms&" +
            "forecast_days=2&" +
            "timezone=auto&start_date=$today&end_date=$today"
    }

    /** Performs an HTTP GET request. */
    private fun httpGet(urlString: String): String {
        val request = Request.Builder().url(urlString).build()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            Log.e(TAG, "HTTP ${response.code}: ${response.message}")
            throw java.io.IOException("Unexpected code ${response.code}")
        }
        return response.body!!.string()
    }

    /** Parses the weather JSON response using org.json. */
    private fun parseOpenMeteo(json: String, lat: Double, lon: Double): OpenMeteoWeather? {
        val root = JSONObject(json)

        // Parse current weather
        val current = root.optJSONObject("current")
        val currentUnits = root.optJSONObject("current_units")

        val timestamp = extractDateTime(root, "current")

        val openMeteoWeather = OpenMeteoWeather(
            temperatureCelsius = optDouble(root, "temperature_2m"),
            apparentTemperatureCelsius = optDouble(root, "apparent_temperature"),
            relativeHumidityPercent = optDouble(root, "relative_humidity_2m"),
            windSpeedMs = optDouble(root, "wind_speed_10m"),
            windDirectionDegrees = optDouble(root, "wind_direction_10m"),
            precipitationMm = optDouble(root, "precipitation"),
            rainMm = optDouble(root, "rain"),
            cloudCoverPercent = optDouble(root, "cloud_cover"),
            uvIndex = optDouble(root, "uv_index"),
            pm2_5 = optDouble(root, "pm2_5"),
            pm10 = optDouble(root, "pm10"),
            nitrogenDioxide = optDouble(root, "nitrogen_dioxide"),
            ozone = optDouble(root, "ozone"),
            sulphurDioxide = optDouble(root, "sulphur_dioxide"),
            carbonMonoxide = optDouble(root, "carbon_monoxide"),
            europeanAqi = optInt(root, "european_aqi"),
            timestamp = timestamp
        )

        // Parse hourly forecast
        val hourlyForecast: OpenMeteoHourlyForecast? = try {
            val hourly = root.optJSONObject("hourly")
            if (hourly == null) null
            else {
                val timeArray = hourly.getJSONArray("time")
                val tempArray = hourly.getJSONArray("temperature_2m")
                val humidityArray = hourly.getJSONArray("relative_humidity_2m")
                val windSpeedArray = hourly.getJSONArray("wind_speed_10m")
                val windDirArray = hourly.getJSONArray("wind_direction_10m")
                val precipArray = hourly.getJSONArray("precipitation")
                val rainArray = hourly.getJSONArray("rain")
                val cloudCoverArray = hourly.getJSONArray("cloud_cover")
                val uvIndexArray = hourly.getJSONArray("uv_index")
                val pm25Array = hourly.getJSONArray("pm2_5")
                val pm10Array = hourly.getJSONArray("pm10")
                val no2Array = hourly.getJSONArray("nitrogen_dioxide")
                val o3Array = hourly.getJSONArray("ozone")
                val so2Array = hourly.getJSONArray("sulphur_dioxide")
                val coArray = hourly.getJSONArray("carbon_monoxide")
                val europeanAqiArray = hourly.getJSONArray("european_aqi")
                val usAqiArray = hourly.getJSONArray("us_aqi")

                val timeList = jsonArrayToStringList(timeArray)
                val tempList = stringArrayToDoubleList(tempArray)
                val humidityList = stringArrayToDoubleList(humidityArray)
                val windSpeedList = stringArrayToDoubleList(windSpeedArray)
                val windDirList = stringArrayToDoubleList(windDirArray)
                val precipList = stringArrayToDoubleList(precipArray)
                val rainList = stringArrayToDoubleList(rainArray)
                val cloudList = stringArrayToDoubleList(cloudCoverArray)
                val uvList = stringArrayToDoubleList(uvIndexArray)
                val pm25List = stringArrayToDoubleList(pm25Array)
                val pm10List = stringArrayToDoubleList(pm10Array)
                val no2List = stringArrayToDoubleList(no2Array)
                val o3List = stringArrayToDoubleList(o3Array)
                val so2List = stringArrayToDoubleList(so2Array)
                val coList = stringArrayToDoubleList(coArray)
                val europeanAqiList = intArrayListFromJSONArray(europeanAqiArray)
                val usAqiList = intArrayListFromJSONArray(usAqiArray)

                OpenMeteoHourlyForecast(
                    time = timeList,
                    temperatureCelsius = tempList,
                    relativeHumidityPercent = humidityList,
                    windSpeedMs = windSpeedList,
                    windDirectionDegrees = windDirList,
                    precipitationMm = precipList,
                    rainMm = rainList,
                    cloudCoverPercent = cloudList,
                    uvIndex = uvList,
                    pm2_5 = pm25List,
                    pm10 = pm10List,
                    nitrogenDioxide = no2List,
                    ozone = o3List,
                    sulphurDioxide = so2List,
                    carbonMonoxide = coList,
                    europeanAqi = europeanAqiList,
                    usAqi = usAqiList
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "hourly parse failed", e)
            null
        }

        return OpenMeteoWeather(
            temperatureCelsius = optDouble(root, "temperature_2m"),
            apparentTemperatureCelsius = optDouble(root, "apparent_temperature"),
            relativeHumidityPercent = optDouble(root, "relative_humidity_2m"),
            windSpeedMs = optDouble(root, "wind_speed_10m"),
            windDirectionDegrees = optDouble(root, "wind_direction_10m"),
            precipitationMm = optDouble(root, "precipitation"),
            rainMm = optDouble(root, "rain"),
            cloudCoverPercent = optDouble(root, "cloud_cover"),
            uvIndex = optDouble(root, "uv_index"),
            pm2_5 = optDouble(root, "pm2_5"),
            pm10 = optDouble(root, "pm10"),
            nitrogenDioxide = optDouble(root, "nitrogen_dioxide"),
            ozone = optDouble(root, "ozone"),
            sulphurDioxide = optDouble(root, "sulphur_dioxide"),
            carbonMonoxide = optDouble(root, "carbon_monoxide"),
            europeanAqi = optInt(root, "european_aqi"),
            timestamp = timestamp,
            hourly = hourlyForecast
        )
    }

    /** Extracts date/time from the root JSON. */
    private fun extractDateTime(root: JSONObject, block: String): Long {
        val obj = root.optJSONObject(block)
        if (obj == null) return System.currentTimeMillis()
        val timeStr = obj.optString("time", null)
        if (timeStr == null) return System.currentTimeMillis()
        try {
            return java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss',000'Z'", java.text.DateFormat.ISO8601)
                .parse(timeStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            return System.currentTimeMillis()
        }
    }

    /** Converts a JSONArray to a List<String>. */
    private fun jsonArrayToStringList(array: JSONArray): List<String> {
        val list = mutableListOf<String>()
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
        return list
    }

    /** Converts a JSONArray to a List<Double>. */
    private fun stringArrayToDoubleList(array: JSONArray): List<Double> {
        val list = mutableListOf<Double>()
        for (i in 0 until array.length()) {
            val opt = array.getJSONObject(i).optDouble("value")
            list.add(opt)
        }
        return list
    }

    /** Converts a JSONArray to a List<Int>. */
    private fun intArrayListFromJSONArray(array: JSONArray): List<Int> {
        val list = mutableListOf<Int>()
        for (i in 0 until array.length()) {
            list.add(array.getInt(i))
        }
        return list
    }

    /** Optimal double extraction - returns null if missing. */
    private fun optDouble(root: JSONObject?, key: String): Double? {
        try {
            return root?.optDouble(key)
        } catch (e: Exception) {
            return null
        }
    }

    /** Optimal int extraction - returns null if missing. */
    private fun optInt(root: JSONObject?, key: String): Int? {
        try {
            return root?.optInt(key)
        } catch (e: Exception) {
            return null
        }
    }

    /** Suspends for the given number of milliseconds. */
    private suspend fun delayMs(ms: Long) {
        kotlinx.coroutines.delay(ms)
    }

    /** Extracts a string value from a JSONObject block. */
    private fun extractString(block: JSONObject, key: String): String? {
        try {
            return block.optString(key)
        } catch (e: Exception) {
            return null
        }
    }

    /** Extracts a double value from a JSONObject block. */
    private fun extractDouble(block: JSONObject, key: String): Double? {
        try {
            return block.optDouble(key)
        } catch (e: Exception) {
            return null
        }
    }

    /** Extracts an int value from a JSONObject block. */
    private fun extractInt(block: JSONObject, key: String): Int? {
        try {
            return block.optInt(key)
        } catch (e: Exception) {
            return null
        }
    }

    /** Extracts a JSONArray from a JSONObject block. */
    private fun extractJSONObject(block: JSONObject, key: String): JSONObject? {
        try {
            return block.optJSONObject(key)
        } catch (e: Exception) {
            return null
        }
    }

    /** API Quality fetch. */
    suspend fun fetchAirQuality(lat: Double, lon: Double): CamsAqis? {
        if (!isNetworkAvailable()) {
            Log.w("OpenMeteoAirQuality", "network unavailable")
            return null
        }

        var lastException: Exception? = null
        for (attempt in 1..maxRetries) {
            try {
                return withTimeout(timeoutMs.toLong()) {
                    val url = buildAirQualityUrl(lat, lon)
                    val response = httpGet(url)
                    parseAirQuality(response)
                }
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries) {
                    delayMs(retryDelayMs.toLong())
                }
            }
        }

        Log.e(TAG, "failed after $maxRetries attempts for lat=$lat, lon=$lon", lastException)
        return null
    }

    /** Builds the Air Quality API URL. */
    private fun buildAirQualityUrl(lat: Double, lon: Double): String {
        return "https://air-quality-api.open-meteo.com/v1/air-quality?" +
            "latitude=$lat&longitude=$lon&" +
            "hourly=pm10,pm2_5,nitrogen_dioxide,ozone,sulphur_dioxide,carbon_monoxide,us_aqi,european_aqi&" +
            "timezone=auto"
    }

    /** Performs an HTTP GET request. */
    private fun httpGet(urlString: String): String {
        val request = Request.Builder().url(urlString).build()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            Log.e(TAG, "HTTP ${response.code}: ${response.message}")
            throw java.io.IOException("Unexpected code ${response.code}")
        }
        return response.body!!.string()
    }

    /** Parses the Air Quality JSON response. */
    private fun parseAirQuality(json: String): CamsAqis? {
        val hourly = root.optJSONObject("hourly") ?: return null
        val time = hourly.optString("time")
        val timestamps = hourly.getJSONArray("time").toStringList()
        val pm25 = hourly.getJSONArray("pm2_5").toDoubleList()
        val pm10 = hourly.getJSONArray("pm10").toDoubleList()
        val no2 = hourly.getJSONArray("nitrogen_dioxide").toDoubleList()
        val o3 = hourly.getJSONArray("ozone").toDoubleList()
        val so2 = hourly.getJSONArray("sulphur_dioxide").toDoubleList()
        val co = hourly.getJSONArray("carbon_monoxide").toDoubleList()
        val usAqi = hourly.getJSONArray("us_aqi").toIntList()
        val europeanAqi = hourly.getJSONArray("european_aqi").toIntList()

        val lastIdx = timestamps.lastIndex
        return CamsAqis(
            usAqi = usAqi.lastOrNull(),
            pm2_5 = pm25.lastOrNull(),
            pm10 = pm10.lastOrNull(),
            nitrogenDioxide = no2.lastOrNull(),
            ozone = o3.lastOrNull(),
            sulphurDioxide = so2.lastOrNull(),
            carbonMonoxide = co.lastOrNull(),
            timestamp = timestamps[lastIdx].toLong()
        )
    }

    /** Extends a string with default if null. */
    private fun String?: defaultValue(d: String): String = this ?: d

    /** String list from JSONArray. */
    private fun String?: toStringList(): List<String> {
        if (this == null) return emptyList()
        val builder = StringBuilder(this?.trim() ?: "")
        // Simple split by comma, handling quoted strings
        val result = mutableListOf<String>()
        var i = 0
        while (i < length) {
            val c = this[i]
            if (c == '\"') {
                val end = indexOf('\"', i + 1)
                if (end > 0) {
                    result.add(this.substring(i + 1, end))
                    i = end + 1
                } else {
                    i++
                }
            } else if (c == ',') {
                i++
            } else {
                result.add(this[i].toString())
                i++
            }
        }
        if (i < length) result.add(this.substring(i))
        return result
    }

    /** String list from JSONArray. */
    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until length()) {
            val opt = getString(i)
            if (opt != null) list.add(opt)
        }
        return list
    }

    /** Double list from JSONArray. */
    private fun JSONArray?.toDoubleList(): List<Double> {
        val list = mutableListOf<Double>()
        for (i in 0 until length()) {
            val opt = getJSONObject(i).optDouble("value")
            if (opt != null) list.add(opt)
        }
        return list
    }

    /** Int list from JSONArray. */
    private fun JSONArray?.toIntList(): List<Int> {
        val list = mutableListOf<Int>()
        for (i in 0 until length()) {
            list.add(getInt(i))
        }
        return list
    }

    /** String list from JSONArray. */
    private fun JSONObject?.getString(default: String = ""): String {
        try {
            if (isNull("")) return default
            return super.getString(default)
        } catch (e: Exception) {
            return default
        }
    }

    /** Double from JSONObject. */
    private fun JSONObject?.optDouble(default: Double = 0.0): Double {
        try {
            return optDouble(this, "")
        } catch (e: Exception) {
            return default
        }
    }

    /** Int from JSONObject. */
    private fun JSONObject?.getInt(default: Int = 0): Int {
        try {
            return getInt(default)
        } catch (e: Exception) {
            return default
        }
    }

    /** Suspends for the given number of milliseconds. */
    private suspend fun delayMs(ms: Long) {
        kotlinx.coroutines.delay(ms)
    }
}

/** Raw weather response from Open-Meteo API. */
data class OpenMeteoWeather(
    val temperatureCelsius: Double? = null,
    val apparentTemperatureCelsius: Double? = null,
    val relativeHumidityPercent: Double? = null,
    val windSpeedMs: Double? = null,
    val windDirectionDegrees: Double? = null,
    val precipitationMm: Double? = null,
    val rainMm: Double? = null,
    val cloudCoverPercent: Double? = null,
    val uvIndex: Double? = null,
    val pm2_5: Double? = null,
    val pm10: Double? = null,
    val nitrogenDioxide: Double? = null,
    val ozone: Double? = null,
    val sulphurDioxide: Double? = null,
    val carbonMonoxide: Double? = null,
    val europeanAqi: Int? = null,
    val timestamp: Long = System.currentTimeMillis(),
    /** Hourly forecast data. */
    val hourly: OpenMeteoHourlyForecast? = null
) {

    /** Converts to EnvironmentalSample. */
    fun toEnvironmentalSample(lat: Double, lon: Double, fetchedAt: Long): EnvironmentalSample {
        return EnvironmentalSample(
            latitude = lat, longitude = lon, timestamp = fetchedAt,
            pm2_5 = pm2_5, pm10 = pm10,
            nitrogenDioxide = nitrogenDioxide, ozone = ozone,
            sulphurDioxide = sulphurDioxide, carbonMonoxide = carbonMonoxide,
            usAqi = europeanAqi, europeanAqi = europeanAqi,
            temperatureCelsius = temperatureCelsius,
            apparentTemperatureCelsius = apparentTemperatureCelsius,
            relativeHumidityPercent = relativeHumidityPercent,
            windSpeedMs = windSpeedMs,
            windDirectionDegrees = windDirectionDegrees,
            precipitationMm = precipitationMm,
            rainMm = rainMm,
            cloudCoverPercent = cloudCoverPercent,
            uvIndex = uvIndex,
            source = DataSource.OPEN_METEO,
            dataQuality = DataQuality.HIGH,
            modelResolutionKm = null,
            fetchedAt = fetchedAt
        )
    }
}

/** Hourly forecast data from Open-Meteo. */
data class OpenMeteoHourlyForecast(
    val time: List<String>,
    val temperatureCelsius: List<Double?>,
    val relativeHumidityPercent: List<Double?>,
    val windSpeedMs: List<Double?>,
    val windDirectionDegrees: List<Double?>,
    val precipitationMm: List<Double?>,
    val rainMm: List<Double?>,
    val cloudCoverPercent: List<Double?>,
    val uvIndex: List<Double?>,
    val pm2_5: List<Double?>,
    val pm10: List<Double?>,
    val nitrogenDioxide: List<Double?>,
    val ozone: List<Double?>,
    val sulphurDioxide: List<Double?>,
    val carbonMonoxide: List<Double?>,
    val europeanAqi: List<Int?>,
    val usAqi: List<Int?>
)

/** Raw AQI response from Open-Meteo API. */
data class CamsAqis(
    val usAqi: Int? = null,
    val pm2_5: Double? = null,
    val pm10: Double? = null,
    val nitrogenDioxide: Double? = null,
    val ozone: Double? = null,
    val sulphurDioxide: Double? = null,
    val carbonMonoxide: Double? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/** Result of environmental data fetch. */
data class EnvironmentalDataResult(
    val weather: OpenMeteoWeather?,
    val aqis: CamsAqis?,
    val fetchTimestamp: Long
)

/** Data source types. */
enum class DataSource {
    OPEN_METEO,
    OPEN_METEO_AQ,
    CAMS,
    OVERPASS,
    MANUAL
}

enum class DataQuality {
    HIGH, MEDIUM, LOW, UNKNOWN, STALE, OUT_OF_RANGE
}
