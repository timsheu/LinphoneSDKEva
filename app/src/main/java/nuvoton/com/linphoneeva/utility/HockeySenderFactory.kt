package nuvoton.com.linphoneeva.utility


import android.content.Context

import org.acra.config.ACRAConfiguration
import org.acra.sender.ReportSender
import org.acra.sender.ReportSenderFactory

/**
 * Created by v-zhjoh on 2016/5/4.
 * Edited by timsheu on 2017
 */
class HockeySenderFactory : ReportSenderFactory {
    override fun create(context: Context, config: ACRAConfiguration): ReportSender {
        return HockeySender()
    }
}