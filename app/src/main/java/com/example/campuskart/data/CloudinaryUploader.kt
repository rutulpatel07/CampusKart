package com.example.campuskart.data

import android.util.Log
import com.example.campuskart.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Where a finished upload lives, or why it did not happen. */
sealed interface UploadOutcome {
    data class Success(val secureUrl: String) : UploadOutcome
    data class Failure(val message: String) : UploadOutcome
}

/**
 * Uploads a listing photo to Cloudinary and hands back the URL that goes into
 * [Listing.photoUrl] (FR-LIST-002).
 *
 * This is a hand-written multipart POST rather than Cloudinary's Android SDK. The SDK is built
 * around a background upload service with its own queue, callbacks and lifecycle, all of which
 * would have to be bridged back into a coroutine; the REST call it wraps is one request, and
 * OkHttp is already in the build for Coil. See README, "Challenges faced".
 *
 * The upload is *unsigned*: it authenticates with an upload preset (safe to ship in the app)
 * instead of the account's API secret (which would not be). The cloud name and preset are read
 * from local.properties at build time - see README, "Setup".
 */
object CloudinaryUploader {

    private const val TAG = "CampusKartUpload"

    /** Uploads run over campus wifi and mobile data, so this is generous rather than snappy. */
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .build()
    }

    val isConfigured: Boolean
        get() = BuildConfig.CLOUDINARY_CLOUD_NAME.isNotBlank() &&
            BuildConfig.CLOUDINARY_UPLOAD_PRESET.isNotBlank()

    /**
     * Posts [jpegBytes] and returns the `secure_url` Cloudinary responds with.
     *
     * Runs on the IO dispatcher: OkHttp's `execute()` blocks the calling thread, and the caller
     * is a ViewModel coroutine on the main one.
     */
    suspend fun upload(jpegBytes: ByteArray): UploadOutcome {
        if (!isConfigured) {
            return UploadOutcome.Failure(
                "Photo uploads are not configured on this build. See README - Setup.",
            )
        }

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            // The filename is required by the form encoding but never used - Cloudinary assigns
            // its own public ID, which is what ends up in the returned URL.
            .addFormDataPart(
                name = "file",
                filename = "listing.jpg",
                body = jpegBytes.toRequestBody("image/jpeg".toMediaType()),
            )
            .addFormDataPart("upload_preset", BuildConfig.CLOUDINARY_UPLOAD_PRESET)
            .build()

        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/${BuildConfig.CLOUDINARY_CLOUD_NAME}/image/upload")
            .post(body)
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    val payload = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        Log.w(TAG, "Cloudinary rejected the upload: HTTP ${response.code} $payload")
                        return@use UploadOutcome.Failure(errorMessage(response.code, payload))
                    }
                    val url = JSONObject(payload).optString("secure_url")
                    if (url.isEmpty()) {
                        Log.w(TAG, "Upload succeeded but no secure_url came back: $payload")
                        UploadOutcome.Failure("The photo uploaded but no link came back. Try again.")
                    } else {
                        UploadOutcome.Success(url)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Log.w(TAG, "Upload failed", e)
                UploadOutcome.Failure(
                    "Could not upload the photo. Check your connection and try again.",
                )
            } catch (e: Exception) {
                Log.w(TAG, "Unexpected upload failure", e)
                UploadOutcome.Failure("Could not upload the photo. Please try again.")
            }
        }
    }

    /**
     * Cloudinary returns its own reason in `{"error":{"message":"..."}}`. Two of them are worth
     * repeating verbatim because they are configuration mistakes only the developer can fix, and
     * "please try again" would send a user in circles over something retrying cannot solve.
     */
    private fun errorMessage(code: Int, payload: String): String {
        val reason = runCatching {
            JSONObject(payload).getJSONObject("error").optString("message")
        }.getOrNull().orEmpty()

        return when {
            reason.contains("preset", ignoreCase = true) ->
                "Cloudinary rejected the upload preset. Check cloudinary.uploadPreset in " +
                    "local.properties is an *unsigned* preset (README - Setup)."

            code == 401 || code == 403 ->
                "Cloudinary refused the upload ($code). Check the cloud name and that the preset " +
                    "is unsigned (README - Setup)."

            code == 413 -> "That photo is too large to upload."

            else -> "The photo could not be uploaded (HTTP $code). Please try again."
        }
    }
}
