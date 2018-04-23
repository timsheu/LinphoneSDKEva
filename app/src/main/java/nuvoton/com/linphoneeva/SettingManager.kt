package nuvoton.com.linphoneeva

import android.content.Context
import android.content.SharedPreferences

/**
 * Created by cchsu20 on 2018/4/20.
 */
class SettingManager {
    lateinit var pref: SharedPreferences
    var ipAddress: String? = null
    var mContext: Context? = null
    set(value) {
        pref = value!!.getSharedPreferences("LinphoneEVA", Context.MODE_PRIVATE)
    }

    companion object {
        val shared = SettingManager()
    }


    fun readIPAddress() : String {
        if (ipAddress == null){
            ipAddress = pref.getString("ipaddress", "sip:192.168.8.8")
        }
        return ipAddress!!
    }

    fun updateIPAddress(ip: String) : Boolean {
        var result = false
        if (ipAddress == null){
            ipAddress = ip
        }
        result = pref.edit().putString("ipaddress", ip).commit()
        return result
    }
}