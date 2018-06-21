package nuvoton.com.linphoneeva

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager

/**
 * Created by cchsu20 on 2018/6/11.
 */
class MQTTBootReceiver : BroadcastReceiver() {
    private val TAG = "MQTTBootReceiver"
    override fun onReceive(context: Context?, intent: Intent?) {
//        if (intent?.action?.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)!!) {
            NuvotonLogger.debugMessage(TAG, "boot intent received")
            val openService = Intent(context, MQTTService::class.java)
            openService.putExtra("from", "MQTTBootReceiver")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context?.startForegroundService(intent)
            }else {
                context?.startService(intent)
            }
//        }
    }
}