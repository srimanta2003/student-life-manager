package com.example.util

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

enum class AttendanceStatus {
    ON_TRACK,
    WARNING,
    CRITICAL,
    NOT_AVAILABLE
}

data class AttendanceStats(
    val attended: Int,
    val missed: Int,
    val cancelled: Int,
    val totalConducted: Int,
    val percentage: Double,
    val targetPercentage: Int,
    val requiredConsecutiveClasses: Int,
    val safeBunksAvailable: Int,
    val status: AttendanceStatus
)

object AttendanceCalculator {

    /**
     * Calculates the attendance percentage: (Attended / Total) * 100.
     * Returns 0.0 when total is 0 to avoid division by zero.
     */
    fun calculatePercentage(attended: Int, total: Int): Double {
        if (total <= 0) return 0.0
        val clampedAttended = attended.coerceIn(0, total)
        return (clampedAttended.toDouble() / total.toDouble()) * 100.0
    }

    /**
     * Smallest non-negative integer x such that:
     * (A + x) / (T + x) >= p
     * where p = targetPercent / 100.0
     */
    fun calculateRequiredClasses(attended: Int, total: Int, targetPercent: Int): Int {
        val target = (targetPercent.coerceIn(1, 100)).toDouble() / 100.0
        if (total <= 0) return 0
        val currentP = attended.toDouble() / total.toDouble()
        if (currentP >= target) return 0
        if (target >= 1.0) {
            // Cannot reach 100% if already missed any classes
            return if (attended < total) Int.MAX_VALUE else 0
        }

        // (A + x) >= target * (T + x) => (1 - target) * x >= target * T - A
        val numerator = target * total - attended
        val denominator = 1.0 - target
        if (numerator <= 0) return 0

        val required = ceil(numerator / denominator).toInt()
        return max(0, required)
    }

    /**
     * Largest non-negative integer y such that:
     * A / (T + y) >= p
     * (How many consecutive classes can be safely skipped while staying >= targetPercent)
     */
    fun calculateBunkableClasses(attended: Int, total: Int, targetPercent: Int): Int {
        val target = (targetPercent.coerceIn(1, 100)).toDouble() / 100.0
        if (total <= 0) return 0
        val currentP = attended.toDouble() / total.toDouble()
        if (currentP < target) return 0
        if (target <= 0.0) return Int.MAX_VALUE

        // A / (T + y) >= target => T + y <= A / target => y <= (A / target) - T
        val maxMissable = floor((attended.toDouble() / target) - total.toDouble()).toInt()
        return max(0, maxMissable)
    }

    /**
     * Projects attendance if a student attends 'futureAttended' and misses 'futureMissed' classes.
     */
    fun projectAttendance(
        currentAttended: Int,
        currentTotal: Int,
        futureAttended: Int,
        futureMissed: Int
    ): Double {
        val projectedAttended = currentAttended + futureAttended
        val projectedTotal = currentTotal + futureAttended + futureMissed
        return calculatePercentage(projectedAttended, projectedTotal)
    }

    fun computeStats(
        attended: Int,
        missed: Int,
        cancelled: Int,
        targetPercent: Int = 75
    ): AttendanceStats {
        val total = attended + missed
        val percentage = calculatePercentage(attended, total)

        val status = when {
            total == 0 -> AttendanceStatus.NOT_AVAILABLE
            percentage >= targetPercent -> AttendanceStatus.ON_TRACK
            percentage >= targetPercent - 10 -> AttendanceStatus.WARNING
            else -> AttendanceStatus.CRITICAL
        }

        val required = calculateRequiredClasses(attended, total, targetPercent)
        val safeBunks = calculateBunkableClasses(attended, total, targetPercent)

        return AttendanceStats(
            attended = attended,
            missed = missed,
            cancelled = cancelled,
            totalConducted = total,
            percentage = percentage,
            targetPercentage = targetPercent,
            requiredConsecutiveClasses = required,
            safeBunksAvailable = safeBunks,
            status = status
        )
    }
}
