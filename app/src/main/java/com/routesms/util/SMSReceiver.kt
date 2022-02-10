package com.routesms.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telephony.SmsMessage
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class SMSReceiver() : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val bundle = intent.extras

        val messages = parseSmsMessage(bundle)
        if (messages.size > 0) {

            val sender = messages[0]!!.originatingAddress
            Log.d(TAG, "sender: $sender")

            val contents = messages[0]!!.messageBody.toString()
            Log.d(TAG, "contents: $contents")

            val receivedDate = Date(messages[0]!!.timestampMillis)
            Log.d(TAG, "received date: $receivedDate")

            if (sender != null) {
                sendAPI(sender, receivedDate.toString(), contents, context)
            }
        }
    }

    private fun sendAPI(sender: String, date: String, content: String, context: Context) {
        var pref = context.getSharedPreferences("Application", AppCompatActivity.MODE_PRIVATE)
        var editor = pref.edit()
        var url: String? = pref.getString("api_url", null) ?: return

        var client = OkHttpClient()

        val body = FormBody.Builder()
                .add("content", content)
                .add("sender", sender)
                .add("date", date)
                .build() as RequestBody
        val request: Request = Request.Builder().url(url).post(body)
                .addHeader("Authorization", "Basic s")
                .addHeader("Content-Type", "application/json")
                .build()
        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call?, e: IOException?) {
                return
            }

            override fun onResponse(call: Call?, response: Response?) {
                return
            }
        })
    }

    // 정형화된 코드. 그냥 가져다 쓰면 된다.
    private fun parseSmsMessage(bundle: Bundle?): Array<SmsMessage?> {
        val objs = bundle!!["pdus"] as Array<Any>?
        val messages = arrayOfNulls<SmsMessage>(
            objs!!.size
        )
        for (i in objs.indices) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val format = bundle.getString("format")
                messages[i] = SmsMessage.createFromPdu(objs[i] as ByteArray, format)
            } else {
                messages[i] = SmsMessage.createFromPdu(objs[i] as ByteArray)
            }
        }
        return messages
    }

    companion object {
        // BroadcastReceiver를 상속한다!!
        private const val TAG = "SmsReceiver"
        private val format: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    }
}