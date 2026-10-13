package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Test

class AttendanceCalculatorTest {

    @Test
    fun case1_attended26_total40_target75() {
        val pct = AttendanceCalculator.calculatePercentage(26, 40)
        assertEquals(65.0, pct, 0.01)

        val required = AttendanceCalculator.calculateRequiredClasses(26, 40, 75)
        assertEquals(16, required)
    }

    @Test
    fun case2_attended30_total40_target75() {
        val pct = AttendanceCalculator.calculatePercentage(30, 40)
        assertEquals(75.0, pct, 0.01)

        val required = AttendanceCalculator.calculateRequiredClasses(30, 40, 75)
        assertEquals(0, required)
    }

    @Test
    fun case3_attended0_total0_divisionByZeroSafe() {
        val pct = AttendanceCalculator.calculatePercentage(0, 0)
        assertEquals(0.0, pct, 0.01)

        val stats = AttendanceCalculator.computeStats(0, 0, 0, 75)
        assertEquals(AttendanceStatus.NOT_AVAILABLE, stats.status)
        assertEquals(0, stats.requiredConsecutiveClasses)
    }

    @Test
    fun case4_attended0_total40_target75() {
        val pct = AttendanceCalculator.calculatePercentage(0, 40)
        assertEquals(0.0, pct, 0.01)

        val required = AttendanceCalculator.calculateRequiredClasses(0, 40, 75)
        // (0 + x)/(40 + x) >= 0.75 => x >= 30 + 0.75x => 0.25x >= 30 => x >= 120
        assertEquals(120, required)
    }

    @Test
    fun case5_bunkableClasses_calculation() {
        // Attended 35 out of 40, Target 75%
        // Current = 35/40 = 87.5%
        // Max bunks y: 35 / (40 + y) >= 0.75 => 40 + y <= 35 / 0.75 = 46.66 => y <= 6
        val bunks = AttendanceCalculator.calculateBunkableClasses(35, 40, 75)
        assertEquals(6, bunks)
    }

    @Test
    fun gpa_sgpa_and_cgpa_calculation() {
        val courses = listOf(
            CourseGrade("Math", 4, 10.0),
            CourseGrade("CS", 4, 8.0),
            CourseGrade("Physics", 2, 9.0)
        )
        // Weighted: (4*10 + 4*8 + 2*9) = 40 + 32 + 18 = 90. Total credits = 10. SGPA = 9.0
        val sgpa = GpaCalculator.calculateSgpa(courses)
        assertEquals(9.0, sgpa, 0.01)

        val semesters = listOf(
            SemesterGpaResult(1, 20, 8.5),
            SemesterGpaResult(2, 20, 9.5)
        )
        // CGPA: (20*8.5 + 20*9.5) / 40 = (170 + 190) / 40 = 360 / 40 = 9.0
        val cgpa = GpaCalculator.calculateCgpa(semesters)
        assertEquals(9.0, cgpa, 0.01)
    }
}
