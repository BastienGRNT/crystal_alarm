package com.crystal.alarm.domain.rescue

import java.text.Normalizer
import javax.inject.Inject
import kotlin.random.Random

enum class Operator(val symbol: String) { Plus("+"), Times("×") }

data class CalcProblem(val left: Int, val operator: Operator, val right: Int) {
    val answer: Int
        get() = when (operator) {
            Operator.Plus -> left + right
            Operator.Times -> left * right
        }
}

enum class RescueStep { CopyText, Calculation, Done }

data class RescueProgress(
    /** Random words to copy, about four lines on a phone. */
    val text: String,
    val problem: CalcProblem,
    val step: RescueStep,
) {
    val isComplete: Boolean get() = step == RescueStep.Done
}

/** How far the typed text matches: [matched] characters of the target are right so far. */
data class TextCheck(val matched: Int, val hasError: Boolean, val isComplete: Boolean)

sealed interface RescueAnswer {
    data class Completed(val progress: RescueProgress) : RescueAnswer
    /** A wrong result only swaps the calculation: the copied text stays done. */
    data class Wrong(val expected: Int, val progress: RescueProgress) : RescueAnswer
}

/**
 * Rescue mode, when the wake code is out of reach: copy a randomly generated text of
 * [TEXT_WORDS] words, then solve one simple calculation, while the alarm keeps ringing.
 * Long enough to wake up, with nothing to memorize from one morning to the next.
 *
 * Case, accents and extra spaces are ignored when comparing the text.
 */
class RescueChallenge(private val random: Random) {

    @Inject
    constructor() : this(Random.Default)

    fun start(): RescueProgress = RescueProgress(
        text = List(TEXT_WORDS) { WORDS.random(random) }.joinToString(" "),
        problem = nextProblem(previous = null),
        step = RescueStep.CopyText,
    )

    fun checkText(progress: RescueProgress, input: String): TextCheck {
        val target = normalize(progress.text)
        val typed = normalize(input)
        val matched = target.commonPrefixWith(typed).length
        return TextCheck(
            matched = matched,
            hasError = matched < typed.length,
            isComplete = typed == target,
        )
    }

    /** Moves on to the calculation once the whole text is copied; otherwise returns [progress] unchanged. */
    fun submitText(progress: RescueProgress, input: String): RescueProgress =
        if (progress.step == RescueStep.CopyText && checkText(progress, input).isComplete) {
            progress.copy(step = RescueStep.Calculation)
        } else {
            progress
        }

    fun answer(progress: RescueProgress, value: Int): RescueAnswer {
        if (progress.step != RescueStep.Calculation) return RescueAnswer.Wrong(progress.problem.answer, progress)
        return if (value == progress.problem.answer) {
            RescueAnswer.Completed(progress.copy(step = RescueStep.Done))
        } else {
            RescueAnswer.Wrong(progress.problem.answer, progress.copy(problem = nextProblem(progress.problem)))
        }
    }

    /** Either 12..49 + 12..49, or a times table from 3 to 9. */
    private fun nextProblem(previous: CalcProblem?): CalcProblem {
        while (true) {
            val problem = if (random.nextBoolean()) {
                CalcProblem(random.nextInt(12, 50), Operator.Plus, random.nextInt(12, 50))
            } else {
                CalcProblem(random.nextInt(3, 10), Operator.Times, random.nextInt(3, 10))
            }
            if (problem != previous) return problem
        }
    }

    companion object {
        const val TEXT_WORDS = 18

        /** Lowercase, accent-free, collapsed spaces: only the words themselves count. */
        fun normalize(text: String): String =
            Normalizer.normalize(text, Normalizer.Form.NFD)
                .replace(DIACRITICS, "")
                .lowercase()
                .trim()
                .replace(SPACES, " ")

        private val DIACRITICS = Regex("\\p{Mn}+")
        private val SPACES = Regex("\\s+")

        /** Common, accent-free French words: easy to read half asleep, no autocorrect surprise. */
        private val WORDS = listOf(
            "arbre", "balle", "bateau", "bougie", "bureau", "cadre", "camion", "canard", "carte", "chaise",
            "chat", "chemin", "cheval", "citron", "clou", "coffre", "colline", "corde", "coton", "couloir",
            "crayon", "cuisine", "dragon", "fenetre", "feuille", "fleur", "fontaine", "foret", "fourchette", "fromage",
            "fusee", "gant", "gateau", "girafe", "glace", "grenier", "guitare", "hibou", "jardin", "journal",
            "lampe", "lapin", "lavande", "lettre", "lion", "livre", "lune", "maison", "marteau", "matin",
            "melon", "miroir", "montagne", "moulin", "mouton", "nuage", "orange", "oreiller", "panier", "papier",
            "parapluie", "pomme", "pont", "porte", "poule", "prairie", "radio", "raisin", "renard", "rideau",
            "riviere", "robot", "route", "sable", "salade", "sapin", "savon", "soleil", "sucre", "table",
            "tapis", "tasse", "tigre", "tomate", "train", "tulipe", "valise", "velo", "village", "violon",
            "voiture", "zebre", "bleu", "rouge", "vert", "jaune", "grand", "petit", "rapide", "calme",
            "doux", "froid", "chaud", "joli", "lent", "fort", "court", "long", "rond", "vide",
            "marche", "mange", "danse", "chante", "dort", "saute", "nage", "lit", "rit", "cherche",
        )
    }
}
