package nuvoton.com.linphoneeva

import android.util.Log

/**
 * Created by cchsu20 on 2018/6/8.
 */
class NuvotonLogger {
    companion object {
        var DEBUG = true
        fun debugMessage(tag: String, message: String) {
            if (DEBUG) Log.d(tag, message)
        }

        fun infoMessage(tag: String, message: String) {
            if (DEBUG) Log.i(tag, message)
        }
    }
}