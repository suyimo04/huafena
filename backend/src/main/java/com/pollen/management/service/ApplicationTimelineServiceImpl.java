package com.pollen.management.service;

import com.pollen.management.entity.ApplicationTimeline;
import com.pollen.management.repository.ApplicationTimelineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationTimelineServiceImpl implements ApplicationTimelineService {

    private final ApplicationTimelineRepository applicationTimelineRepository;

    @Override
    public void recordTimelineEvent(Long applicationId, String status, String operator, String description) {
        ApplicationTimeline timeline = ApplicationTimeline.builder()
                .applicationId(applicationId)
                .status(status)
                .operator(operator)
                .description(description)
                .build();
        applicationTimelineRepository.save(timeline);
    }

    @Override
    public List<ApplicationTimeline> getTimeline(Long applicationId) {
        return applicationTimelineRepository.findByApplicationIdOrderByCreatedAtAsc(applicationId);
    }
}
