package nuvoton.com.linphoneeva

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Created by cchsu20 on 14/03/2018.
 */
class LinphoneMiniUtils {
    companion object {
        @Throws(IOException::class)
        fun copyIfNotExist(context: Context, resSourceId: Int, target: String) {
            val lFilesToCopy = File(target)
            if ( !lFilesToCopy.exists() ){
                copyFromPackage(context, resSourceId, target)
            }
        }

        @Throws(IOException::class)
        public fun copyFromPackage(context: Context, resSourceId: Int, target: String) {
            val lOutputStream =  FileOutputStream(File(target))
//            val lOutputStream = context.openFileOutput(target, Context.MODE_PRIVATE)
            val lInputStream = context.resources.openRawResource(resSourceId)
            lInputStream.use { input ->
                lOutputStream.use { fileOut ->
                    input.copyTo(fileOut)
                }
            }
        }
    }
}