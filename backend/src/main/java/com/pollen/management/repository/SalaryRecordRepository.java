package com.pollen.management.repository;

import com.pollen.management.entity.SalaryRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalaryRecordRepository extends JpaRepository<SalaryRecord, Long> {
    List<SalaryRecord> findByUserId(Long userId);
    List<SalaryRecord> findByArchivedFalse();
    List<SalaryRecord> findByUserIdAndArchivedTrueOrderByArchivedAtDesc(Long userId);
    List<SalaryRecord> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** 按周期查询所有薪资记录 */
    List<SalaryRecord> findByPeriod(String period);

    /** 按周期查询未归档的薪资记录 */
    List<SalaryRecord> findByPeriodAndArchivedFalse(String period);

    /** 查询某用户在某周期的记录（唯一约束保证最多一条） */
    Optional<SalaryRecord> findByUserIdAndPeriod(Long userId, String period);

    /** 查询所有不同的周期值，按时间倒序排列 */
    @Query("SELECT DISTINCT sr.period FROM SalaryRecord sr ORDER BY sr.period DESC")
    List<String> findDistinctPeriods();

    /** 查询所有不同的未归档周期，按时间倒序排列 */
    @Query("SELECT DISTINCT sr.period FROM SalaryRecord sr WHERE sr.archived = false ORDER BY sr.period DESC")
    List<String> findDistinctActivePeriods();

    /** 按周期查询记录数量 */
    long countByPeriod(String period);

    /** 检查某周期是否存在已归档记录 */
    boolean existsByPeriodAndArchivedTrue(String period);
}
