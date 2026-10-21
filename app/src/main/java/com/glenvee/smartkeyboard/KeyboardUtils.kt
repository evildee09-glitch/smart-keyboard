package com.glenvee.smartkeyboard

import android.content.Context
import android.content.res.Configuration
import android.provider.Settings
import android.view.InputDevice

object KeyboardUtils {

    /** Physical (non-virtual) alphabetic keyboards currently connected. */
    fun externalKeyboards(): List<InputDevice> =
        InputDevice.getDeviceIds()
            .mapNotNull { InputDevice.getDevice(it) }
            .filter {
                !it.isVirtual &&
                    (it.sources and InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD &&
                    it.keyboardType == InputDevice.KEYBOARD_TYPE_ALPHABETIC
            }

    fun hasExternalKeyboard(ctx: Context): Boolean {
        val c = ctx.resources.configuration
        if (c.keyboard != Configuration.KEYBOARD_NOKEYS &&
            c.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO
        ) return true
        return externalKeyboards().isNotEmpty()
    }

    fun isDefaultIme(ctx: Context): Boolean {
        val current = Settings.Secure.getString(
            ctx.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD
        ) ?: return false
        return current.startsWith(ctx.packageName + "/")
    }
}
