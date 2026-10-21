package com.glenvee.smartkeyboard

import android.content.res.Configuration
import android.hardware.input.InputManager
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.InputDevice
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout

/**
 * A minimal keyboard that refuses to show itself while a physical keyboard is attached.
 * The key trick is onEvaluateInputViewShown(): when it returns false, Android never
 * displays the input view, and physical key events go straight to the app.
 */
class SmartKeyboardService : InputMethodService() {

    private var shift = false
    private var symbols = false
    private lateinit var container: LinearLayout
    private lateinit var inputManager: InputManager

    private val deviceListener = object : InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(deviceId: Int) = refresh()
        override fun onInputDeviceRemoved(deviceId: Int) = refresh()
        override fun onInputDeviceChanged(deviceId: Int) = refresh()
    }

    override fun onCreate() {
        super.onCreate()
        inputManager = getSystemService(INPUT_SERVICE) as InputManager
        inputManager.registerInputDeviceListener(deviceListener, Handler(Looper.getMainLooper()))
    }

    override fun onDestroy() {
        inputManager.unregisterInputDeviceListener(deviceListener)
        super.onDestroy()
    }

    private fun refresh() {
        updateInputViewShown()
        if (!hasExternalKeyboard()) return
        requestHideSelf(0)
    }

    private fun hasExternalKeyboard() = KeyboardUtils.hasExternalKeyboard(this)

    override fun onEvaluateInputViewShown(): Boolean {
        super.onEvaluateInputViewShown()
        return !hasExternalKeyboard()
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    // Refuse every show request (tap, mouse click, app call) while a hardware keyboard is attached.
    override fun onShowInputRequested(flags: Int, configChange: Boolean): Boolean {
        if (hasExternalKeyboard()) return false
        return super.onShowInputRequested(flags, configChange)
    }

    // Safety net: if the window still gets created, hide it straight away.
    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (hasExternalKeyboard()) requestHideSelf(0)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        refresh()
    }

    override fun onCreateInputView(): View {
        container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFE8EAED.toInt())
            setPadding(dp(4), dp(6), dp(4), dp(6))
        }
        render()
        return container
    }

    // ---------- UI ----------

    private fun render() {
        container.removeAllViews()
        if (symbols) {
            addRow("1234567890")
            addRow("@#$%&-+()/")
            addRow("*\"':;!?~=_")
        } else {
            addRow("qwertyuiop")
            addRow("asdfghjkl", sidePad = 0.5f)
            addRow("zxcvbnm", leading = key("⇧", 1.5f) { shift = !shift; render() },
                trailing = key("⌫", 1.5f) { backspace() })
        }
        val bottom = row()
        bottom.addView(key(if (symbols) "ABC" else "?123", 1.5f) { symbols = !symbols; render() })
        bottom.addView(key(",", 1f) { type(",") })
        bottom.addView(key("space", 5f) { type(" ") })
        bottom.addView(key(".", 1f) { type(".") })
        bottom.addView(key("⏎", 1.5f) { enter() })
        container.addView(bottom)
    }

    private fun row() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(52)
        )
    }

    private fun addRow(
        chars: String,
        sidePad: Float = 0f,
        leading: View? = null,
        trailing: View? = null
    ) {
        val r = row()
        if (sidePad > 0) r.addView(spacer(sidePad))
        leading?.let { r.addView(it) }
        for (ch in chars) {
            val label = if (shift && !symbols) ch.uppercaseChar().toString() else ch.toString()
            r.addView(key(label, 1f) { type(label) })
        }
        trailing?.let { r.addView(it) }
        if (sidePad > 0) r.addView(spacer(sidePad))
        container.addView(r)
    }

    private fun spacer(weight: Float) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(0, 1, weight)
    }

    private fun key(label: String, weight: Float, onClick: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 18f
        setPadding(0, 0, 0, 0)
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, weight)
            .also { it.setMargins(dp(2), dp(2), dp(2), dp(2)) }
        setOnClickListener { onClick() }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    // ---------- input ----------

    private fun type(s: String) {
        currentInputConnection?.commitText(s, 1)
        if (shift) { shift = false; render() }
    }

    private fun backspace() {
        val ic = currentInputConnection ?: return
        if (ic.getSelectedText(0).isNullOrEmpty()) ic.deleteSurroundingText(1, 0)
        else ic.commitText("", 1)
    }

    private fun enter() {
        val ei = currentInputEditorInfo
        val action = ei?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        val noEnterAction = ((ei?.imeOptions ?: 0) and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        if (!noEnterAction &&
            action != EditorInfo.IME_ACTION_NONE &&
            action != EditorInfo.IME_ACTION_UNSPECIFIED
        ) currentInputConnection?.performEditorAction(action)
        else sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
    }
}
