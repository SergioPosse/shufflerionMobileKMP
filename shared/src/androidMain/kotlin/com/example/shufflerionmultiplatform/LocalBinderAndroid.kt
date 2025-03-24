package com.example.shufflerionmultiplatform

import android.os.Binder

class LocalBinderAndroid<T>(private val service: T) : Binder() {
    fun getService(): T = service
}