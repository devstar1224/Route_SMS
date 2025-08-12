package com.routesms.util

import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.SmsMessage
import android.text.TextUtils
import android.util.Log
import com.routesms.util.slack.SlackWebHook
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.text.MessageFormat
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.*
import kotlin.concurrent.thread


class SMSReceiver() : BroadcastReceiver() {

    val MMS_RECEIVED = "android.provider.Telephony.WAP_PUSH_RECEIVED"

    private lateinit var _context: Context
    override fun onReceive(context: Context, intent: Intent) {
        this._context = context
        val bundle = intent.extras
        if (intent.getAction().equals(MMS_RECEIVED)){
            try {
                Handler(Looper.getMainLooper()).postDelayed({
                    parseMMS()
                }, 1500)
            } catch (e: Exception) {
                Log.e("error", e.toString())
            }
        } else {
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
    private fun parseMMS() {
        val contentResolver: ContentResolver = _context.getContentResolver()
        val projection = arrayOf("_id")
        val uri: Uri = Uri.parse("content://mms")
        val cursor: Cursor = contentResolver.query(uri,     arrayOf("_id", "date"),
            null,
            null,
            "date DESC limit 1")!!
        if (cursor.getCount() === 0) {
            cursor.close()
            return
        }
        cursor.moveToFirst()
        val id: String = cursor.getString(cursor.getColumnIndex("_id"))
        cursor.close()
        val number = parseNumber(id)
        val msg = parseMessage(id)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            sendAPI(number.toString(), LocalDateTime.now().toString(), msg.toString(), _context)
        } else {
            sendAPI(number.toString(), Calendar.getInstance().time.toString(), msg.toString(), _context)
        }
    }

    private fun parseNumber(`$id`: String): String? {
        var result: String? = null
        val uri: Uri = Uri.parse(MessageFormat.format("content://mms/{0}/addr", `$id`))
        val projection = arrayOf("address")
        val selection = "msg_id = ? and type = 137" // type=137은 발신자
        val selectionArgs = arrayOf(`$id`)
        val cursor: Cursor = _context.getContentResolver()
            .query(uri, projection, selection, selectionArgs, "_id asc limit 1")!!
        if (cursor.getCount() === 0) {
            cursor.close()
            return result
        }
        cursor.moveToFirst()
        result = cursor.getString(cursor.getColumnIndex("address"))
        cursor.close()
        return result
    }

    private fun parseMessage(`$id`: String): String? {
        var result: String? = null

        // 조회에 조건을 넣게되면 가장 마지막 한두개의 mms를 가져오지 않는다.
        val cursor: Cursor = _context.getContentResolver().query(
            Uri.parse("content://mms/part"),
            arrayOf("mid", "_id", "ct", "_data", "text"),
            null,
            null,
            null
        )!!
        Log.i(
            "MMSReceiver.java | parseMessage",
            "|mms 메시지 갯수 : " + cursor.getCount().toString() + "|"
        )
        if (cursor.getCount() === 0) {
            cursor.close()
            return result
        }
        cursor.moveToFirst()
        while (!cursor.isAfterLast()) {
            val mid: String = cursor.getString(cursor.getColumnIndex("mid"))
            if (`$id` == mid) {
                val partId: String = cursor.getString(cursor.getColumnIndex("_id"))
                val type: String = cursor.getString(cursor.getColumnIndex("ct"))
                if ("text/plain" == type) {
                    val data: String? = cursor.getString(cursor.getColumnIndex("_data"))
                    result =
                        if (TextUtils.isEmpty(data)) cursor.getString(cursor.getColumnIndex("text")) else parseMessageWithPartId(
                            partId
                        )
                }
            }
            cursor.moveToNext()
        }
        cursor.close()
        return result
    }


    private fun parseMessageWithPartId(`$id`: String): String? {
        val partURI: Uri = Uri.parse("content://mms/part/$`$id`")
        var `is`: InputStream? = null
        val sb = StringBuilder()
        try {
            `is` = _context.getContentResolver().openInputStream(partURI)
            if (`is` != null) {
                val isr = InputStreamReader(`is`, "UTF-8")
                val reader = BufferedReader(isr)
                var temp: String = reader.readLine()
                while (!TextUtils.isEmpty(temp)) {
                    sb.append(temp)
                    temp = reader.readLine()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (`is` != null) {
                try {
                    `is`.close()
                } catch (e: IOException) {
                }
            }
        }
        return sb.toString()
    }
    companion object {
        private const val TAG = "SmsReceiver"
        private val format: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    }
}