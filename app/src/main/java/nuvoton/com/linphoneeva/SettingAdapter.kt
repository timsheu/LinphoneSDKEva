package nuvoton.com.linphoneeva

import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.ToggleButton

/**
 * Created by cchsu20 on 2018/6/28.
 */
class SettingAdapter(private val dataset: ArrayList<SettingListItem>) :
RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    class ButtonHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val category: TextView = view.findViewById(R.id.button_category)
        val button: ToggleButton = view.findViewById(R.id.button_button)
    }

    class EditTextHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val category: TextView = view.findViewById(R.id.edittext_category)
        val edittext: EditText = view.findViewById(R.id.edittext_text)
    }

    override fun getItemCount(): Int {
        return dataset.count()
    }

    override fun getItemViewType(position: Int): Int {
        return dataset[position].type.raw
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            SettingType.Button.raw -> {
                val view = LayoutInflater.from(parent?.context)
                        .inflate(R.layout.item_holder_button, parent, false)
                view.minimumHeight = 30
                ButtonHolder(view)
            }
            // SettingType.EditText.raw
            else -> {
                val view = LayoutInflater.from(parent?.context)
                        .inflate(R.layout.item_holder_edittext, parent, false)
                view.minimumHeight = 30
                EditTextHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        val item = dataset[position]
        when(item.type) {
            SettingType.Button -> {
                (holder as ButtonHolder).category.text = item.category
                if (item.value == "true") holder.button.toggle()
                holder.button.setOnCheckedChangeListener { buttonView, isChecked ->
                    SettingManager.shared.settingMap[item.category] = if (isChecked) "true" else "false"
                }
            }
            else -> {
                (holder as EditTextHolder).category.text = item.category
                holder.edittext.setText(item.value)
                holder.edittext.imeOptions = EditorInfo.IME_ACTION_DONE
                holder.edittext.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        SettingManager.shared.settingMap[item.category] = s?.toString() ?: "null"
                    }

                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    }

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                    }
                })
            }
        }
    }
}

class SettingListItem(val category: String, val value: String, val type: SettingType)

enum class SettingType(val raw: Int) {
    Button(0),
    EditText(1)
}