package com.crystal.alarm.domain

import com.crystal.alarm.domain.rescue.RescueAnswer
import com.crystal.alarm.domain.rescue.RescueChallenge
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RescueChallengeTest {
    private val challenge = RescueChallenge(Random(7))

    @Test
    fun `operands are hard two digit numbers`() {
        var progress = challenge.start()
        repeat(200) {
            val (a, b) = progress.problem
            listOf(a, b).forEach { n ->
                assertTrue(n in 13..99 && n % 10 != 0 && n % 11 != 0, "bad operand $n")
            }
            progress = (challenge.answer(progress, -1) as RescueAnswer.Wrong).progress
        }
    }

    @Test
    fun `a mistake resets progress`() {
        var progress = challenge.start()
        repeat(3) { progress = assertIs<RescueAnswer.Correct>(challenge.answer(progress, progress.problem.answer)).progress }
        assertEquals(3, progress.solved)

        val wrong = assertIs<RescueAnswer.Wrong>(challenge.answer(progress, progress.problem.answer + 1))
        assertEquals(0, wrong.progress.solved)
    }

    @Test
    fun `completes after the required streak`() {
        var progress = challenge.start()
        repeat(RescueChallenge.REQUIRED_ANSWERS - 1) {
            progress = assertIs<RescueAnswer.Correct>(challenge.answer(progress, progress.problem.answer)).progress
        }
        val done = assertIs<RescueAnswer.Completed>(challenge.answer(progress, progress.problem.answer))
        assertTrue(done.progress.isComplete)
    }
}
