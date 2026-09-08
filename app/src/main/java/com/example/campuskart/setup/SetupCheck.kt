package com.example.campuskart.setup

import android.util.Log
import com.example.campuskart.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * TEMPORARY (Day 1). A one-shot check that every external service CampusKart depends on is
 * configured and reachable, before any real feature code exists. It writes to Logcat under the
 * tag [TAG] and is rendered on screen by MainActivity.
 *
 * This whole file is scaffolding and gets deleted once the real screens land (Day 3 onwards).
 */
object SetupCheck {

    const val TAG = "CampusKartSetup"

    /** project_id in the committed google-services.json.template placeholder. */
    private const val PLACEHOLDER_PROJECT_ID = "campuskart-placeholder"

    enum class Status { OK, WARN, FAIL }

    data class Result(val label: String, val status: Status, val detail: String)

    suspend fun runAll(): List<Result> {
        val results = mutableListOf<Result>()
        results += checkFirebaseConfig()
        results += checkFirebaseAuth()
        results += checkFirestore()
        results += checkCloudinaryConfig()
        results += checkCloudinaryReachable()

        results.forEach { Log.i(TAG, "[${it.status}] ${it.label} - ${it.detail}") }
        return results
    }

    private fun checkFirebaseConfig(): Result {
        val projectId = runCatching { FirebaseApp.getInstance().options.projectId }.getOrNull()
        return when {
            projectId == null -> Result(
                "Firebase config",
                Status.FAIL,
                "google-services.json was not read. Is app/google-services.json present?",
            )

            projectId == PLACEHOLDER_PROJECT_ID -> Result(
                "Firebase config",
                Status.FAIL,
                "Still using the placeholder file. Download the real google-services.json from " +
                    "the Firebase Console into app/ (see README -> Setup).",
            )

            else -> Result("Firebase config", Status.OK, "Project: $projectId")
        }
    }

    private fun checkFirebaseAuth(): Result = runCatching {
        val user = FirebaseAuth.getInstance().currentUser
        Result(
            "Firebase Auth SDK",
            Status.OK,
            if (user == null) "Initialised, nobody signed in yet" else "Signed in as ${user.email}",
        )
    }.getOrElse { Result("Firebase Auth SDK", Status.FAIL, it.message ?: it.toString()) }

    /**
     * Reads a document that is never expected to exist, forcing a real round trip to the server
     * (rather than the local cache) so this genuinely proves Firestore is reachable.
     */
    private suspend fun checkFirestore(): Result = try {
        FirebaseFirestore.getInstance()
            .collection("_setup").document("ping")
            .get(Source.SERVER)
            .await()
        Result("Cloud Firestore", Status.OK, "Reached the database and completed a read")
    } catch (e: FirebaseFirestoreException) {
        if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
            // Reaching the server at all is what we are testing; rules rejecting us still proves it.
            Result(
                "Cloud Firestore",
                Status.WARN,
                "Reachable, but security rules denied the read. Expected if the database was not " +
                    "created in test mode.",
            )
        } else {
            Result("Cloud Firestore", Status.FAIL, "${e.code}: ${e.message}")
        }
    } catch (e: Exception) {
        Result("Cloud Firestore", Status.FAIL, e.message ?: e.toString())
    }

    private fun checkCloudinaryConfig(): Result = when {
        BuildConfig.CLOUDINARY_CLOUD_NAME.isBlank() -> Result(
            "Cloudinary config",
            Status.FAIL,
            "cloudinary.cloudName is not set in local.properties (see README -> Setup)",
        )

        BuildConfig.CLOUDINARY_UPLOAD_PRESET.isBlank() -> Result(
            "Cloudinary config",
            Status.FAIL,
            "cloudinary.uploadPreset is not set in local.properties (see README -> Setup)",
        )

        else -> Result(
            "Cloudinary config",
            Status.OK,
            "Cloud '${BuildConfig.CLOUDINARY_CLOUD_NAME}', preset '${BuildConfig.CLOUDINARY_UPLOAD_PRESET}'",
        )
    }

    /**
     * Every new Cloudinary account ships with a demo image at .../image/upload/sample.jpg, so a
     * successful GET confirms both network access and that the cloud name is spelled correctly.
     */
    private suspend fun checkCloudinaryReachable(): Result {
        val cloudName = BuildConfig.CLOUDINARY_CLOUD_NAME
        if (cloudName.isBlank()) {
            return Result("Cloudinary reachable", Status.WARN, "Skipped - no cloud name configured")
        }
        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient.Builder()
                    .callTimeout(15, TimeUnit.SECONDS)
                    .build()
                val request = Request.Builder()
                    .url("https://res.cloudinary.com/$cloudName/image/upload/sample.jpg")
                    .head()
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Result("Cloudinary reachable", Status.OK, "Served the demo image (HTTP ${response.code})")
                    } else {
                        Result(
                            "Cloudinary reachable",
                            Status.WARN,
                            "HTTP ${response.code} - check the cloud name, or the account's demo " +
                                "sample.jpg may have been deleted",
                        )
                    }
                }
            } catch (e: Exception) {
                Result("Cloudinary reachable", Status.FAIL, e.message ?: e.toString())
            }
        }
    }
}
