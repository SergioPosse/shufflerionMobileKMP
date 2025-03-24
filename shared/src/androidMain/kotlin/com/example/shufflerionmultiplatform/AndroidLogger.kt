package com.example.shufflerionmultiplatform

import com.newrelic.agent.android.NewRelic

class AndroidLogger : Logger {
    override fun log(message: String) {
        NewRelic.logInfo("shufflerionApp: $message")
    }

    override fun logError(error: String) {
        NewRelic.logError("shufflerionApp: $error")
    }

    override fun logWarning(warning: String) {
        NewRelic.logWarning("shufflerionApp: $warning")
    }
}
