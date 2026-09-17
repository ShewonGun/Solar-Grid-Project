/* ============================================================================
 * File        : QrCodeAnalyzer.kt
 * Purpose     : CameraX frame analyser that decodes QR codes with ZXing. Reads
 *               the luminance plane of each frame and reports the first code it
 *               recognises; the token is then checked by the Web API.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.ui.operator

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer

/**
 * Decodes QR codes from the camera stream and hands the first hit to [onQrCode].
 * Reporting stops after the first success so a held-up code is not scanned twice.
 */
class QrCodeAnalyzer(private val onQrCode: (String) -> Unit) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        setHints(
            mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE))
        )
    }

    /** True once a code has been reported, so later frames are ignored. */
    private var done = false

    /** Inspects one camera frame and closes it, whatever the outcome. */
    override fun analyze(image: ImageProxy) {
        if (done) {
            image.close()
            return
        }

        try {
            decode(image)?.let { text ->
                done = true
                onQrCode(text)
            }
        } finally {
            image.close()
        }
    }

    /** Runs ZXing over the frame's luminance plane, returning null when nothing is found. */
    private fun decode(image: ImageProxy): String? {
        val plane = image.planes.firstOrNull() ?: return null
        val buffer = plane.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        // The row stride is the real width of the buffer and can exceed the image
        // width, so it is passed as the data width rather than image.width.
        val source = PlanarYUVLuminanceSource(
            bytes,
            plane.rowStride,
            image.height,
            0,
            0,
            image.width,
            image.height,
            false
        )

        return try {
            reader.decode(BinaryBitmap(HybridBinarizer(source))).text
        } catch (e: ReaderException) {
            // No code in this frame; the next one is analysed a moment later.
            null
        } catch (e: IllegalArgumentException) {
            // Frame geometry the source rejected, e.g. during a rotation change.
            null
        } finally {
            reader.reset()
        }
    }
}
