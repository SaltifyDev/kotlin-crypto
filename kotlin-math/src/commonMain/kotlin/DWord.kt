package org.ntqqrev.math

internal data class DWord(
    val high: ULong,
    val low: ULong,
) {
    companion object {
        fun fromProduct(a: ULong, b: ULong): DWord {
            val mask = 0xffffffffuL
            val x0 = a and mask
            val x1 = a shr 32
            val y0 = b and mask
            val y1 = b shr 32
            val p00 = x0 * y0
            val p01 = x0 * y1
            val p10 = x1 * y0
            val p11 = x1 * y1
            val mid = (p00 shr 32) + (p01 and mask) + (p10 and mask)
            val low = (p00 and mask) or (mid shl 32)
            val high = p11 + (p01 shr 32) + (p10 shr 32) + (mid shr 32)
            return DWord(high, low)
        }

        fun fromSum(a: ULong, b: ULong): DWord {
            val sum = a + b
            val carry = if (sum < a) 1uL else 0uL
            return DWord(carry, sum)
        }

        fun fromSum(a: ULong, b: ULong, c: ULong): DWord {
            val t1 = fromSum(a, b)
            val t2 = fromSum(t1.low, c)
            val carry = t1.high + t2.high
            return DWord(carry, t2.low)
        }
    }
}