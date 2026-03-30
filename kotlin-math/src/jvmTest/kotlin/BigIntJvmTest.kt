package org.ntqqrev.math

import org.ntqqrev.math.util.toHexString
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals

class BigIntJvmTest {
    @Test
    fun multiplicationMatchesBigIntegerForDecimalInputs() {
        val cases = listOf(
            "0" to "123456789",
            "123456789" to "987654321",
            "-123456789" to "987654321",
            "18446744073709551616" to "18446744073709551616",
            "-18446744073709551617" to "36893488147419103231",
            "340282366920938463463374607431768211455" to "4294967297",
        )

        for ((left, right) in cases) {
            val expected = BigInteger(left).multiply(BigInteger(right)).toString(16)
            val actual = (
                BigInt(left) * BigInt(right)
            ).toHexString(withPrefix = false)
            assertEquals(expected, actual, "$left * $right")
        }
    }

    @Test
    fun multiplicationMatchesBigIntegerForHexInputs() {
        val cases = listOf(
            "0x0" to "0x123456789abcdef",
            "0x75bcd15" to "0x3ade68b1",
            "-0x75bcd15" to "0x3ade68b1",
            "0x10000000000000000" to "0x10000000000000000",
            "0x10000000000000001" to "0xffffffffffffffff",
            "-0xfedcba98765432100123456789abcdef" to "0x123456789abcdef0fedcba9876543211",
        )

        for ((left, right) in cases) {
            val expected = parseBigIntegerHex(left).multiply(parseBigIntegerHex(right)).toString(16)
            val actual = (
                BigInt(left, 16) * BigInt(right, 16)
            ).toHexString(withPrefix = false)
            assertEquals(expected, actual, "$left * $right")
        }
    }

    private fun parseBigIntegerHex(value: String): BigInteger {
        var body = value.trim()
        var negative = false

        if (body.startsWith('-') || body.startsWith('+')) {
            negative = body[0] == '-'
            body = body.substring(1)
        }
        if (body.startsWith("0x", ignoreCase = true)) {
            body = body.substring(2)
        }
        if (body.startsWith('-') || body.startsWith('+')) {
            negative = body[0] == '-'
            body = body.substring(1)
        }

        val result = BigInteger(body, 16)
        return if (negative) result.negate() else result
    }
}