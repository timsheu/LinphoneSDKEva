package nuvoton.com.linphoneeva

import android.content.Context
import android.content.SharedPreferences

/**
 * Created by cchsu20 on 2018/4/20.
 */

enum class Category(val value: String) {
    DeviceIpAddress("sip:192.168.8.8"),
    EnableEarlyMedia("false"),
    EnableVideo("true"),
    EnableAudio("true"),
    EnableSpeaker("true"),
    LoginUsername("cchsu20"),
    LoginPassword("48694062"),
    LoginDomain("sip.linphone.org")
//    MQTTServerAddress("tcp://rak.adacomm.io"),
//    MQTTServerPort("1883"),
//    MQTTProductId("36AM17"),
//    MQTTClientId("null"),
//    MQTTControlTopic(""),
//    MQTTStatusTopic("")
}

class SettingManager private constructor(){
    val settingMap: LinkedHashMap<String, String> = LinkedHashMap()
    var pref: SharedPreferences? = null
    var ipAddress: String? = null
    var mContext: Context? = null
    set(value) {
        field = value?.applicationContext
        pref = field!!.getSharedPreferences("Nuvoton", Context.MODE_PRIVATE)
        readSettingsFromPref()
    }

    private object Holder { val INSTANCE = SettingManager() }

    companion object {
        val shared: SettingManager by lazy { Holder.INSTANCE }
    }

    private fun readSettingsFromPref() {
        Category.values().forEach { it ->
            settingMap[it.name] = pref?.getString(it.name, it.value) ?: it.value
        }
    }

    fun updateSettingsToPref() {
        settingMap.forEach { setting ->
            pref?.edit()?.putString(setting.key, setting.value)?.apply()
        }
    }
}