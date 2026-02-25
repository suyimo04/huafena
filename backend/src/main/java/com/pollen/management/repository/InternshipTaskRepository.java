package com.pollen.management.repository;

import com.pollen.management.entity.InternshipTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InternshipTaskRepository extends JpaRepository<InternshipTask, Long> {

    List<InternshipTask> findByInternshipId(Long internshipId);

    long countByInternshipIdAndCompleted(Long internshipId, Boolean completed);
}
