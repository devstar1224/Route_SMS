package com.routesms.bind

import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.routesms.R
import com.routesms.util.SMSReceiver
import com.routesms.util.ServiceDaemon
import com.routesms.util.slack.SlackWebHook
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    var url: String? = null
    val MY_PERMISSION_ACCESS_ALL = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initComp()

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECEIVE_MMS) != PackageManager.PERMISSION_GRANTED||
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            var permissions = arrayOf(
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.RECEIVE_MMS,
                Manifest.permission.READ_SMS
            )
            ActivityCompat.requestPermissions(this, permissions, MY_PERMISSION_ACCESS_ALL)
        }
    }

    fun regBroadcast() {
        val filter = IntentFilter()
        filter.addAction("BroadcastReceive")
        filter.addAction("android.provider.Telephony.WAP_PUSH_RECEIVED")
        filter.addDataType("application/vnd.wap.mms-message")
        registerReceiver(SMSReceiver(), filter)
    }

    fun initComp() {
        startService()
        var pref = getSharedPreferences("Application", MODE_PRIVATE)
        var editor = pref.edit()
        url = pref.getString("api_url", null)
        if (url != null) {
            mTxtUrl.setText(url)
        }

        bindButton.setOnClickListener {
            editor.putString("api_url", mTxtUrl.text.toString())
            editor.apply()
            Toast.makeText(this, "저장완료!", LENGTH_LONG).show()
        }

        testButton.setOnClickListener {
            SlackWebHook.builder()
                .title("수신테스트")
                .timeStampEnabled(true)
                .color("#FF0000")
                .fields("발신" to "0123456789", "내용" to "수신 테스트", "수신시각" to "Fri Feb 11 06:03:52 GMT 2022")
                .build()
                .send(this)
            Toast.makeText(this, "테스트 전송!", LENGTH_LONG).show()
        }
    }

    private fun startService() {
        val intent: Intent = Intent(this, ServiceDaemon::class.java)
        startService(intent)
    }

    override fun onRequestPermissionsResult( requestCode: Int, permissions: Array<out String>, grantResults: IntArray ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode === MY_PERMISSION_ACCESS_ALL) {
            if (grantResults.size > 0) {
                for (grant in grantResults) {
                    if (grant != PackageManager.PERMISSION_GRANTED)
                        System.exit(0)
                }
            }
        }
    }
}