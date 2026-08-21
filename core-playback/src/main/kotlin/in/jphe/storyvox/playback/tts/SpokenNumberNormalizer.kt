package `in`.jphe.storyvox.playback.tts

/** Turns ordinary numbers into words immediately before synthesis only. */
object SpokenNumberNormalizer {
    const val VERSION = 1

    private val protectedToken = Regex(
        "(?i)https?://\\S+|www\\.\\S+|\\bv\\d+(?:\\.\\d+){1,3}\\b|\\b\\d+(?:\\.\\d+){2,3}\\b|\\b\\d{1,2}:\\d{2}\\b|\\b\\d{4,5}-\\d{4}\\b",
    )
    private val numberToken = Regex(
        "(?<![\\p{L}\\p{N}_-])(?:\\d{1,3}(?:[.,]\\d{3})+|\\d+)(?:[.,]\\d+)?(?![\\p{L}\\p{N}_-])",
    )

    fun normalize(text: String, language: String?): String {
        val mode = when {
            language?.startsWith("pt", ignoreCase = true) == true -> Mode.PORTUGUESE
            language?.startsWith("en", ignoreCase = true) == true -> Mode.ENGLISH
            else -> return text
        }
        if (text.none(Char::isDigit)) return text
        val result = StringBuilder(text.length)
        var cursor = 0
        protectedToken.findAll(text).forEach { protected ->
            result.append(normalizeSegment(text.substring(cursor, protected.range.first), mode))
            result.append(protected.value)
            cursor = protected.range.last + 1
        }
        result.append(normalizeSegment(text.substring(cursor), mode))
        return result.toString()
    }

    private fun normalizeSegment(text: String, mode: Mode): String = numberToken.replace(text) { match ->
        spokenNumber(match.value, mode) ?: match.value
    }

    private fun spokenNumber(token: String, mode: Mode): String? {
        val parts = splitNumber(token, mode) ?: return null
        if (parts.integerDigits.length > 1 && parts.integerDigits.startsWith('0')) return null
        val integer = parts.integerDigits.toLongOrNull() ?: return null
        if (integer > 999_999_999L) return null
        val integerWords = words(integer, mode)
        val fraction = parts.fractionDigits ?: return integerWords
        val separator = if (mode == Mode.PORTUGUESE) "vírgula" else "point"
        val digits = fraction.map { words(it.digitToInt().toLong(), mode) }.joinToString(" ")
        return "$integerWords $separator $digits"
    }

    private fun splitNumber(token: String, mode: Mode): NumberParts? {
        val comma = token.lastIndexOf(',')
        val dot = token.lastIndexOf('.')
        val decimalIndex = when (mode) {
            Mode.PORTUGUESE -> when {
                comma >= 0 -> comma
                dot >= 0 && token.length - dot - 1 != 3 -> dot
                else -> -1
            }
            Mode.ENGLISH -> when {
                dot >= 0 -> dot
                comma >= 0 && token.length - comma - 1 != 3 -> comma
                else -> -1
            }
        }
        val integerRaw = if (decimalIndex >= 0) token.substring(0, decimalIndex) else token
        val fraction = if (decimalIndex >= 0) token.substring(decimalIndex + 1) else null
        val digits = integerRaw.filter(Char::isDigit)
        if (digits.isEmpty() || fraction?.any { !it.isDigit() } == true) return null
        return NumberParts(digits, fraction)
    }

    private fun words(number: Long, mode: Mode): String = when (mode) {
        Mode.PORTUGUESE -> portuguese(number)
        Mode.ENGLISH -> english(number)
    }

    private fun portuguese(number: Long): String = when {
        number < 10 -> listOf("zero", "um", "dois", "três", "quatro", "cinco", "seis", "sete", "oito", "nove")[number.toInt()]
        number < 20 -> listOf("dez", "onze", "doze", "treze", "quatorze", "quinze", "dezesseis", "dezessete", "dezoito", "dezenove")[number.toInt() - 10]
        number < 100 -> tens(number, listOf("vinte", "trinta", "quarenta", "cinquenta", "sessenta", "setenta", "oitenta", "noventa"), ::portuguese, " e ")
        number == 100L -> "cem"
        number < 1_000 -> listOf("cento", "duzentos", "trezentos", "quatrocentos", "quinhentos", "seiscentos", "setecentos", "oitocentos", "novecentos")[(number / 100).toInt() - 1] + portugueseRemainder(number % 100)
        number < 1_000_000 -> portugueseScale(number, 1_000, "mil", "mil")
        else -> portugueseScale(number, 1_000_000, "milhão", "milhões")
    }

    private fun english(number: Long): String = when {
        number < 10 -> listOf("zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine")[number.toInt()]
        number < 20 -> listOf("ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen")[number.toInt() - 10]
        number < 100 -> tens(number, listOf("twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"), ::english, "-")
        number < 1_000 -> english(number / 100) + " hundred" + englishRemainder(number % 100)
        number < 1_000_000 -> english(number / 1_000) + " thousand" + englishRemainder(number % 1_000)
        else -> english(number / 1_000_000) + " million" + englishRemainder(number % 1_000_000)
    }

    private fun tens(number: Long, values: List<String>, units: (Long) -> String, separator: String): String {
        val tensWord = values[(number / 10).toInt() - 2]
        return if (number % 10 == 0L) tensWord else tensWord + separator + units(number % 10)
    }

    private fun portugueseScale(number: Long, divisor: Long, singular: String, plural: String): String {
        val major = number / divisor
        val label = if (major == 1L) singular else plural
        val prefix = if (divisor == 1_000L && major == 1L) "" else portuguese(major) + " "
        val remainder = number % divisor
        val suffix = when {
            remainder == 0L -> ""
            remainder < 100L -> " e ${portuguese(remainder)}"
            else -> " ${portuguese(remainder)}"
        }
        return prefix + label + suffix
    }

    private fun portugueseRemainder(value: Long): String = if (value == 0L) "" else " e ${portuguese(value)}"
    private fun englishRemainder(value: Long): String = if (value == 0L) "" else " ${english(value)}"

    private data class NumberParts(val integerDigits: String, val fractionDigits: String?)
    private enum class Mode { PORTUGUESE, ENGLISH }
}
