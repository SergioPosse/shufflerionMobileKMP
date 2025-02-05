package com.example.shufflerionmultiplatform

import kotlin.test.Test
import kotlin.test.assertTrue

class CommonGreetingTest {

    @Test
    fun testExample() {
        assertTrue(MainScreen().greet().contains("Hello"), "Check 'Hello' is mentioned")
    }
}