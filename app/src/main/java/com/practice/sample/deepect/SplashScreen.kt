package com.practice.sample.deepect

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.core.app.ActivityCompat

class SplashScreen : Activity() {

    private final val REQUEST_USED_PERMISSION = 1

    private final val needPermissions  = arrayOf (
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_layout)

        requestPermission()

    }

    private fun requestPermission () {
        for (permission in needPermissions) {
            if(ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, needPermissions, REQUEST_USED_PERMISSION)
            }
            else
                skipSplashScreen()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)


        for (result in grantResults) {
            if(result != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "권한을 허용해주세요",Toast.LENGTH_LONG).show()
                requestPermission()

                return
            }
        }

        skipSplashScreen()
    }



    private fun skipSplashScreen() {
        val handler = Handler()
        handler.postDelayed(object : Runnable{
            override fun run() {
                val intent = Intent(this@SplashScreen, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }, 2000)

    }
}