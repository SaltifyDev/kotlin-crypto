@file:OptIn(ExperimentalUnsignedTypes::class)

package org.ntqqrev.math.util

import org.ntqqrev.math.BigInt

private fun MutableList<ULong>.multiplyBy2Pow32AndAdd(addend: ULong) {
    var carry = addend
    for (i in indices) {
        val value = this[i] * (1UL shl 32) + carry
        this[i] = value % 1_000_000_000UL
        carry = value / 1_000_000_000UL
    }
    while (carry != 0UL) {
        add(carry % 1_000_000_000UL)
        carry /= 1_000_000_000UL
    }
}

/**
 * Converts a [BigInt] to a decimal (base-10) string.
 */
fun BigInt.toDecimalString(): String {
    if (words.size == 1 && words[0] == 0UL) {
        return "0"
    }

    val decimalWords = mutableListOf(0UL)
    for (i in words.lastIndex downTo 0) {
        val word = words[i]
        decimalWords.multiplyBy2Pow32AndAdd((word shr 32).toUInt().toULong())
        decimalWords.multiplyBy2Pow32AndAdd((word and 0xffffffffUL).toUInt().toULong())
    }

    return buildString {
        if (isNegative) {
            append('-')
        }
        append(decimalWords.last())
        for (i in decimalWords.size - 2 downTo 0) {
            append(decimalWords[i].toString().padStart(9, '0'))
        }
    }
}

/**
 * Converts a [BigInt] to a decimal (base-10) string.
 *
 * This is an alias of [toDecimalString].
 */
fun BigInt.toDecString() = toDecimalString()

/**
 * Converts a [BigInt] to a hexadecimal (base-16) string.
 * @param withPrefix Whether to include a `0x`- prefix in the result string.
 */
fun BigInt.toHexString(withPrefix: Boolean = true): String {
    return buildString {
        if (isNegative) {
            append('-')
        }
        if (withPrefix) {
            append("0x")
        }
        append(words.last().toString(16))
        for (i in words.size - 2 downTo 0) {
            append(words[i].toString(16).padStart(16, '0'))
        }
    }
}