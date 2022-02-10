package com.routesms.bind

import android.Manifest
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
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    var url: String? = null
    val MY_PERMISSION_ACCESS_ALL = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initComp()

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            var permissions = arrayOf(
                Manifest.permission.RECEIVE_SMS
            )
            ActivityCompat.requestPermissions(this, permissions, MY_PERMISSION_ACCESS_ALL)
        }
    }

    fun initComp() {
        var pref = getSharedPreferences("Application", MODE_PRIVATE)
        var editor = pref.edit()
        url = pref.getString("api_url", null)
        if (url != null) {
            mTxtUrl.setText(url)
        }

        bindButton.setOnClickListener {
            editor.putString("api_url", mTxtUrl.text.toString())
            editor.apply()
            Toast.makeText(this, "save!", LENGTH_LONG).show()
        }

        val filter = IntentFilter()
        filter.addAction("BroadcastReceive")
        registerReceiver(SMSReceiver(), filter)
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