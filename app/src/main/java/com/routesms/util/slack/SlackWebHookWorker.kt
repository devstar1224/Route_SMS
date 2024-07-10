package com.routesms.util.slack

import SelfSigningHelper
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory


class SlackWebHookWorker(
    private val appContext: Context,
    workerParameters: WorkerParameters
) : Worker(appContext, workerParameters) {
    override fun doWork(): Result {
        val requestBody = inputData.getString(BODY) ?: return Result.failure()
        val path = getApiKeyFromManifest(appContext) ?: return Result.failure()
        return when (sendWebHook(path, requestBody)) {
            RESULT_OK -> Result.success()
            else -> Result.failure()
        }
    }

    private fun sendWebHook(path: String, requestBody: String): String? {
        return try {
            val body = Gson().fromJson(requestBody, SlackWebHook::class.java)
            val slackWebHookResult =
                getRetrofit(appContext).create(SlackApi::class.java).sendWebHook(path, body)
            slackWebHookResult.execute().body()
        } catch (e: Exception) {
            ""
        }
    }

    private fun getApiKeyFromManifest(context: Context): String? {
        var pref = context.getSharedPreferences("Application", AppCompatActivity.MODE_PRIVATE)
        val url = pref.getString("api_url", null)
        return url
    }

    private fun getRetrofit(context: Context): Retrofit {
        val gson = GsonBuilder()
            .setLenient()
            .create()
        val okHttpClient = SelfSigningHelper(context).setSSLOkHttp(OkHttpClient.Builder())
        return Retrofit.Builder()
            .baseUrl(WEB_HOOK_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addConverterFactory(ScalarsConverterFactory.create())
            .client(okHttpClient.build())
            .build()
    }

    companion object {
        const val WEB_HOOK_BASE_URL = "https://hooks.slack.com/"
        const val BODY = "BODY"
        const val RESULT_OK = "ok"
        const val META_DATA_PATH = "slack_webhook_path"
    }
}