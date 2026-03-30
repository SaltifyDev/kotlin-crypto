@file:OptIn(ExperimentalUnsignedTypes::class)

package org.ntqqrev.math

import org.ntqqrev.math.util.toDecimalString
import kotlin.math.max

class BigInt {
    /**
     * Little-endian storage of words.
     */
    internal val words: ULongArray

    /**
     * Whether this [BigInt] is negative.
     */
    val isNegative: Boolean

    constructor() {
        words = ulongArrayOf(0UL)
        isNegative = false
    }

    private constructor(words: ULongArray, isNegative: Boolean) {
        this.words = words
        this.isNegative = isNegative
    }

    constructor(value: Long) {
        isNegative = value < 0
        words = ulongArrayOf(
            if (isNegative) (-value).toULong() else value.toULong()
        )
    }

    constructor(value: Int) : this(value.toLong())

    operator fun plus(other: BigInt): BigInt {
        if (this.isNegative == other.isNegative) {
            return BigInt(
                words = addWords(this.words, other.words),
                isNegative = this.isNegative,
            )
        } else {
            val cmp = compareWords(this.words, other.words)
            return when {
                cmp > 0 -> BigInt(
                    words = subtractWords(this.words, other.words),
                    isNegative = this.isNegative,
                )

                cmp < 0 -> BigInt(
                    words = subtractWords(other.words, words),
                    isNegative = other.isNegative,
                )

                else -> ZERO
            }
        }
    }

    operator fun minus(other: BigInt): BigInt {
        if (this.isNegative != other.isNegative) {
            return BigInt(
                words = addWords(this.words, other.words),
                isNegative = this.isNegative,
            )
        } else {
            val cmp = compareWords(this.words, other.words)
            return when {
                cmp > 0 -> BigInt(
                    words = subtractWords(this.words, other.words),
                    isNegative = this.isNegative,
                )

                cmp < 0 -> BigInt(
                    words = subtractWords(other.words, words),
                    isNegative = !this.isNegative,
                )

                else -> ZERO
            }
        }
    }

    operator fun unaryMinus(): BigInt = ZERO.minus(this)

    operator fun times(other: BigInt): BigInt {
        if (this == ZERO || other == ZERO) {
            return ZERO
        }
        return BigInt(
            words = multiplyWords(this.words, other.words),
            isNegative = this.isNegative xor other.isNegative,
        )
    }

    override fun toString(): String = this.toDecimalString()

    override operator fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BigInt) return false

        if (isNegative != other.isNegative) return false
        if (words.size != other.words.size) return false
        for (i in words.indices) {
            if (words[i] != other.words[i]) return false
        }
        return true
    }

    override fun hashCode(): Int {
        TODO()
    }

    companion object {
        val ZERO = BigInt(0)
        val ONE = BigInt(1)
        val TEN = BigInt(10)

        private fun ULongArray.trailingZeroWordCount(): Int {
            for (i in indices) {
                if (this[size - i - 1] != 0UL) {
                    return i
                }
            }
            return size
        }

        /**
         * Get a copy of the array with trailing zeros removed,
         * unless the only one element of the array is `0UL`.
         * If there is nothing to normalize, the method returns the array itself.
         */
        private fun ULongArray.normalized(): ULongArray {
            val sizeToPreserve: Int = max(1, size - trailingZeroWordCount())
            if (sizeToPreserve == size) return this
            return this.copyOf(sizeToPreserve)
        }

        private fun addWords(a: ULongArray, b: ULongArray): ULongArray {
            val n = max(a.size, b.size)
            val dest = ULongArray(n + 1)
            var carry = 0UL
            for (i in 0 until n) {
                val ai = if (i < a.size) a[i] else 0UL
                val bi = if (i < b.size) b[i] else 0UL

                val sum = ai + bi
                val sumWithCarry = sum + carry
                dest[i] = sumWithCarry

                val carryInPlace = sum < ai
                val carryFromAbove = sumWithCarry < sum
                carry = if (carryInPlace || carryFromAbove) 1UL else 0UL
            }
            return if (carry == 0UL) {
                dest.copyOf(max(1, n))
            } else {
                dest[n] = carry
                dest
            }
        }

        private fun subtractWords(a: ULongArray, b: ULongArray): ULongArray {
            val n = a.size
            val dest = ULongArray(n)
            var borrow = 0UL
            for (i in 0 until n) {
                val ai = a[i]
                val bi = if (i < b.size) b[i] else 0UL

                val diff = ai - bi
                val diffWithBorrow = diff - borrow
                dest[i] = diffWithBorrow

                val borrowInPlace = ai < bi
                val borrowFromAbove = diff < borrow
                borrow = if (borrowInPlace || borrowFromAbove) 1UL else 0UL
            }
            var size = dest.size
            while (size > 1 && dest[size - 1] == 0UL) {
                size--
            }
            return dest.copyOf(size)
        }

        private fun multiplyWords(a: ULongArray, b: ULongArray): ULongArray {
            val n = a.size + b.size
            val dest = ULongArray(n)
            for (i in a.indices) {
                var carry = 0UL
                for (j in b.indices) {
                    val k = i + j
                    val productInPlace = DWord.fromProduct(a[i], b[j])
                    val sumInPlace = DWord.fromSum(dest[k], productInPlace.low)
                    val sumWithCarry = DWord.fromSum(sumInPlace.low, carry)
                    dest[k] = sumWithCarry.low

                    val newCarry = DWord.fromSum(
                        productInPlace.high,
                        sumInPlace.high,
                        sumWithCarry.high,
                    ) // may exceed 2^64-1! must be presented in DWord
                    carry = newCarry.low
                    if (newCarry.high != 0UL) {
                        dest.addWordAt(k + 2, newCarry.high)
                    }
                }
                dest.addWordAt(i + b.size, carry)
            }
            return dest.normalized()
        }

        private fun ULongArray.addWordAt(index: Int, value: ULong) {
            var i = index
            var carry = value
            while (carry != 0uL && i < size) {
                val sum = DWord.fromSum(this[i], carry)
                this[i] = sum.low
                carry = sum.high
                i++
            }
        }

        private fun compareWords(a: ULongArray, b: ULongArray): Int {
            if (a.size != b.size) {
                return if (a.size < b.size) -1 else 1
            }
            for (i in a.size - 1 downTo 0) {
                when {
                    a[i] < b[i] -> return -1
                    a[i] > b[i] -> return 1
                }
            }
            return 0
        }
    }
}