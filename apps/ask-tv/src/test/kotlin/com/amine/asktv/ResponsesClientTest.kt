package com.amine.asktv
import org.junit.Assert.*
import org.junit.Test
import kotlinx.serialization.json.*
class ResponsesClientTest {
    @Test fun catalogKeepsOnlyListedInServerOrder() { val models=SubscriptionClient.parseModels("""{"models":[{"slug":"b","display_name":"B","visibility":"list"},{"slug":"hidden","visibility":"hidden"},{"slug":"a","display_name":"A","visibility":"list"}]}"""); assertEquals(listOf("b","a"),models.map{it.slug}) }
    @Test fun subscriptionPayloadIncludesWholeBoundedContextAndRequiredFlags() { val s=ConversationSession(); s.complete("first","answer"); val p=SubscriptionClient.payload("server-model",s.input("next")); assertFalse(p["store"]!!.jsonPrimitive.boolean); assertTrue(p["stream"]!!.jsonPrimitive.boolean); assertEquals(3,p["input"]!!.jsonArray.size); assertEquals("server-model",p["model"]!!.jsonPrimitive.content) }
    private fun consume(s:String,canceled:()->Boolean={false}):StreamResult = ResponsesStream.consume(s.reader().buffered(),canceled){}
    @Test fun textDeltasNeedCompleted() { val r=consume("data: {\"type\":\"response.output_text.delta\",\"delta\":\"Hello\"}\n\ndata: {\"type\":\"response.completed\"}\n\n"); assertTrue(r.complete); assertEquals("Hello",r.text) }
    @Test fun eofWithoutCompletionIsFailure() { val r=consume("data: {\"type\":\"response.output_text.delta\",\"delta\":\"Partial\"}\n\n"); assertFalse(r.complete); assertEquals("Partial",r.text) }
    @Test fun limitsAfterDeltaRemainIncomplete() { val r=consume("data: {\"type\":\"response.output_text.delta\",\"delta\":\"Partial\"}\n\ndata: {\"type\":\"response.failed\",\"response\":{\"error\":{\"code\":\"subscription_sharing_usage_limit_exceeded\"}}}\n\n"); assertFalse(r.complete); assertTrue(r.message.contains("limit",true)) }
    @Test fun incompleteEventIsFailure() { assertFalse(consume("data: {\"type\":\"response.incomplete\"}\n\n").complete) }
    @Test fun malformedSseIsFailure() { assertFalse(consume("data: invalid\n\n").complete) }
    @Test fun cancelNeverCompletes() { assertFalse(consume("data: {\"type\":\"response.completed\"}\n\n",{true}).complete) }
    @Test fun hiddenModelCannotBeSelected() { assertThrows(IllegalArgumentException::class.java) { SubscriptionClient.requireModel("hidden",listOf(Model("visible","Visible"))) } }
}
