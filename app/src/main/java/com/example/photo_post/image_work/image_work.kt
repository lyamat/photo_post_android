package com.example.photo_post.image_work

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.Base64
import android.widget.ImageView
import java.io.ByteArrayOutputStream



fun convertImageToBase64(rotatedBitmap: Bitmap): String {
    // Encode rotated bitmap to base64 and send to server
    val baos = ByteArrayOutputStream()
    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
    val imageBytes: ByteArray = baos.toByteArray()
    val imageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT)

    return imageBase64
}

fun getRotatedImageWithExif(pathToImage: String): Bitmap {
    val bitmap = BitmapFactory.decodeFile(pathToImage)

    // Read rotation information from metadata
    val ei = ExifInterface(pathToImage)
    val orientation = ei.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_UNDEFINED
    )

    fun Bitmap.rotate(degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    // Rotate bitmap based on orientation
    val rotatedBitmap = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> bitmap.rotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> bitmap.rotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> bitmap.rotate(270f)
        else -> bitmap
    }

    return rotatedBitmap
}

