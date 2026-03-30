package org.ntqqrev.math

import org.ntqqrev.math.util.toDecString
import org.ntqqrev.math.util.toHexString
import kotlin.test.Test
import kotlin.test.assertEquals

class BigIntTest {
    @Test
    fun parsesDecimalAndHexStrings() {
        assertEquals(BigInt.ZERO, BigInt("0"))
        assertEquals(BigInt.ZERO, BigInt("0x0", 16))
        assertEquals("-42", BigInt("-00042").toDecString())
        assertEquals("42", BigInt("+00042").toDecString())
        assertEquals("0x2a", BigInt("0x2a", 16).toHexString())
        assertEquals("-0x2a", BigInt("-0x2a", 16).toHexString())
        assertEquals(
            "18446744073709551616",
            BigInt("18446744073709551616").toDecString(),
        )
        assertEquals(
            "0x10000000000000000",
            BigInt("0x10000000000000000", 16).toHexString(),
        )
    }

    @Test
    fun multipliesZeroAndSignedValues() {
        val value = BigInt(123456789)

        assertEquals(BigInt.ZERO, BigInt.ZERO * value)
        assertEquals(BigInt.ZERO, value * BigInt.ZERO)
        assertEquals("75bcd15", value.toHexString(withPrefix = false))
        assertEquals("15241578750190521", (value * value).toDecString())
        assertEquals("-15241578750190521", ((-value) * value).toDecString())
        assertEquals("-0x3626229738a3b9", ((-value) * value).toHexString())
        assertEquals("0x3626229738a3b9", ((-value) * (-value)).toHexString())
    }

    @Test
    fun multipliesValuesAcrossWordBoundaries() {
        val twoPow64 = (BigInt(Long.MAX_VALUE) + BigInt.ONE) * BigInt(2)
        val twoPow64PlusOne = twoPow64 + BigInt.ONE
        val maxWord = BigInt(Long.MAX_VALUE) * BigInt(2) + BigInt.ONE

        assertEquals(
            "0x100000000000000000000000000000000",
            (twoPow64 * twoPow64).toHexString()
        )
        assertEquals(
            "0x100000000000000020000000000000001",
            (twoPow64PlusOne * twoPow64PlusOne).toHexString(),
        )
        assertEquals(
            "340282366920938463426481119284349108225",
            (maxWord * maxWord).toDecString(),
        )
        assertEquals(
            "0xfffffffffffffffe0000000000000001",
            (maxWord * maxWord).toHexString(),
        )
    }

    @Test
    fun subtractsSameSignValuesWithCorrectSign() {
        assertEquals("-5", (BigInt(0) - BigInt(5)).toDecString())
        assertEquals("-2", (BigInt(3) - BigInt(5)).toDecString())
        assertEquals("2", (BigInt(-3) - BigInt(-5)).toDecString())
        assertEquals("-0x5", (BigInt(0) - BigInt(5)).toHexString())
        assertEquals("0x2", (BigInt(-3) - BigInt(-5)).toHexString())
    }

    @Test
    fun formatsZeroAndSignedValues() {
        assertEquals("0", BigInt(0).toDecString())
        assertEquals("0x0", BigInt(0).toHexString())
        assertEquals("-42", BigInt(-42).toDecString())
        assertEquals("-0x2a", BigInt(-42).toHexString())
    }

    @Test
    fun formatsValuesBeyondSingleWord() {
        val value = (BigInt(Long.MAX_VALUE) + BigInt.ONE) * BigInt(2)

        assertEquals("18446744073709551616", value.toDecString())
        assertEquals("0x10000000000000000", value.toHexString())
        assertEquals("-18446744073709551616", (-value).toDecString())
        assertEquals("-0x10000000000000000", (-value).toHexString())
    }
}
