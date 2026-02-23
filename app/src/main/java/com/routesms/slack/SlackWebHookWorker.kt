package com.routesms.slack

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

class SlackWebHookWorker(
    appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(appContext, workerParameters) {

    override suspend fun doWork(): Result {
        val requestBody = inputData.getString(BODY) ?: return Result.failure()
        val url = inputData.getString(URL) ?: return Result.failure()

        return try {
            val body = Gson().fromJson(requestBody, SlackWebHook::class.java)
            val response = getRetrofit()
                .create(SlackApi::class.java)
                .sendWebHook(url, body)
                .execute()
            if (response.body() == RESULT_OK) Result.success() else Result.retry()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun getRetrofit(): Retrofit {
        val gson = GsonBuilder().setLenient().create()
        val okHttpClient = OkHttpClient.Builder().build()
        return Retrofit.Builder()
            .baseUrl(WEB_HOOK_BASE_URL)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(okHttpClient)
            .build()
    }

    companion object {
        const val WEB_HOOK_BASE_URL = "https://hooks.slack.com/"
        const val BODY = "BODY"
        const val URL = "URL"
        const val RESULT_OK = "ok"
    }
}
