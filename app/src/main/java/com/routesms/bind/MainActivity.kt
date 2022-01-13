package com.routesms.bind

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.appcompat.app.AppCompatActivity
import com.routesms.R
import com.routesms.util.SMSReceiver
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    var url: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initComp()
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
}