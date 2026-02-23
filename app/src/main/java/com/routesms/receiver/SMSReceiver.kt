package com.routesms.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telephony.SmsMessage
import android.text.TextUtils
import android.util.Log
import com.routesms.data.DeduplicationCache
import com.routesms.data.ForwardedMessage
import com.routesms.data.MessageLog
import com.routesms.slack.SlackWebHook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.text.MessageFormat
import java.time.LocalDateTime
import java.util.Calendar

class SMSReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SMSReceiver"
        private const val MMS_RECEIVED = "android.provider.Telephony.WAP_PUSH_RECEIVED"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val bundle = intent.extras

        if (intent.action == MMS_RECEIVED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    delay(1500)
                    parseMMS(context)
                } catch (e: Exception) {
                    Log.e(TAG, "MMS parsing error", e)
                } finally {
                    pendingResult.finish()
                }
            }
        } else {
            val messages = parseSmsMessage(bundle)
            if (messages.isNotEmpty()) {
                val msg = messages[0] ?: return
                val sender = msg.originatingAddress ?: return
                val contents = buildString {
                    messages.forEach { sms ->
                        sms?.messageBody?.let { append(it) }
                    }
                }
                val receivedDate = java.util.Date(msg.timestampMillis)
                sendAPI(sender, receivedDate.toString(), contents, context, "SMS")
            }
        }
    }

    private fun sendAPI(sender: String, date: String, content: String, context: Context, source: String) {
        if (DeduplicationCache.isDuplicate(sender, content)) {
            Log.d(TAG, "Duplicate $source skipped: $sender")
            return
        }
        DeduplicationCache.register(sender, content)

        val color = if (source == "MMS") "#00AA00" else "#0000FF"
        SlackWebHook.builder()
            .title("$source 수신")
            .timeStampEnabled(true)
            .color(color)
            .fields("발신" to sender, "내용" to content, "수신시각" to date)
            .build()
            .send(context)

        MessageLog.addMessage(
            ForwardedMessage(
                source = source,
                sender = sender,
                content = content
            )
        )
    }

    private fun parseSmsMessage(bundle: Bundle?): Array<SmsMessage?> {
        val objs = bundle?.get("pdus") as? Array<*> ?: return emptyArray()
        return Array(objs.size) { i ->
            val pdu = objs[i] as? ByteArray ?: return@Array null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val format = bundle.getString("format")
                SmsMessage.createFromPdu(pdu, format)
            } else {
                @Suppress("DEPRECATION")
                SmsMessage.createFromPdu(pdu)
            }
        }
    }

    private fun parseMMS(context: Context) {
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(
            Uri.parse("content://mms"),
            arrayOf("_id", "date"),
            null,
            null,
            "date DESC limit 1"
        ) ?: return

        cursor.use {
            if (!it.moveToFirst()) return
            val idIndex = it.getColumnIndexOrThrow("_id")
            val id = it.getString(idIndex)

            val number = parseNumber(context, id)
            val msg = parseMessage(context, id)

            if (number != null && msg != null) {
                val dateStr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    LocalDateTime.now().toString()
                } else {
                    Calendar.getInstance().time.toString()
                }
                sendAPI(number, dateStr, msg, context, "MMS")
            }
        }
    }

    private fun parseNumber(context: Context, id: String): String? {
        val uri = Uri.parse(MessageFormat.format("content://mms/{0}/addr", id))
        val cursor = context.contentResolver.query(
            uri,
            arrayOf("address"),
            "msg_id = ? and type = 137",
            arrayOf(id),
            "_id asc limit 1"
        ) ?: return null

        cursor.use {
            if (!it.moveToFirst()) return null
            val addressIndex = it.getColumnIndexOrThrow("address")
            return it.getString(addressIndex)
        }
    }

    private fun parseMessage(context: Context, id: String): String? {
        val cursor = context.contentResolver.query(
            Uri.parse("content://mms/part"),
            arrayOf("mid", "_id", "ct", "_data", "text"),
            null,
            null,
            null
        ) ?: return null

        cursor.use {
            if (!it.moveToFirst()) return null

            val midIndex = it.getColumnIndexOrThrow("mid")
            val idIndex = it.getColumnIndexOrThrow("_id")
            val ctIndex = it.getColumnIndexOrThrow("ct")
            val dataIndex = it.getColumnIndexOrThrow("_data")
            val textIndex = it.getColumnIndexOrThrow("text")

            while (!it.isAfterLast) {
                val mid = it.getString(midIndex)
                if (id == mid) {
                    val partId = it.getString(idIndex)
                    val type = it.getString(ctIndex)
                    if ("text/plain" == type) {
                        val data = it.getString(dataIndex)
                        return if (TextUtils.isEmpty(data)) {
                            it.getString(textIndex)
                        } else {
                            parseMessageWithPartId(context, partId)
                        }
                    }
                }
                it.moveToNext()
            }
        }
        return null
    }

    private fun parseMessageWithPartId(context: Context, id: String): String? {
        val partURI = Uri.parse("content://mms/part/$id")
        var inputStream: java.io.InputStream? = null
        val sb = StringBuilder()
        try {
            inputStream = context.contentResolver.openInputStream(partURI)
            if (inputStream != null) {
                val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
                var temp = reader.readLine()
                while (!TextUtils.isEmpty(temp)) {
                    sb.append(temp)
                    temp = reader.readLine()
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "MMS part read error", e)
        } finally {
            try {
                inputStream?.close()
            } catch (_: IOException) {
            }
        }
        return sb.toString()
    }
}
