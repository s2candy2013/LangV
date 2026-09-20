package com.linusv.englishcoach

import com.linusv.englishcoach.data.parseFeedback
import com.linusv.englishcoach.data.parseLesson
import org.junit.Assert.assertEquals
import org.junit.Test

class JsonContractTest {
    @Test fun parsesLessonWrappedInMarkdownFence() {
        val lesson = parseLesson("""```json {"title":"T","topic":"travel","cefr":"A2","vocabulary":[],"grammar":[],"passage":{"title":"P","textEn":"Text","translationVi":"Dịch","questions":[]},"speakingPrompts":[]}```""")
        assertEquals("T", lesson.title)
    }

    @Test fun parsesFeedback() {
        val feedback = parseFeedback("prefix {\"pronunciation\":80,\"fluency\":70,\"issues\":[],\"tips\":[]}")
        assertEquals(80, feedback.pronunciation)
    }

    @Test fun parsesMultilingualLesson() {
        val lesson = parseLesson("""{"title":"毎日の日本語","topic":"daily life","languageCode":"ja","level":"N5","vocabulary":[{"term":"こんにちは","pronunciation":"こんにちは","romanization":"konnichiwa","partOfSpeech":"lời chào","meaningVi":"xin chào","exampleTarget":"こんにちは。","exampleVi":"Xin chào."}],"grammar":[],"passage":{"title":"P","textTarget":"こんにちは。","translationVi":"Xin chào.","questions":[]},"speakingPrompts":[]}""")
        assertEquals("ja", lesson.languageCode)
        assertEquals("konnichiwa", lesson.vocabulary.first().romanization)
    }
}
