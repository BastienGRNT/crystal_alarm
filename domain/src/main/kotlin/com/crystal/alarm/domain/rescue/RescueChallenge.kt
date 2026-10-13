package com.crystal.alarm.domain.rescue

import javax.inject.Inject
import kotlin.random.Random

data class MultiplicationProblem(val left: Int, val right: Int) {
    val answer: Int get() = left * right
}

data class RescueProgress(
    val problem: MultiplicationProblem,
    val solved: Int,
    val required: Int,
) {
    val isComplete: Boolean get() = solved >= required
}

sealed interface RescueAnswer {
    data class Correct(val progress: RescueProgress) : RescueAnswer
    /** Any mistake resets the whole challenge. */
    data class Wrong(val expected: Int, val progress: RescueProgress) : RescueAnswer
    data class Completed(val progress: RescueProgress) : RescueAnswer
}

/**
 * Rescue mode: [REQUIRED_ANSWERS] two-digit multiplications in a row, solved mentally,
 * while the alarm keeps ringing. One mistake and it starts over. It must stay painful,
 * otherwise it becomes the default way to stop the alarm.
 */
class RescueChallenge(private val random: Random) {

    @Inject
    constructor() : this(Random.Default)

    fun start(): RescueProgress = RescueProgress(nextProblem(previous = null), solved = 0, required = REQUIRED_ANSWERS)

    fun answer(progress: RescueProgress, value: Int): RescueAnswer {
        if (value != progress.problem.answer) {
            return RescueAnswer.Wrong(
                expected = progress.problem.answer,
                progress = progress.copy(problem = nextProblem(progress.problem), solved = 0),
            )
        }
        val solved = progress.solved + 1
        val next = progress.copy(problem = nextProblem(progress.problem), solved = solved)
        return if (next.isComplete) RescueAnswer.Completed(next) else RescueAnswer.Correct(next)
    }

    private fun nextProblem(previous: MultiplicationProblem?): MultiplicationProblem {
        while (true) {
            val problem = MultiplicationProblem(operand(), operand())
            if (problem != previous) return problem
        }
    }

    /** 13..99, skipping multiples of 10 and 11 which are too easy. */
    private fun operand(): Int {
        while (true) {
            val n = random.nextInt(MIN_OPERAND, MAX_OPERAND + 1)
            if (n % 10 != 0 && n % 11 != 0) return n
        }
    }

    companion object {
        const val REQUIRED_ANSWERS = 6
        private const val MIN_OPERAND = 13
        private const val MAX_OPERAND = 99
    }
}
