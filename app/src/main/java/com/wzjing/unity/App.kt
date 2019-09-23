package com.wzjing.unity

import android.app.Application
import cn.jiguang.verifysdk.api.JVerificationInterface

class App: Application() {

    override fun onCreate() {
        super.onCreate()
        JVerificationInterface.init(baseContext)
        JVerificationInterface.setDebugMode(true)
    }

}