package com.example.groupformer

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import okhttp3.*
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

private const val TAG = "SurveyAPI"

object OkHttpClientInstance {
    val client = OkHttpClient()
}

fun getAuthorizedRequest(context: Context, endpoint: String, method: String, body: RequestBody? = null): Request? {
    val sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
    val token = sharedPreferences.getString("google_id_token", null) ?: return null
    val baseUrl = sharedPreferences.getString("api_base_url", "https://sendsock.com")!!  // Default to real API
    val url = "$baseUrl$endpoint"

    Log.d(TAG, "Endpoint triggered: $endpoint")
    val builder = Request.Builder()
        .url(url)
        .header("Authorization", "Bearer $token")

    when (method) {
        "POST" -> builder.post(body!!)
        "GET" -> builder.get()
        "PUT" -> builder.put(body!!)
        "DELETE" -> builder.delete(body)
    }

    return builder.build()
}

fun submitSurveyOkHttp(
    context: Context,
    title: String,
    description: String,
    questions: List<QuestionText>,
    onSuccess: (QuestionnaireResponse) -> Unit,
    onFailure: (String) -> Unit
) {
    val url = "/questionnaires"

    val questionnaire = QuestionnaireRequest(title, description, questions)
    val jsonBody = Gson().toJson(questionnaire)
    val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

    val request = getAuthorizedRequest(context, url, "POST", requestBody)
        ?: return onFailure("Authentication error: No token found/Token expired")

    OkHttpClientInstance.client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e(TAG, "Network Error: ${e.message}")
            Handler(Looper.getMainLooper()).post { onFailure("Network error: ${e.message}") }
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Log.e(TAG, "Error submitting survey: $errorBody")
                    Handler(Looper.getMainLooper()).post { onFailure("Error creating survey: $errorBody") }
                    return
                }
                val responseBody = response.body?.string()
                val questionnaireResponse = Gson().fromJson(responseBody, QuestionnaireResponse::class.java)
                Handler(Looper.getMainLooper()).post { onSuccess(questionnaireResponse) }
            }
        }
    })
}

fun fetchQuestionnaires(context: Context, onSuccess: (List<Questionnaire>) -> Unit, onFailure: (String) -> Unit) {
    val url = "/questionnaires"
    val request = getAuthorizedRequest(context, url, "GET") ?: return onFailure("Authentication error: No token found")

    OkHttpClientInstance.client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e(TAG, "Network Error: ${e.message}")
            Handler(Looper.getMainLooper()).post { onFailure("Network error: ${e.message}") }
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                val responseBody = response.body?.string()

                if (!response.isSuccessful) {
                    val errorBody = responseBody ?: "Unknown error"
                    Log.e(TAG, "Error: $errorBody")
                    Handler(Looper.getMainLooper()).post { onFailure("Error fetching surveys: $errorBody") }
                    return
                }

                try {
                    val questionnaires = Gson().fromJson(responseBody, Array<Questionnaire>::class.java).toList()
                    Handler(Looper.getMainLooper()).post { onSuccess(questionnaires) }
                } catch (e: Exception) {
                    Log.e(TAG, "Parsing Error: ${e.message}")
                    Handler(Looper.getMainLooper()).post { onFailure("Parsing error: ${e.message}") }
                }
            }
        }
    })
}

fun fetchQuestionnaire(
    context: Context,
    questionnaireId: String,
    onSuccess: (QuestionnaireResponse) -> Unit,
    onFailure: (String) -> Unit
) {
    val url = "/questionnaires/$questionnaireId"
    val request = getAuthorizedRequest(context, url, "GET") ?: return onFailure("Authentication error: No token found")

    OkHttpClientInstance.client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e(TAG, "Network Error: ${e.message}")
            Handler(Looper.getMainLooper()).post { onFailure("Network error: ${e.message}") }
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Log.e(TAG, "Error: $errorBody")
                    Handler(Looper.getMainLooper()).post { onFailure("Error fetching survey: $errorBody") }
                    return
                }

                val responseBody = response.body?.string()
                val questionnaire = Gson().fromJson(responseBody, QuestionnaireResponse::class.java)

                Handler(Looper.getMainLooper()).post {
                    onSuccess(questionnaire)
                }
            }
        }
    })
}

fun submitAnswersOkHttp(
    context: Context,
    questionnaireId: String,
    answers: List<Answer>,
    studentId: Int,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
) {
    val url = "/questionnaires/$questionnaireId/answers"
    val requestBody = Gson().toJson(mapOf("studentId" to studentId, "answers" to answers))
        .toRequestBody("application/json".toMediaTypeOrNull())

    val request = getAuthorizedRequest(context, url, "POST", requestBody)
        ?: return onFailure("Authentication error: No token found")

    OkHttpClientInstance.client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e(TAG, "Error submitting answers: ${e.message}")
            Handler(Looper.getMainLooper()).post { onFailure(e.message ?: "Unknown error") }
        }

        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                Handler(Looper.getMainLooper()).post { onSuccess() }
            } else {
                Handler(Looper.getMainLooper()).post { onFailure("Failed: ${response.message}") }
            }
        }
    })
}

fun formGroups(
    context: Context,
    questionnaireId: String,
    groupSize: Int,
    onResult: (List<List<Int>>?, String?) -> Unit
) {
    val url = "/questionnaires/$questionnaireId/generate-groups?groupSize=$groupSize"
    val request = getAuthorizedRequest(context, url, "GET") ?: return onResult(null, "Authentication error: No token found")

    OkHttpClientInstance.client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Handler(Looper.getMainLooper()).post { onResult(null, e.message ?: "Unknown error") }
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                if (!it.isSuccessful) {
                    try {
                        val errorBody = it.body?.string()
                        Handler(Looper.getMainLooper()).post { onResult(null, "Error forming groups: $errorBody") }
                    } catch (e: Exception) {
                        // If there's an error parsing the error body, fall back to using the status code
                        Handler(Looper.getMainLooper()).post { onResult(null, "Error forming groups: ${it.code}") }
                    }
                    return
                }
                val responseBody = it.body?.string()

                try {
                    val jsonObject = JSONObject(responseBody)
                    val groupsArray = jsonObject.getJSONArray("groups")
                    val groupData = mutableListOf<List<Int>>()
                    for (i in 0 until groupsArray.length()) {
                        val group = groupsArray.getJSONArray(i)
                        val studentIds = mutableListOf<Int>()
                        for (j in 0 until group.length()) {
                            studentIds.add(group.getInt(j))
                        }
                        groupData.add(studentIds)
                    }
                    Handler(Looper.getMainLooper()).post { onResult(groupData, null) }
                } catch (e: Exception) {
                    Handler(Looper.getMainLooper()).post { onResult(null, "Error forming groups: {Parsing error: ${e.message}}") }
                }
            }
        }
    })
}
