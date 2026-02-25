package com.pollen.management.service;

import com.pollen.management.entity.ApplicationTimeline;
import com.pollen.management.repository.ApplicationTimelineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationTimelineServiceImplTest {

    @Mock
    private ApplicationTimelineRepository applicationTimelineRepository;

    private ApplicationTimelineServiceImpl timelineService;

    @BeforeEach
    void setUp() {
        timelineService = new ApplicationTimelineServiceImpl(applicationTimelineRepository);
    }

    @Test
    void recordTimelineEvent_shouldSaveTimelineWithCorrectFields() {
        when(applicationTimelineRepository.save(any(ApplicationTimeline.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        timelineService.recordTimelineEvent(1L, "PENDING_INITIAL_REVIEW", "系统", "申请已提交");

        ArgumentCaptor<ApplicationTimeline> captor = ArgumentCaptor.forClass(ApplicationTimeline.class);
        verify(applicationTimelineRepository).save(captor.capture());

        ApplicationTimeline saved = captor.getValue();
        assertEquals(1L, saved.getApplicationId());
        assertEquals("PENDING_INITIAL_REVIEW", saved.getStatus());
        assertEquals("系统", saved.getOperator());
        assertEquals("申请已提交", saved.getDescription());
    }

    @Test
    void getTimeline_shouldReturnTimelineOrderedByCreatedAtAsc() {
        ApplicationTimeline t1 = ApplicationTimeline.builder()
                .id(1L).applicationId(10L).status("PENDING_INITIAL_REVIEW")
                .operator("系统").description("申请已提交")
                .build();
        t1.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0));

        ApplicationTimeline t2 = ApplicationTimeline.builder()
                .id(2L).applicationId(10L).status("INITIAL_REVIEW_PASSED")
                .operator("审核人员").description("初审通过")
                .build();
        t2.setCreatedAt(LocalDateTime.of(2024, 1, 2, 10, 0));

        when(applicationTimelineRepository.findByApplicationIdOrderByCreatedAtAsc(10L))
                .thenReturn(List.of(t1, t2));

        List<ApplicationTimeline> timeline = timelineService.getTimeline(10L);

        assertEquals(2, timeline.size());
        assertEquals("PENDING_INITIAL_REVIEW", timeline.get(0).getStatus());
        assertEquals("INITIAL_REVIEW_PASSED", timeline.get(1).getStatus());
        assertTrue(timeline.get(0).getCreatedAt().isBefore(timeline.get(1).getCreatedAt()));
    }

    @Test
    void getTimeline_shouldReturnEmptyListForNonExistentApplication() {
        when(applicationTimelineRepository.findByApplicationIdOrderByCreatedAtAsc(999L))
                .thenReturn(List.of());

        List<ApplicationTimeline> timeline = timelineService.getTimeline(999L);

        assertTrue(timeline.isEmpty());
    }
}
