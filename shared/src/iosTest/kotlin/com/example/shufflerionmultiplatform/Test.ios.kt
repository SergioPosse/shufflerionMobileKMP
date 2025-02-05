package com.example.shufflerionmultiplatform

import kotlin.test.Test
import kotlin.test.assertTrue

class IosGreetingTest {

    @Test
    fun testExample() {
        assertTrue(MainScreen().greet().contains("iOS"), "Check iOS is mentioned")
    }
}