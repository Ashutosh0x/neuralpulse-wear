package com.alphahealth.monitor.vision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TemporalConsensusEngineTest {

    @Test
    fun testConsensus_InitialState() {
        val engine = TemporalConsensusEngine(windowSize = 3, requiredAgreement = 2)
        assertNull(engine.addPredictionAndCheckConsensus("Apple"))
    }

    @Test
    fun testConsensus_ReachedWithTwoIdentical() {
        val engine = TemporalConsensusEngine(windowSize = 3, requiredAgreement = 2)
        assertNull(engine.addPredictionAndCheckConsensus("Apple"))
        assertEquals("Apple", engine.addPredictionAndCheckConsensus("Apple"))
    }

    @Test
    fun testConsensus_NoConsensusWithMixedInputs() {
        val engine = TemporalConsensusEngine(windowSize = 3, requiredAgreement = 2)
        assertNull(engine.addPredictionAndCheckConsensus("Apple"))
        assertNull(engine.addPredictionAndCheckConsensus("Banana"))
        assertNull(engine.addPredictionAndCheckConsensus("Orange"))
    }

    @Test
    fun testConsensus_SlidingWindowRejection() {
        val engine = TemporalConsensusEngine(windowSize = 3, requiredAgreement = 2)
        assertNull(engine.addPredictionAndCheckConsensus("Apple")) // History: [Apple]
        assertNull(engine.addPredictionAndCheckConsensus("Banana")) // History: [Apple, Banana]
        assertNull(engine.addPredictionAndCheckConsensus("Orange")) // History: [Banana, Orange] (Apple discarded)
        // Banana is in history once. Next frame Banana. Banana count should be 2.
        assertEquals("Banana", engine.addPredictionAndCheckConsensus("Banana")) // History: [Orange, Banana, Banana]
    }

    @Test
    fun testConsensus_ClearHistory() {
        val engine = TemporalConsensusEngine(windowSize = 3, requiredAgreement = 2)
        assertNull(engine.addPredictionAndCheckConsensus("Apple"))
        engine.clearHistory()
        assertNull(engine.addPredictionAndCheckConsensus("Apple"))
    }
}
