/* ============================================================================
 * File        : QrCodeEncoder.kt
 * Purpose     : Turns the transaction token issued by the Web API into a QR
 *               bitmap for the screen. Encoding only - the token itself is
 *               minted and verified by the service, never by this app.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.ui.qr

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** Black on white, because scanners read that far more reliably than tinted codes. */
private const val DARK_PIXEL = 0xFF000000.toInt()
private const val LIGHT_PIXEL = 0xFFFFFFFF.toInt()

/**
 * Encodes [content] as a square QR bitmap of [sizePx] pixels, or returns null
 * when the token is blank or ZXing refuses to encode it.
 */
fun encodeQrCode(content: String, sizePx: Int): ImageBitmap? {
    if (content.isBlank() || sizePx <= 0) return null

    return try {
        val hints = mapOf(
            // A quiet zone of one module keeps the code readable without wasting space.
            EncodeHintType.MARGIN to 1,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)

        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (matrix.get(x, y)) DARK_PIXEL else LIGHT_PIXEL
            }
        }

        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            .apply { setPixels(pixels, 0, width, 0, 0, width, height) }
            .asImageBitmap()
    } catch (e: WriterException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }
}
