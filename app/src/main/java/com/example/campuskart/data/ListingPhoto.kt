package com.example.campuskart.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

/**
 * Turns whatever the camera or the photo picker handed back into the JPEG that actually gets
 * uploaded (SRS 2.5: "images compressed before upload to minimize footprint").
 *
 * This is not an optimisation detail. A modern phone camera produces a 12-megapixel, 4-6 MB
 * file; the app displays it as a feed card and a phone-width detail image. Uploading the
 * original would spend a student's mobile data and Cloudinary's free-tier quota on pixels no
 * screen in the app will ever show, and it is the difference between a publish that takes two
 * seconds and one that takes thirty on campus wifi.
 */
object ListingPhoto {

    private const val TAG = "CampusKartPhoto"

    /** Longest edge after downscaling. Comfortably above the largest size the app displays. */
    private const val MAX_DIMENSION = 1600

    private const val JPEG_QUALITY = 80

    /** Subdirectory of the cache the camera writes into. Mirrored in res/xml/file_paths.xml. */
    private const val CAMERA_DIR = "listing-photos"

    /**
     * Creates the empty file the system camera app will write the full-size photo into, and
     * wraps it in a content:// URI that camera app is allowed to open.
     *
     * A FileProvider URI rather than a plain file:// one, because passing a raw file path across
     * an app boundary has thrown FileUriExposedException since Android 7. The matching provider
     * is declared in AndroidManifest.xml.
     *
     * The file lands in the cache directory, so Android is free to reclaim it once the listing
     * has been uploaded - nothing in the app reads it again afterwards.
     */
    fun newCameraOutputUri(context: Context): Uri {
        val dir = File(context.cacheDir, CAMERA_DIR).apply { mkdirs() }
        val file = File(dir, "capture-${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /** Deletes captures left behind by a publish that was abandoned or has already finished. */
    fun clearCameraCache(context: Context) {
        runCatching { File(context.cacheDir, CAMERA_DIR).deleteRecursively() }
            .onFailure { Log.w(TAG, "Could not clear the camera cache", it) }
    }

    /**
     * Reads [uri], rotates it upright, scales the longest edge down to [MAX_DIMENSION] and
     * encodes it as JPEG. Returns null if the image cannot be read or decoded at all.
     *
     * Decoding runs on the IO dispatcher and, more importantly, in two passes: the first reads
     * only the header to learn the real dimensions, so the second can ask BitmapFactory for a
     * pre-subsampled bitmap. Decoding a 12 MP photo at full size and shrinking it afterwards
     * needs about 48 MB of heap for a bitmap that is immediately thrown away, which is how image
     * pickers end up with OutOfMemoryError on cheaper devices.
     */
    suspend fun prepareForUpload(context: Context, uri: Uri): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                // The bounds pass reads the header only. `decodeStream` returns null here by
                // contract - inJustDecodeBounds means "measure it, do not allocate it" - so the
                // stream, not the decode result, is what says whether the photo was readable.
                // Treating that documented null as a failure is a real bug this hit once: every
                // capture was rejected with "that photo could not be read" while the on-screen
                // preview of the very same URI drew perfectly.
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                val opened = context.contentResolver.openInputStream(uri) ?: return@withContext null
                opened.use { BitmapFactory.decodeStream(it, null, bounds) }

                val options = BitmapFactory.Options().apply {
                    inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
                }
                // Here a null result *is* a failure: this pass is asked for real pixels.
                val decoded = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                } ?: return@withContext null

                val upright = applyOrientation(context, uri, decoded)
                val scaled = scaleToFit(upright)

                ByteArrayOutputStream().use { out ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                    scaled.recycle()
                    out.toByteArray()
                }
            } catch (e: IOException) {
                Log.w(TAG, "Could not read the selected photo", e)
                null
            } catch (e: OutOfMemoryError) {
                // Possible on a low-memory device with an unusually large image even after
                // subsampling. Reported to the user as "could not read the photo" rather than
                // taking the whole app down with it.
                Log.w(TAG, "Ran out of memory decoding the selected photo", e)
                null
            }
        }

    /** Largest power-of-two subsample that still leaves both edges at or above the target. */
    private fun sampleSizeFor(width: Int, height: Int): Int {
        if (width <= 0 || height <= 0) return 1
        var sample = 1
        while (width / (sample * 2) >= MAX_DIMENSION && height / (sample * 2) >= MAX_DIMENSION) {
            sample *= 2
        }
        return sample
    }

    /**
     * Phone cameras do not rotate the pixels when the phone is held sideways - they store the
     * orientation as an EXIF tag and leave the image as the sensor saw it. Coil and the system
     * gallery read that tag, but JPEG bytes re-encoded from a Bitmap have lost it, so the
     * rotation has to be baked into the pixels here or every photo taken in portrait arrives on
     * the feed lying on its side.
     */
    private fun applyOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: IOException) {
            // Not every source carries EXIF (a screenshot, a PNG from the picker). Assume upright.
            Log.i(TAG, "No EXIF orientation on the selected photo", e)
            ExifInterface.ORIENTATION_NORMAL
        }

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        return bitmap.transformed(matrix)
    }

    /** Final exact scale - subsampling only gets within a factor of two of the target. */
    private fun scaleToFit(bitmap: Bitmap): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= MAX_DIMENSION) return bitmap
        val factor = MAX_DIMENSION.toFloat() / longest
        return bitmap.transformed(Matrix().apply { postScale(factor, factor) })
    }

    /**
     * `createBitmap` returns the *same* instance when the matrix turns out to be a no-op, so the
     * source is only recycled when a genuinely new bitmap came back - recycling it otherwise
     * would free the very bitmap being returned.
     */
    private fun Bitmap.transformed(matrix: Matrix): Bitmap =
        Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
            .also { if (it !== this) recycle() }
}
