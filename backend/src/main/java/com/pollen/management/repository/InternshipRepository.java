package com.pollen.management.repository;

import com.pollen.management.entity.Internship;
import com.pollen.management.entity.enums.InternshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InternshipRepository extends JpaRepository<Internship, Long> {

    Optional<Internship> findByUserId(Long userId);

    List<Internship> findByStatus(InternshipStatus status);

    List<Internship> findByMentorId(Long mentorId);
}
