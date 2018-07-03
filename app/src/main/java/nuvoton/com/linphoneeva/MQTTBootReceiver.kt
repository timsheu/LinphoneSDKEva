package nuvoton.com.linphoneeva

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.widget.Toast

/**
 * Created by cchsu20 on 2018/6/11.
 */
class MQTTBootReceiver : BroadcastReceiver() {
    private val TAG = "MQTTBootReceiver"
    override fun onReceive(context: Context?, intent: Intent?) {
        val message = "boot intent received, action=${intent?.action}"
        NuvotonLogger.debugMessage(TAG, message)
        NuvotonLogger.context = context?.applicationContext
        NuvotonLogger.debugMessageInPref("$TAG, ${intent?.action.toString()}", message)
        if (intent?.action.equals(Intent.ACTION_BOOT_COMPLETED) or
                intent?.action.equals(Intent.ACTION_REBOOT) or
                (intent?.action.toString() == "android.intent.action.OPPO_BOOT_COMPLETED")) {
        val openService = Intent(context, MQTTService::class.java)
            openService.putExtra("from", "MQTTBootReceiver")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NuvotonLogger.debugMessageInPref("$TAG, >O", "openService")
                context?.startForegroundService(openService)
            }else {
                NuvotonLogger.debugMessageInPref("$TAG, else O", "openService")
                context?.startService(openService)
                Toast.makeText(context, "start service, version is less than Android 8", Toast.LENGTH_LONG).show()
            }
        }
    }
}