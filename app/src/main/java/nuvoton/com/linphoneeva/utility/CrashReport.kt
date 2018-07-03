package nuvoton.com.linphoneeva.utility

import android.app.Application
import android.content.Context
import android.widget.Toast

import nuvoton.com.linphoneeva.R

import org.acra.ACRA
import org.acra.ReportField
import org.acra.ReportingInteractionMode
import org.acra.annotation.ReportsCrashes

/**
 * Created by timsheu on 7/15/16.
 */


@ReportsCrashes(mode = ReportingInteractionMode.TOAST,
        resToastText = R.string.toast_crash_report,
        reportSenderFactoryClasses = arrayOf(HockeySenderFactory::class),
        mailTo = "cchsu20@nuvoton.com",
        customReportContent = arrayOf(ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME, ReportField.ANDROID_VERSION, ReportField.PHONE_MODEL, ReportField.CUSTOM_DATA, ReportField.STACK_TRACE, ReportField.LOGCAT))

class CrashReport : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        val toastTextID = R.string.toast_crash_report
        if (ACRA.isInitialised()) {
            Toast.makeText(this, toastTextID, Toast.LENGTH_LONG).show()
        }
        ACRA.init(this)
    }

    companion object {
        private val TAG = "CrashReport"
    }
}
