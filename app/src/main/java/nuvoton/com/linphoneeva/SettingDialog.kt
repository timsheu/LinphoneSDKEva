package nuvoton.com.linphoneeva

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ToggleButton
import com.afollestad.materialdialogs.MaterialDialog

/**
 * Created by cchsu20 on 2018/6/28.
 */
class SettingDialog {
    companion object {
        fun create(context: Context) {
            val dataset = ArrayList<SettingListItem>()
            val map = SettingManager.shared.settingMap

            map.entries.withIndex().forEach { (index, entry) ->
                var type = SettingType.Button
                if (index > 4) type = SettingType.EditText
                dataset.add(SettingListItem(entry.key, entry .value, type))
            }

            val dialog = MaterialDialog.Builder(context).title(R.string.title_setting)
                    .positiveText("Save")
                    .adapter(SettingAdapter(dataset), null)
                    .onPositive { dialog, action ->
                        SettingManager.shared.updateSettingsToPref()
                    }
                    .neutralText("Cancel")
                    .build()

//            val listView = dialog.customView?.findViewById<RecyclerView>(R.id.recycle_setting_view)

//            listView?.adapter = SettingAdapter(dataset)
//            val toggleButtonListener = CompoundButton.OnCheckedChangeListener { it, checked ->
//                var category = ""
//                when (it.id) {
//                    R.id.toggle_earlymedia -> {
//                        category = Category.EnableEarlyMedia.name
//                    }
//                    R.id.toggle_video -> {
//                        category = Category.EnableVideo.name
//                    }
//                    R.id.toggle_audio -> {
//                        category = Category.EnableAudio.name
//                    }
//                    R.id.toggle_speaker -> {
//                        category = Category.EnableSpeaker.name
//                    }
//                }
//                SettingManager.shared.settingMap[category] = if (checked) "true" else "false"
//            }
//            val earlyMediaButton = dialog.customView?.findViewById<ToggleButton>(R.id.toggle_earlymedia)
//            if (SettingManager.shared.settingMap[Category.EnableEarlyMedia.name] == "true") {
//                earlyMediaButton?.toggle()
//            }
//            earlyMediaButton?.setOnCheckedChangeListener(toggleButtonListener)
//
//            val videoButton = dialog.customView?.findViewById<ToggleButton>(R.id.toggle_video)
//            if (SettingManager.shared.settingMap[Category.EnableVideo.name] == "true") {
//                videoButton?.toggle()
//            }
//            videoButton?.setOnCheckedChangeListener(toggleButtonListener)
//
//            val audioButton = dialog.customView?.findViewById<ToggleButton>(R.id.toggle_audio)
//            if (SettingManager.shared.settingMap[Category.EnableAudio.name] == "true") {
//                audioButton?.toggle()
//            }
//            audioButton?.setOnCheckedChangeListener(toggleButtonListener)
//
//            val speakerButton = dialog.customView?.findViewById<ToggleButton>(R.id.toggle_speaker)
//            if (SettingManager.shared.settingMap[Category.EnableSpeaker.name] == "true") {
//                speakerButton?.toggle()
//            }
//            speakerButton?.setOnCheckedChangeListener(toggleButtonListener)
//
//            val linphoneUsername = dialog?.customView?.findViewById<EditText>(R.id.text_username)
//            linphoneUsername?.setText(SettingManager.shared.settingMap[Category.LoginUsername.name])
//            val linphonePassword = dialog?.customView?.findViewById<EditText>(R.id.text_password)
//            linphonePassword?.setText(SettingManager.shared.settingMap[Category.LoginPassword.name])
//            val linphoneDomain = dialog?.customView?.findViewById<EditText>(R.id.text_domain)
//            linphoneDomain?.setText(SettingManager.shared.settingMap[Category.LoginDomain.name])
//            val mqttServer = dialog?.customView?.findViewById<EditText>(R.id.text_mqtt_server)
//            mqttServer?.setText(SettingManager.shared.settingMap[Category.MQTTServerAddress.name])
//            val mqttPort = dialog?.customView?.findViewById<EditText>(R.id.text_mqtt_port)
//            mqttPort?.setText(SettingManager.shared.settingMap[Category.MQTTServerPort.name])
//            val mqttProductId = dialog?.customView?.findViewById<EditText>(R.id.text_mqtt_product_id)
//            mqttProductId?.setText(SettingManager.shared.settingMap[Category.MQTTProductId.name])
//            val mqttClientId = dialog?.customView?.findViewById<EditText>(R.id.text_mqtt_client_id)
//            mqttClientId?.setText(SettingManager.shared.settingMap[Category.MQTTClientId.name])
//            val mqttControlTopic = dialog?.customView?.findViewById<EditText>(R.id.text_mqtt_topic_control)
//            mqttControlTopic?.setText(SettingManager.shared.settingMap[Category.MQTTControlTopic.name])
//            val mqttStatusTopic = dialog?.customView?.findViewById<EditText>(R.id.text_mqtt_topic_status)
//            mqttStatusTopic?.setText(SettingManager.shared.settingMap[Category.MQTTStatusTopic.name])
//
//            val textListener = object : TextWatcher {
//                override fun afterTextChanged(s: Editable?) {
//                    var category = ""
//                    when(s?.hashCode()) {
//                        linphoneUsername?.text?.hashCode() -> {
//                            category = Category.LoginUsername.name
//                        }
//                        linphonePassword?.text?.hashCode() -> {
//                            category = Category.LoginPassword.name
//                        }
//                        linphoneDomain?.text?.hashCode() -> {
//                            category = Category.LoginDomain.name
//                        }
//                        mqttServer?.text?.hashCode() -> {
//                            category = Category.MQTTServerAddress.name
//                        }
//                        mqttPort?.text?.hashCode() -> {
//                            category = Category.MQTTServerPort.name
//                        }
//                        mqttProductId?.text?.hashCode() -> {
//                            category = Category.MQTTProductId.name
//                        }
//                        mqttClientId?.text?.hashCode() -> {
//                            category = Category.MQTTClientId.name
//                        }
//                        mqttControlTopic?.text?.hashCode() -> {
//                            category = Category.MQTTControlTopic.name
//                        }
//                        mqttStatusTopic?.text?.hashCode() -> {
//                            category = Category.MQTTStatusTopic.name
//                        }
//                    }
//                    SettingManager.shared.settingMap[category] = s?.toString() ?: "null"
//                }
//
//                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
//
//                }
//
//                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//
//                }
//            }
//            linphoneUsername?.addTextChangedListener(textListener)
//            linphonePassword?.addTextChangedListener(textListener)
//            linphoneDomain?.addTextChangedListener(textListener)
//            mqttServer?.addTextChangedListener(textListener)
//            mqttPort?.addTextChangedListener(textListener)
//            mqttProductId?.addTextChangedListener(textListener)
//            mqttClientId?.addTextChangedListener(textListener)
//            mqttControlTopic?.addTextChangedListener(textListener)
//            mqttStatusTopic?.addTextChangedListener(textListener)
            dialog.show()
        }
    }
}