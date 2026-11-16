package com.kichikan.negah

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Typeface
import com.kichikan.negah.domain.member.JalaliCalendar
import java.time.LocalDate

/**
 * Notification small icon: a calendar page showing today's Solar Hijri day of month.
 * Android renders small icons as an alpha mask, so the digits are punched out of a white page.
 */
object DayIcon {
    private const val SIZE = 96

    fun persianDigits(value: Int): String =
        value.toString().map { '۰' + (it - '0') }.joinToString("")

    fun render(date: LocalDate): Bitmap {
        val day = JalaliCalendar.toJalali(date).third
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
        canvas.drawRoundRect(RectF(6f, 10f, SIZE - 6f, SIZE - 6f), 12f, 12f, fill)
        // Binding rings above the page.
        canvas.drawRoundRect(RectF(26f, 2f, 34f, 20f), 4f, 4f, fill)
        canvas.drawRoundRect(RectF(62f, 2f, 70f, 20f), 4f, 4f, fill)
        val cut = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = if (day >= 10) 46f else 56f
        }
        val baseline = 28f + (SIZE - 6f - 28f) / 2f + cut.textSize * 0.35f
        canvas.drawText(persianDigits(day), SIZE / 2f, baseline, cut)
        return bitmap
    }
}
