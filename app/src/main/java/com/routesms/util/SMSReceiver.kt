package com.routesms.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telephony.SmsMessage
import com.routesms.util.slack.SlackWebHook
import java.text.SimpleDateFormat
import java.util.*


class SMSReceiver() : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val bundle = intent.extras

        val messages = parseSmsMessage(bundle)
        if (messages.size > 0) {
            val sender = messages[0]!!.originatingAddress
            val contents = messages[0]!!.messageBody.toString()
            val receivedDate = Date(messages[0]!!.timestampMillis)
            if (sender != null) {
                sendAPI(sender, receivedDate.toString(), contents, context)
            }
        }
    }

    private fun sendAPI(sender: String, date: String, content: String, context: Context) {
        SlackWebHook.builder()
            .title("SMS 수신")
            .timeStampEnabled(true)
            .color("#0000FF")
            .fields("발신" to sender, "내용" to content, "수신시각" to date)
            .build()
            .send(context)
    }

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
        private const val TAG = "SmsReceiver"
        private val format: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    }
}