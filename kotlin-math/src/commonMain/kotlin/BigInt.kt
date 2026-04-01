@file:OptIn(ExperimentalUnsignedTypes::class)

package org.ntqqrev.math

import org.ntqqrev.math.util.toDecimalString
import kotlin.math.max

class BigInt : Comparable<BigInt> {
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

    constructor(words: ULongArray, isNegative: Boolean) {
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

    constructor(value: ByteArray, endian: Endian = Endian.BE) {
        words = value.toWords(endian)
        isNegative = false
    }

    constructor(value: String, radix: Int) {
        when (radix) {
            10 -> {
                val (isNegative, digits) = parseSignedDigits(value)
                require(digits.all { it in '0'..'9' }) {
                    "Invalid decimal string: $value"
                }
                var words = ulongArrayOf(0UL)
                var index = 0
                while (index < digits.length) {
                    val chunkEnd = minOf(index + 9, digits.length)
                    val chunk = digits.substring(index, chunkEnd)
                    words = words.multiply(DECIMAL_CHUNK_POWERS[chunk.length])
                    words = words.add(chunk.toUInt())
                    index = chunkEnd
                }
                this.words = words
                this.isNegative = isNegative && !(words.size == 1 && words[0] == 0UL)
            }

            16 -> {
                val (isNegative, digits) = parseSignedDigits(value, allowHexPrefix = true)
                require(digits.all { it.digitToIntOrNull(16) != null }) {
                    "Invalid hexadecimal string: $value"
                }

                val words = ULongArray((digits.length + 15) / 16)
                var wordIndex = 0
                var end = digits.length
                while (end > 0) {
                    val start = max(0, end - 16)
                    words[wordIndex++] = run {
                        var value = 0UL
                        for (ch in digits.substring(start, end)) {
                            value = (value shl 4) or ch.digitToInt(16).toULong()
                        }
                        value
                    }
                    end = start
                }
                this.words = words
                this.isNegative = isNegative && !(words.size == 1 && words[0] == 0UL)
            }

            else -> throw IllegalArgumentException("Unsupported radix: $radix")
        }
    }

    constructor(value: String) : this(value, 10)

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

    override operator fun compareTo(other: BigInt): Int {
        if (isNegative != other.isNegative) {
            return if (isNegative) -1 else 1
        }

        val magnitudeComparison = compareWords(words, other.words)
        return if (isNegative) -magnitudeComparison else magnitudeComparison
    }

    fun abs(): BigInt = if (isNegative) {
        BigInt(words, false)
    } else {
        this
    }

    fun bitLength(): Int {
        if (this == ZERO) {
            return 0
        }
        val magnitude = if (isNegative) {
            subtractWords(words, ONE.words)
        } else {
            words
        }
        return magnitude.bitLength()
    }

    fun testBit(n: Int): Boolean {
        require(n >= 0) {
            "Bit index must not be negative: $n"
        }
        return if (isNegative) {
            !subtractWords(words, ONE.words).testBitAt(n)
        } else {
            words.testBitAt(n)
        }
    }

    infix fun shl(bitCount: Int): BigInt {
        require(bitCount >= 0) {
            "Shift count must not be negative: $bitCount"
        }
        if (bitCount == 0 || this == ZERO) {
            return this
        }
        return BigInt(words.shiftWordsLeft(bitCount), isNegative)
    }

    infix fun shr(bitCount: Int): BigInt {
        require(bitCount >= 0) {
            "Shift count must not be negative: $bitCount"
        }
        if (bitCount == 0 || this == ZERO) {
            return this
        }
        if (!isNegative) {
            return BigInt(words.shiftWordsRight(bitCount), false)
        }

        val shiftedMagnitude = subtractWords(words, ONE.words).shiftWordsRight(bitCount)
        return -(BigInt(shiftedMagnitude, false) + ONE)
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
        var result = if (isNegative) 1 else 0
        for (word in words) {
            result = 31 * result + (word xor (word shr 32)).toInt()
        }
        return result
    }

    companion object {
        val ZERO = BigInt(0)
        val ONE = BigInt(1)
        val TEN = BigInt(10)

        private fun ULongArray.bitLength(): Int {
            if (size == 1 && this[0] == 0UL) {
                return 0
            }
            val mostSignificantWord = this[size - 1]
            return (size - 1) * 64 + (64 - mostSignificantWord.countLeadingZeroBits())
        }

        private fun ULongArray.testBitAt(index: Int): Boolean {
            val wordIndex = index / 64
            if (wordIndex >= size) {
                return false
            }
            val bitIndex = index % 64
            return ((this[wordIndex] shr bitIndex) and 1UL) != 0UL
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
            return if (dest[n - 1] == 0UL) {
                dest.copyOf(n - 1)
            } else {
                dest
            }
        }

        private fun ULongArray.shiftWordsLeft(bitCount: Int): ULongArray {
            val wordShift = bitCount / 64
            val bitShift = bitCount % 64
            val dest = ULongArray(size + wordShift + if (bitShift == 0) 0 else 1)

            if (bitShift == 0) {
                for (i in indices) {
                    dest[i + wordShift] = this[i]
                }
                return dest
            }

            var carry = 0UL
            for (i in indices) {
                val word = this[i]
                dest[i + wordShift] = (word shl bitShift) or carry
                carry = word shr (64 - bitShift)
            }
            dest[size + wordShift] = carry
            return if (carry == 0UL) {
                dest.copyOf(size + wordShift)
            } else {
                dest
            }
        }

        private fun ULongArray.shiftWordsRight(bitCount: Int): ULongArray {
            val wordShift = bitCount / 64
            if (wordShift >= size) {
                return ulongArrayOf(0UL)
            }

            val bitShift = bitCount % 64
            val newSize = size - wordShift
            if (bitShift == 0) {
                return copyOfRange(wordShift, size)
            }

            val dest = ULongArray(newSize)
            var carry = 0UL
            for (i in newSize - 1 downTo 0) {
                val word = this[i + wordShift]
                dest[i] = (word shr bitShift) or carry
                carry = word shl (64 - bitShift)
            }
            return when {
                dest[newSize - 1] != 0UL -> dest
                newSize == 1 -> ulongArrayOf(0UL)
                else -> dest.copyOf(newSize - 1)
            }
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

        private fun ULongArray.add(addend: UInt): ULongArray {
            if (addend == 0u) {
                return this
            }

            val dest = copyOf(size + 1)
            var carry = addend.toULong()
            var index = 0
            while (carry != 0UL && index < dest.size) {
                val sum = DWord.fromSum(dest[index], carry)
                dest[index] = sum.low
                carry = sum.high
                index++
            }
            return if (dest[size] == 0UL) {
                dest.copyOf(size)
            } else {
                dest
            }
        }

        private fun ULongArray.multiply(multiplier: UInt): ULongArray {
            if (multiplier == 0u || size == 1 && this[0] == 0UL) {
                return ulongArrayOf(0UL)
            }
            val dest = ULongArray(size + 1)
            val multiplierULong = multiplier.toULong()
            var carry = 0UL
            for (i in indices) {
                val product = DWord.fromProduct(this[i], multiplierULong)
                val sum = DWord.fromSum(product.low, carry)
                dest[i] = sum.low
                carry = product.high + sum.high
            }
            dest[size] = carry
            return if (carry == 0UL) {
                dest.copyOf(size)
            } else {
                dest
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


        private val DECIMAL_CHUNK_POWERS = uintArrayOf(
            1u,
            10u,
            100u,
            1_000u,
            10_000u,
            100_000u,
            1_000_000u,
            10_000_000u,
            100_000_000u,
            1_000_000_000u,
        )

        private fun parseSignedDigits(
            value: String,
            allowHexPrefix: Boolean = false,
        ): Pair<Boolean, String> {
            var body = value.trim()
            require(body.isNotEmpty()) {
                "Value must not be blank"
            }

            var sign: Char? = null

            fun consumeSign() {
                require(sign == null) {
                    "Duplicate sign in: $value"
                }
                sign = body[0]
                body = body.substring(1)
                require(body.isNotEmpty()) {
                    "Missing digits in: $value"
                }
            }

            fun consumeHexPrefix() {
                body = body.substring(2)
                require(body.isNotEmpty()) {
                    "Missing digits in: $value"
                }
            }

            if (body[0] == '+' || body[0] == '-') {
                consumeSign()
            }
            if (allowHexPrefix && body.startsWith("0x", ignoreCase = true)) {
                consumeHexPrefix()
            }
            require(!(allowHexPrefix && (body[0] == '+' || body[0] == '-'))) {
                "Malformed hexadecimal string: $value"
            }

            val digits = body.trimStart('0').ifEmpty { "0" }
            return ((sign == '-') to digits)
        }

        private fun ByteArray.toWords(endian: Endian): ULongArray {
            if (isEmpty()) {
                return ulongArrayOf(0UL)
            }

            when (endian) {
                Endian.BE -> {
                    var first = 0
                    while (first < size && this[first] == 0.toByte()) {
                        first++
                    }
                    if (first == size) {
                        return ulongArrayOf(0UL)
                    }

                    val words = ULongArray((size - first + 7) / 8)
                    var wordIndex = 0
                    var end = size
                    while (end > first) {
                        val start = max(first, end - 8)
                        var word = 0UL
                        for (i in start until end) {
                            word = (word shl 8) or this[i].toUByte().toULong()
                        }
                        words[wordIndex++] = word
                        end = start
                    }
                    return words
                }

                Endian.LE -> {
                    var endExclusive = size
                    while (endExclusive > 0 && this[endExclusive - 1] == 0.toByte()) {
                        endExclusive--
                    }
                    if (endExclusive == 0) {
                        return ulongArrayOf(0UL)
                    }

                    val words = ULongArray((endExclusive + 7) / 8)
                    var wordIndex = 0
                    var start = 0
                    while (start < endExclusive) {
                        val end = minOf(start + 8, endExclusive)
                        var word = 0UL
                        for (i in start until end) {
                            val shift = (i - start) * 8
                            word = word or (this[i].toUByte().toULong() shl shift)
                        }
                        words[wordIndex++] = word
                        start = end
                    }
                    return words
                }
            }
        }
    }
}
