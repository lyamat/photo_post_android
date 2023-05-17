package com.example.photo_post.image_work

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer

class QRCodeScanner {

    private val multiFormatReader = MultiFormatReader()

    fun scanQRCode(bitmap: Bitmap): String? {
        val intArray = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val source = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

        return try {
            val result: Result = multiFormatReader.decodeWithState(binaryBitmap)
            result.text
        } catch (e: Exception) {
            Log.e("QRCodeScanner", "Error scanning QR code", e)
            null
        }
    }
}
