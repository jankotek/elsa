package org.mapdb.elsa

import java.util.*

/**
 * Test utilities
 */
object TT{

    @JvmStatic fun randomString(size: Int=1+Random().nextInt(32), seed: Int= Random().nextInt()): String {
        val chars = "0123456789abcdefghijklmnopqrstuvwxyz !@#$%^&*()_+=-{}[]:\",./<>?|\\".toCharArray()
        var seed = seed
        val b = StringBuilder(size)
        for (i in 0..size - 1) {
            b.append(chars[Math.abs(seed% chars.size)])
            seed = 31 * seed + seed
        }
        return b.toString()
    }
}
