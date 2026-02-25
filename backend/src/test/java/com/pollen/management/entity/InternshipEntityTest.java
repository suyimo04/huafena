package com.pollen.management.entity;

import com.pollen.management.entity.enums.InternshipStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class InternshipEntityTest {

    @Test
    void builderShouldCreateInternshipWithDefaults() {
        Internship internship = Internship.builder()
                .userId(1L)
                .startDate(LocalDate.now())
                .expectedEndDate(LocalDate.now().plusDays(30))
                .build();

        assertNull(internship.getId());
        assertEquals(1L, internship.getUserId());
        assertNull(internship.getMentorId());
        assertEquals(InternshipStatus.IN_PROGRESS, internship.getStatus());
    }

    @Test
    void builderShouldAcceptAllFields() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);

        Internship internship = Internship.builder()
                .id(10L)
                .userId(1L)
                .mentorId(2L)
                .startDate(start)
                .expectedEndDate(end)
                .status(InternshipStatus.CONVERTED)
                .build();

        assertEquals(10L, internship.getId());
        assertEquals(1L, internship.getUserId());
        assertEquals(2L, internship.getMentorId());
        assertEquals(start, internship.getStartDate());
        assertEquals(end, internship.getExpectedEndDate());
        assertEquals(InternshipStatus.CONVERTED, internship.getStatus());
    }

    @Test
    void internshipStatusEnumShouldContainAllValues() {
        InternshipStatus[] values = InternshipStatus.values();
        assertEquals(6, values.length);
        assertNotNull(InternshipStatus.valueOf("IN_PROGRESS"));
        assertNotNull(InternshipStatus.valueOf("PENDING_CONVERSION"));
        assertNotNull(InternshipStatus.valueOf("PENDING_EVALUATION"));
        assertNotNull(InternshipStatus.valueOf("CONVERTED"));
        assertNotNull(InternshipStatus.valueOf("EXTENDED"));
        assertNotNull(InternshipStatus.valueOf("TERMINATED"));
    }

    @Test
    void internshipTaskBuilderShouldCreateWithDefaults() {
        InternshipTask task = InternshipTask.builder()
                .internshipId(1L)
                .taskName("Learn group rules")
                .deadline(LocalDate.now().plusDays(7))
                .build();

        assertNull(task.getId());
        assertEquals(1L, task.getInternshipId());
        assertEquals("Learn group rules", task.getTaskName());
        assertFalse(task.getCompleted());
        assertNull(task.getCompletedAt());
    }

    @Test
    void internshipTaskBuilderShouldAcceptAllFields() {
        InternshipTask task = InternshipTask.builder()
                .id(5L)
                .internshipId(1L)
                .taskName("Handle violations")
                .taskDescription("Practice handling user violations")
                .deadline(LocalDate.of(2024, 2, 15))
                .completed(true)
                .build();

        assertEquals(5L, task.getId());
        assertEquals("Handle violations", task.getTaskName());
        assertEquals("Practice handling user violations", task.getTaskDescription());
        assertTrue(task.getCompleted());
    }
}
