package nuvoton.com.linphoneeva

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction
import android.support.v7.app.AppCompatActivity
import android.util.Log
import java.net.NetworkInterface
import java.net.SocketException


/**
 * Created by cchsu20 on 2018/4/11.
 */

inline fun FragmentManager.inTransaction(func: FragmentTransaction.() -> Unit) {
    val fragmentTransaction = beginTransaction()
    fragmentTransaction.func()
    fragmentTransaction.commit()
}

fun AppCompatActivity.addFragment(fragment: Fragment, frameId: Int) {
    supportFragmentManager.inTransaction { add(frameId, fragment) }
}

fun AppCompatActivity.replaceFragment(fragment: Fragment, frameId: Int){
    supportFragmentManager.inTransaction { replace(frameId, fragment) }
}

fun AppCompatActivity.removeFragment(fragment: Fragment) {
    supportFragmentManager.inTransaction { remove(fragment) }
}

fun getLocalIpAddress(): String? {
    try {
        val en = NetworkInterface.getNetworkInterfaces()
        while (en.hasMoreElements()) {
            val intf = en.nextElement()
            val enumIpAddr = intf.getInetAddresses()
            while (enumIpAddr.hasMoreElements()) {
                val inetAddress = enumIpAddr.nextElement()
                if (!inetAddress.isLoopbackAddress()) {
                    return inetAddress.getHostAddress().toString()
                }
            }
        }
    } catch (ex: SocketException) {
        Log.e("SocketException", ex.toString())
    }

    return null
}