package com.digitaldiscipline.spike.intervention.validation

import com.digitaldiscipline.spike.intervention.session.InterventionSession
import java.util.Random

sealed class CognitiveChallenge {
    data class MathQuestion(
        val questionText: String,
        val correctAnswer: Int,
        val options: List<Int>
    ) : CognitiveChallenge()

    data class MemorySequence(
        val sequence: List<Int>, // 0..3 (Colors/tiles)
        val durationMs: Long
    ) : CognitiveChallenge()

    data class TapSequence(
        val numbers: List<Int> // 1..5 in random order
    ) : CognitiveChallenge()

    data class ReactionTest(
        val delayMs: Long // Delay before green signal
    ) : CognitiveChallenge()
}

class CognitiveInteractionValidator : InterventionValidator {

    private var activeSession: InterventionSession? = null
    private var callback: ((ValidationResult) -> Unit)? = null
    private val random = Random()

    override fun isSupported(): Boolean = true

    override fun startValidation(
        session: InterventionSession,
        onResult: (ValidationResult) -> Unit
    ) {
        activeSession = session
        callback = onResult
        onResult(ValidationResult.Progress(currentProgress = 0, requiredTarget = session.requiredReps.coerceAtLeast(1)))
    }

    fun generateMathChallenge(): CognitiveChallenge.MathQuestion {
        val a = random.nextInt(15) + 3
        val b = random.nextInt(15) + 3
        val isAdd = random.nextBoolean()
        val correct = if (isAdd) a + b else a - b
        val text = if (isAdd) "$a + $b = ?" else "$a − $b = ?"

        val options = mutableSetOf(correct)
        while (options.size < 4) {
            val offset = random.nextInt(7) - 3
            if (offset != 0) options.add(correct + offset)
        }
        return CognitiveChallenge.MathQuestion(
            questionText = text,
            correctAnswer = correct,
            options = options.toList().shuffled()
        )
    }

    fun submitAnswer(isCorrect: Boolean) {
        val session = activeSession ?: return
        val cb = callback ?: return

        if (!isCorrect) {
            cb(ValidationResult.Failed("Incorrect answer. Try again."))
            return
        }

        val nextProgress = session.currentProgress + 1
        session.updateProgress(nextProgress)
        val target = session.requiredReps.coerceAtLeast(1)

        cb(ValidationResult.Progress(currentProgress = nextProgress, requiredTarget = target))

        if (nextProgress >= target) {
            session.markCompleting()
            session.complete()
            cb(ValidationResult.Completed(session))
        }
    }

    override fun stopValidation() {
        activeSession = null
        callback = null
    }
}
