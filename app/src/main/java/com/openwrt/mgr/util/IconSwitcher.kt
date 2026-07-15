package com.openwrt.mgr.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import com.openwrt.mgr.ui.theme.AppIconStyle
import java.io.File
import java.io.FileOutputStream

object IconSwitcher {
    private const val PKG = "com.openwrt.mgr"
    private val aliasClasses = listOf(
        "MainAliasSakura",
        "MainAliasMint",
        "MainAliasOcean",
        "MainAliasMinimal",
        "MainAliasCustom"
    )

    fun customIconFile(context: Context): File = File(context.filesDir, "custom_launcher_icon.png")

    fun applyStyle(context: Context, style: AppIconStyle) {
        val pm = context.packageManager
        val enable = style.componentSuffix
        aliasClasses.forEach { suffix ->
            val cn = ComponentName(PKG, "$PKG.$suffix")
            val state = if (suffix == enable) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            runCatching {
                pm.setComponentEnabledSetting(cn, state, PackageManager.DONT_KILL_APP)
            }
        }
    }

    fun saveCustomIcon(context: Context, uri: Uri): Boolean {
        return try {
            // Take persistable permission when possible so re-open works after reboot.
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val original = context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            } ?: return false

            val size = 512
            val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            // fill background so adaptive icon doesn't look empty
            canvas.drawColor(0xFFB12D5B.toInt())
            val scale = maxOf(size.toFloat() / original.width, size.toFloat() / original.height)
            val w = original.width * scale
            val h = original.height * scale
            val left = (size - w) / 2f
            val top = (size - h) / 2f
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(original, null, RectF(left, top, left + w, top + h), paint)

            val masked = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val mc = Canvas(masked)
            val path = Path().apply {
                addRoundRect(
                    RectF(size * 0.08f, size * 0.08f, size * 0.92f, size * 0.92f),
                    size * 0.18f,
                    size * 0.18f,
                    Path.Direction.CW
                )
            }
            mc.clipPath(path)
            mc.drawBitmap(out, 0f, 0f, paint)

            FileOutputStream(customIconFile(context)).use { fos ->
                masked.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            // Also try writing to cache for Coil preview stability
            runCatching {
                FileOutputStream(File(context.cacheDir, "custom_launcher_preview.png")).use { fos ->
                    masked.compress(Bitmap.CompressFormat.PNG, 100, fos)
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun clearCustomIcon(context: Context) {
        runCatching { customIconFile(context).delete() }
        runCatching { File(context.cacheDir, "custom_launcher_preview.png").delete() }
    }
}
