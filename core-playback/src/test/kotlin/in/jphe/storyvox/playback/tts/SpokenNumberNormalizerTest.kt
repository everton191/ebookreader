package `in`.jphe.storyvox.playback.tts

import org.junit.Assert.assertEquals
import org.junit.Test

class SpokenNumberNormalizerTest {
    @Test fun `speaks brazilian Portuguese integers and decimals`() {
        assertEquals("Há doze livros e mil duzentos e trinta e quatro vírgula cinco zero leitores.", SpokenNumberNormalizer.normalize("Há 12 livros e 1.234,50 leitores.", "pt_BR"))
    }

    @Test fun `speaks English integers and decimals`() {
        assertEquals("Chapter twelve has one thousand two hundred thirty-four point five zero words.", SpokenNumberNormalizer.normalize("Chapter 12 has 1,234.50 words.", "en_US"))
    }

    @Test fun `preserves urls versions clock times and phone numbers`() {
        val source = "Use https://x.test/v1.2 at 12:30, app v2.3.1, or 99999-9999."
        assertEquals(source, SpokenNumberNormalizer.normalize(source, "pt_BR"))
    }

    @Test fun `does not touch unsupported languages or identifiers`() {
        assertEquals("ID AB12 custa 42.", SpokenNumberNormalizer.normalize("ID AB12 custa 42.", "fr_FR"))
        assertEquals("ID AB12 custa quarenta e dois.", SpokenNumberNormalizer.normalize("ID AB12 custa 42.", "pt_BR"))
    }
}
