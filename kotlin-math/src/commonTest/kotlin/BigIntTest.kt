package org.ntqqrev.math

import org.ntqqrev.math.util.toDecString
import org.ntqqrev.math.util.toHexString
import kotlin.test.Test
import kotlin.test.assertEquals

class BigIntTest {
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
