package nuvoton.com.linphoneeva.utility

import android.content.Context
import android.util.Log

import org.acra.ReportField
import org.acra.collector.CrashReportData
import org.acra.sender.ReportSender
import org.acra.sender.ReportSenderException

import java.util.Date

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response


/**
 * Created by v-zhjoh on 2016/5/4.
 * Edited by timsheu on 2017
 */
class HockeySender : ReportSender {

    @Throws(ReportSenderException::class)
    override fun send(context: Context, report: CrashReportData) {

        val log = createCrashLog(report)

        //App id on Hockeyapp dashboard
        val formKey = "709e212998b94f6fbec0acecfae9762c"
        val url = BASE_URL + formKey + CRASHES_PATH
        val client = OkHttpClient()
        val formBody = FormBody.Builder()
                .add("raw", log)
                .build()
        val request = Request.Builder()
                .url(url)
                .post(formBody)
                .build()

        try {
            val response = client.newCall(request).execute()
            Log.d(TAG, "okhttp response: " + response.body()!!.string())
            //            DefaultHttpClient httpClient = new DefaultHttpClient();
            //            HttpPost httpPost = new HttpPost(url);
            //
            //            List<NameValuePair> parameters = new ArrayList<NameValuePair>();
            //            parameters.add(new BasicNameValuePair("raw", log));
            //            parameters.add(new BasicNameValuePair("userID", report.get(ReportField.INSTALLATION_ID)));
            //            parameters.add(new BasicNameValuePair("contact", report.get(ReportField.USER_EMAIL)));
            //            parameters.add(new BasicNameValuePair("description", report.get(ReportField.USER_COMMENT)));
            //
            //            httpPost.setEntity(new UrlEncodedFormEntity(parameters, HTTP.UTF_8));
            //
            //            httpClient.execute(httpPost);
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun createCrashLog(report: CrashReportData): String {
        val now = Date()
        val log = StringBuilder()

        log.append("Package: " + report[ReportField.PACKAGE_NAME] + "\n")
        log.append("Version: " + report[ReportField.APP_VERSION_CODE] + "\n")
        log.append("Android: " + report[ReportField.ANDROID_VERSION] + "\n")
        log.append("Manufacturer: " + android.os.Build.MANUFACTURER + "\n")
        log.append("Model: " + report[ReportField.PHONE_MODEL] + "\n")
        log.append("Date: " + now + "\n")
        log.append("\n")
        log.append(report[ReportField.STACK_TRACE])

        return log.toString()
    }

    companion object {
        private val TAG = "HockeySender"
        private val BASE_URL = "https://rink.hockeyapp.net/api/2/apps/"
        private val CRASHES_PATH = "/crashes"
    }
}