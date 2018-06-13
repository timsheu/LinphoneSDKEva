package nuvoton.com.linphoneeva

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Created by cchsu20 on 2018/6/11.
 */
class MQTTBootReceiver : BroadcastReceiver() {
    private val TAG = "MQTTBootReceiver"
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action?.equals(Intent.ACTION_BOOT_COMPLETED)!!) {
            NuvotonLogger.debugMessage(TAG, "boot intent received")
            val openService = Intent(context, MQTTService::class.java)
            openService.putExtra("from", "MQTTBootReceiver")
            context?.startService(openService)
        }
    }
}