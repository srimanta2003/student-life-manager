package com.example.util

enum class GradingScale(val label: String, val maxPoint: Double) {
    TEN_POINT("10-Point Scale (Indian/Global Universities)", 10.0),
    FOUR_POINT("4-Point Scale (US/Standard International)", 4.0)
}

data class CourseGrade(
    val courseName: String,
    val credits: Int,
    val gradePoint: Double
)

data class SemesterGpaResult(
    val semesterNumber: Int,
    val totalCredits: Int,
    val sgpa: Double
)

object GpaCalculator {

    val tenPointGrades = mapOf(
        "O (Outstanding)" to 10.0,
        "A+ (Excellent)" to 9.0,
        "A (Very Good)" to 8.0,
        "B+ (Good)" to 7.0,
        "B (Above Average)" to 6.0,
        "C (Average)" to 5.0,
        "P (Pass)" to 4.0,
        "F (Fail)" to 0.0
    )

    val fourPointGrades = mapOf(
        "A (4.0)" to 4.0,
        "A- (3.7)" to 3.7,
        "B+ (3.3)" to 3.3,
        "B (3.0)" to 3.0,
        "B- (2.7)" to 2.7,
        "C+ (2.3)" to 2.3,
        "C (2.0)" to 2.0,
        "C- (1.7)" to 1.7,
        "D (1.0)" to 1.0,
        "F (0.0)" to 0.0
    )

    /**
     * SGPA = Sum(credits * gradePoint) / Sum(credits)
     */
    fun calculateSgpa(courses: List<CourseGrade>): Double {
        val totalCredits = courses.sumOf { it.credits }
        if (totalCredits <= 0) return 0.0
        val weightedSum = courses.sumOf { it.credits * it.gradePoint }
        val result = weightedSum / totalCredits
        return String.format("%.2f", result).toDoubleOrNull() ?: result
    }

    /**
     * CGPA = Sum(semesterCredits * sgpa) / Sum(semesterCredits)
     */
    fun calculateCgpa(semesters: List<SemesterGpaResult>): Double {
        val totalCredits = semesters.sumOf { it.totalCredits }
        if (totalCredits <= 0) return 0.0
        val weightedSum = semesters.sumOf { it.totalCredits * it.sgpa }
        val result = weightedSum / totalCredits
        return String.format("%.2f", result).toDoubleOrNull() ?: result
    }
}
