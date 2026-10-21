package com.glenvee.smartkeyboard

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class SettingsActivity : Activity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (24 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        root.addView(TextView(this).apply {
            textSize = 20f
            text = "Smart Keyboard\n\nHides the on-screen keyboard while an external keyboard is connected."
        })
        root.addView(Button(this).apply {
            text = "1. Enable Smart Keyboard"
            setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        })
        root.addView(Button(this).apply {
            text = "2. Select Smart Keyboard"
            setOnClickListener {
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
            }
        })
        status = TextView(this).apply { textSize = 16f; setPadding(0, pad, 0, 0) }
        root.addView(status)
        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        val kbs = KeyboardUtils.externalKeyboards()
        val sb = StringBuilder()
        sb.append("Smart Keyboard is the active keyboard: ")
            .append(if (KeyboardUtils.isDefaultIme(this)) "YES" else "NO  <- select it with step 2")
            .append("\n\nExternal keyboard detected: ")
            .append(if (KeyboardUtils.hasExternalKeyboard(this)) "YES" else "NO")
        if (kbs.isNotEmpty()) {
            sb.append("\n\nDevices:")
            kbs.forEach { sb.append("\n - ").append(it.name) }
        }
        sb.append("\n\n(Reopen this screen after connecting or disconnecting a keyboard to refresh.)")
        status.text = sb.toString()
    }
}
