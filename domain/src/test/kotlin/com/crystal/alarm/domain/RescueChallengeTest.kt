package com.crystal.alarm.domain

import com.crystal.alarm.domain.rescue.Operator
import com.crystal.alarm.domain.rescue.RescueAnswer
import com.crystal.alarm.domain.rescue.RescueChallenge
import com.crystal.alarm.domain.rescue.RescueStep
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RescueChallengeTest {
    private val challenge = RescueChallenge(Random(7))

    @Test
    fun `text is random and long enough`() {
        val first = challenge.start()
        assertEquals(RescueChallenge.TEXT_WORDS, first.text.split(" ").size)
        assertNotEquals(first.text, challenge.start().text)
        assertEquals(RescueStep.CopyText, first.step)
    }

    @Test
    fun `text check ignores case, accents and extra spaces`() {
        val progress = challenge.start()
        val sloppy = "  " + progress.text.uppercase().replace("e", "é").replace(" ", "   ") + " "
        assertTrue(challenge.checkText(progress, sloppy).isComplete)
    }

    @Test
    fun `text check reports where the first mistake is`() {
        val progress = challenge.start()
        val ok = progress.text.take(10)
        val check = challenge.checkText(progress, ok + "#")
        assertEquals(10, check.matched)
        assertTrue(check.hasError)
        assertFalse(check.isComplete)
        assertFalse(challenge.checkText(progress, ok).hasError)
    }

    @Test
    fun `calculation only unlocks once the text is copied`() {
        val progress = challenge.start()
        assertIs<RescueAnswer.Wrong>(challenge.answer(progress, progress.problem.answer))
        assertEquals(progress, challenge.submitText(progress, progress.text.dropLast(1)))
        assertEquals(RescueStep.Calculation, challenge.submitText(progress, progress.text).step)
    }

    @Test
    fun `wrong result swaps the calculation, right result completes`() {
        val calc = challenge.start().let { challenge.submitText(it, it.text) }
        val wrong = assertIs<RescueAnswer.Wrong>(challenge.answer(calc, calc.problem.answer + 1))
        assertEquals(RescueStep.Calculation, wrong.progress.step)
        assertNotEquals(calc.problem, wrong.progress.problem)

        val done = assertIs<RescueAnswer.Completed>(challenge.answer(wrong.progress, wrong.progress.problem.answer))
        assertTrue(done.progress.isComplete)
    }

    @Test
    fun `calculations stay simple`() {
        var progress = challenge.start().let { challenge.submitText(it, it.text) }
        repeat(200) {
            val problem = progress.problem
            when (problem.operator) {
                Operator.Plus -> assertTrue(problem.left in 12..49 && problem.right in 12..49)
                Operator.Times -> assertTrue(problem.left in 3..9 && problem.right in 3..9)
            }
            progress = (challenge.answer(progress, -1) as RescueAnswer.Wrong).progress
        }
    }
}
