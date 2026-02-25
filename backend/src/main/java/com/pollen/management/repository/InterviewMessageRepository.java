package com.pollen.management.repository;

import com.pollen.management.entity.InterviewMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewMessageRepository extends JpaRepository<InterviewMessage, Long> {
    List<InterviewMessage> findByInterviewIdOrderByTimestamp(Long interviewId);
}
