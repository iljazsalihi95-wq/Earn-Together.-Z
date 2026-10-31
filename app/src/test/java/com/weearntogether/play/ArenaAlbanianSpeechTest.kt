package com.weearntogether.play

import org.junit.Assert.assertEquals
import org.junit.Test

class ArenaAlbanianSpeechTest {
    @Test fun numbersAreGeneralAndDoNotConfuse108With114() {
        val cases=mapOf(1L to "një",10L to "dhjetë",20L to "njëzet",40L to "dyzet",100L to "njëqind",108L to "njëqind e tetë",114L to "njëqind e katërmbëdhjetë",120L to "njëqind e njëzet",2026L to "dy mijë e njëzet e gjashtë")
        cases.forEach{(n,spoken)->assertEquals(spoken,ArenaAlbanianSpeech.forVoice(n.toString(),"sq"))}
    }
    @Test fun honorificsExpandWithoutChangingOrdinaryWords() {
        assertEquals("alejhi selam radijallahu anhu xhel-le shanuhu sal-lallahu alejhi ve sel-lem",ArenaAlbanianSpeech.forVoice("a.s. R. A. XH.SH. ﷺ","sq"))
        assertEquals("as ra rast sure shkencë",ArenaAlbanianSpeech.forVoice("as ra rast sure shkencë","sq"))
    }
    @Test fun referencesDecimalsPercentagesAndDates() {
        assertEquals("surja dy, ajeti dyqind e pesëdhjetë e pesë",ArenaAlbanianSpeech.forVoice("2:255","sq"))
        assertEquals("dy presje pesë për qind",ArenaAlbanianSpeech.forVoice("2.5%","sq"))
        assertEquals("tridhjetë e një tetor dy mijë e njëzet e gjashtë",ArenaAlbanianSpeech.forVoice("31.10.2026","sq"))
    }
    @Test fun otherLanguagesRemainUntouched() { assertEquals("114 a.s.",ArenaAlbanianSpeech.forVoice("114 a.s.","de")) }
}
