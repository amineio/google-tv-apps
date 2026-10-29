package com.amine.asktv
import org.junit.Assert.*
import org.junit.Test
class AskControllerTest {
    @Test fun idleAnswerHasNoStopAction() {
        assertNull(AskUi(page=Page.ANSWER,answer="A completed answer").stopLabel)
    }
    @Test fun speakingAnswerNamesItsStopAction() {
        assertEquals("Stop reading",AskUi(page=Page.ANSWER,busy="Speaking…").stopLabel)
        assertEquals("Stop",AskUi(page=Page.ANSWER,busy="Answering…").stopLabel)
    }

    @Test fun cancelSpeechRetainsCompletedAnswer() {
        val completed = AskUi(page=Page.ANSWER,answer="A completed answer",incomplete=false,busy="Speaking…")
        val stopped = completed.afterCancellation()
        assertEquals("A completed answer",stopped.answer)
        assertNull(stopped.busy)
        assertFalse(stopped.incomplete)
    }
    @Test fun cancelInferenceKeepsPartialAnswerIncomplete() {
        val partial = AskUi(page=Page.ANSWER,answer="A partial answer",incomplete=true,busy="Answering…")
        val stopped = partial.afterCancellation()
        assertEquals("A partial answer",stopped.answer)
        assertNull(stopped.busy)
        assertTrue(stopped.incomplete)
    }

    @Test fun newOperationCancelsPrevious() { val c=AskController(); var canceled=false; c.begin { canceled=true }; c.begin{}; assertTrue(canceled) }
    @Test fun stopInvalidatesPendingCallbacks() { val c=AskController(); val first=c.begin{}; c.stop(); assertFalse(c.isCurrent(first)) }
    @Test fun staleCallbacksIgnoredAfterReplacement() { val c=AskController(); val first=c.begin{}; val next=c.begin{}; assertFalse(c.isCurrent(first)); assertTrue(c.isCurrent(next)) }
    @Test fun backCancelsBusyBeforeExit() { val c=AskController(); c.begin{}; assertTrue(c.back()); assertFalse(c.back()) }
    @Test fun recognitionDenialOffersTypedFallback() { assertTrue(AskController.recognitionError("permission").contains("Type")) }
    @Test fun recognitionUnavailableOffersTypedFallback() { assertTrue(AskController.recognitionError("unavailable").contains("Type")) }
    @Test fun sessionDropsOldestWholePairs() { val s=ConversationSession(); repeat(8){s.complete("q$it","a$it")}; assertEquals(6,s.pairs.size); assertEquals("q2",s.pairs.first().question) }
    @Test fun sessionHonorsCharacterBound() { val s=ConversationSession(); repeat(8){s.complete("q","x".repeat(5000))}; assertTrue(s.pairs.sumOf{it.question.length+it.answer.length}<=24000) }
    @Test fun overlongQuestionRejected() { assertThrows(IllegalArgumentException::class.java) { ConversationSession().input("x".repeat(4001)) } }
    @Test fun newConversationClearsAllContext() { val s=ConversationSession(); s.complete("q","a"); s.clear(); assertTrue(s.pairs.isEmpty()) }
}
