package org.mapdb.elsa

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.*

/**
 * Test utilities
 */
object TT{


    fun bytesToHex(input: ByteArray): String {
        val builder = StringBuilder()
        for (b in input) {
            builder.append(String.format("%02x", b))
        }
        return builder.toString()
    }

    fun hexToBytes(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    fun  toHex(ser: ElsaSerializer, obj: Any):String {
        val out = ByteArrayOutputStream()
        val out2 = DataOutputStream(out)
        ser.serialize(out2, obj)

        return bytesToHex(out.toByteArray())
    }

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

    fun  hexDeser(ser: ElsaSerializer, value: String): Any {
        val bytes = hexToBytes(value)
        val ins = DataInputStream(ByteArrayInputStream(bytes))
        return ser.deserialize(ins)
    }

}
