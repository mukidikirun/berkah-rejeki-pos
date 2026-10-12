package com.berkahrejeki.pos.payment

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

object QrisGenerator {
    private const val MERCHANT_QRIS =
        "00020101021126610014COM.GO-JEK.WWW01189360091400000000000210G000000000303UMI51440014ID.CO.QRIS.WWW0215ID10200000000000303UMI5204541153033605802ID5910BERKAH REJ60075234567861051234562070703A01"

    fun buildDynamicQris(amount: Double): String {
        val amountStr = amount.toInt().toString()
        val tag54 = "54" + String.format("%02d", amountStr.length) + amountStr
        val withAmount = MERCHANT_QRIS + tag54
        val crc = crc16(withAmount + "6304")
        return withAmount + "6304" + crc
    }

    fun generateQrBitmap(content: String, size: Int = 600): Bitmap {
        val writer = QRCodeWriter()
        val matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) for (y in 0 until size) {
            bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
        }
        return bmp
    }

    private fun crc16(data: String): String {
        var crc = 0xFFFF
        for (c in data.toByteArray()) {
            crc = crc xor (c.toInt() and 0xFF shl 8)
            for (i in 0 until 8) {
                crc = if (crc and 0x8000 != 0) (crc shl 1) xor 0x1021 else crc shl 1
                crc = crc and 0xFFFF
            }
        }
        return String.format("%04X", crc)
    }
}
