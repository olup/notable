package com.olup.notable

import android.app.Application
import com.onyx.android.sdk.rx.RxManager
import org.lsposed.hiddenapibypass.HiddenApiBypass

class NotableApp : Application() {

    override fun onCreate() {
        super.onCreate()
        RxManager.Builder.initAppContext(this)
        checkHiddenApiBypass()
    }

    private fun checkHiddenApiBypass() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            HiddenApiBypass.addHiddenApiExemptions("")
        }
    }

}