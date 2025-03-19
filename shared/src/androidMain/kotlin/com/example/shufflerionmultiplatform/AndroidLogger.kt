package com.example.shufflerionmultiplatform

import com.newrelic.agent.android.NewRelic

class AndroidLogger : Logger {
    override fun log(message: String) {
        NewRelic.logInfo(message)
    }

    override fun logError(error: String) {
        NewRelic.logError(error)
    }

    override fun logWarning(warning: String) {
        NewRelic.logWarning(warning)
    }
}
