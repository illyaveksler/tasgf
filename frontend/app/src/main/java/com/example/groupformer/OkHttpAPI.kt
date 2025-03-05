package com.example.groupformer

import android.util.Log
import com.google.gson.Gson
import okhttp3.*
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import android.os.Handler
import android.os.Looper
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject

object OkHttpClientInstance {
    val client = OkHttpClient()
}

fun submitSurveyOkHttp(
    title: String,
    description: String,
    questions: List<QuestionText>,
    onSuccess: (QuestionnaireResponse) -> Unit,
    onFailure: (String) -> Unit
) {
    val url = "http://MyServiceLoadBa-vffahcwu-1375031736.us-east-1.elb.amazonaws.com/questionnaires"

    val questionnaire = QuestionnaireRequest(title, description, questions)
    val jsonBody = Gson().toJson(questionnaire)

    val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

    val request = Request.Builder()
        .url(url)
        .post(requestBody)
        .header("Content-Type", "application/json")
        .build()

    OkHttpClientInstance.client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("SurveyAPI", "Network Error: ${e.message}")
            onFailure("Network error: ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Log.e("SurveyAPI", "Error: $errorBody")
                    onFailure("Error: $errorBody")
                    return
                }

                val responseBody = response.body?.string()
                val questionnaireResponse = Gson().fromJson(responseBody, QuestionnaireResponse::class.java)

                Log.d("SurveyAPI", "Success: $responseBody")
                onSuccess(questionnaireResponse)
            }
        }
    })
}

fun fetchQuestionnaires(
    onSuccess: (List<Questionnaire>) -> Unit,
    onFailure: (String) -> Unit
) {
    val url = "http://MyServiceLoadBa-vffahcwu-1375031736.us-east-1.elb.amazonaws.com/questionnaires"

    val request = Request.Builder()
        .url(url)
        .get()
        .build()

    OkHttpClientInstance.client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("SurveyAPI", "Network Error: ${e.message}")
            Handler(Looper.getMainLooper()).post { onFailure("Network error: ${e.message}") }
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Log.e("SurveyAPI", "Error: $errorBody")
                    Handler(Looper.getMainLooper()).post { onFailure("Error: $errorBody") }
                    return
                }

                val responseBody = response.body?.string()
                val questionnaires = Gson().fromJson(responseBody, Array<Questionnaire>::class.java).toList()

                Handler(Looper.getMainLooper()).post {
                    onSuccess(questionnaires)
                }
            }
        }
    })
}

fun fetchQuestionnaire(
    questionnaireId: String,
    onSuccess: (QuestionnaireResponse) -> Unit,
    onFailure: (String) -> Unit
) {
    val url = "http://MyServiceLoadBa-vffahcwu-1375031736.us-east-1.elb.amazonaws.com/questionnaires/$questionnaireId"

    val request = Request.Builder()
        .url(url)
        .get()
        .build()

    OkHttpClientInstance.client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("SurveyAPI", "Network Error: ${e.message}")
            Handler(Looper.getMainLooper()).post { onFailure("Network error: ${e.message}") }
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Log.e("SurveyAPI", "Error: $errorBody")
                    Handler(Looper.getMainLooper()).post { onFailure("Error: $errorBody") }
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
    questionnaireId: String,
    answers: List<Answer>,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
) {
    val client = OkHttpClient()
    val gson = Gson()

    // API Endpoint
    val url = "http://MyServiceLoadBa-vffahcwu-1375031736.us-east-1.elb.amazonaws.com/questionnaires/$questionnaireId/answers"
    val studentId = (100000..999999).random()
    Log.d("SurveyAPI", "Answers: ${answers}")
    // Convert answers to JSON
    val requestBody = gson.toJson(
        mapOf(
            "studentId" to studentId,
            "answers" to answers
        )
    ).toRequestBody("application/json".toMediaTypeOrNull())

    val request = Request.Builder()
        .url(url)
        .post(requestBody)
        .addHeader("Content-Type", "application/json")
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("API", "Error submitting answers: ${e.message}")
            onFailure(e.message ?: "Unknown error")
        }

        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                Log.d("API", "Answers submitted successfully")
                onSuccess()
            } else {
                Log.e("API", "Failed to submit answers: ${response.message}")
                onFailure(response.message ?: "Unknown error")
            }
        }
    })
}

fun formGroups(questionnaireId: String, groupSize: Int, onResult: (List<List<Int>>?, String?) -> Unit) {
    val url = "http://MyServiceLoadBa-vffahcwu-1375031736.us-east-1.elb.amazonaws.com/questionnaires/$questionnaireId/generate-groups?groupSize=$groupSize"

    val request = Request.Builder()
        .url(url)
        .get()
        .build()

    OkHttpClient().newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Handler(Looper.getMainLooper()).post {
                onResult(null, e.message ?: "Unknown error")
            }
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                if (!it.isSuccessful) {
                    Handler(Looper.getMainLooper()).post {
                        onResult(null, "Error: ${it.code}")
                    }
                    return
                }

                val responseBody = it.body?.string()
                Log.d("API_RESPONSE", "Raw response: $responseBody")

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

                    Handler(Looper.getMainLooper()).post {
                        onResult(groupData, null) // Send the parsed data back
                    }
                } catch (e: Exception) {
                    Handler(Looper.getMainLooper()).post {
                        onResult(null, "Parsing error: ${e.message}")
                    }
                }
            }
        }
    })
}



