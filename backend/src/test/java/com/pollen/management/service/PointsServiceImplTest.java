package com.pollen.management.service;

import com.pollen.management.entity.PointsRecord;
import com.pollen.management.entity.enums.PointsType;
import com.pollen.management.repository.PointsRecordRepository;
import com.pollen.management.repository.UserRepository;
import com.pollen.management.util.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointsServiceImplTest {

    @Mock
    private PointsRecordRepository pointsRecordRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PointsServiceImpl pointsService;

    // --- addPoints tests ---

    @Test
    void addPoints_shouldCreatePositiveRecord() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(pointsRecordRepository.save(any(PointsRecord.class))).thenAnswer(inv -> {
            PointsRecord r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        PointsRecord result = pointsService.addPoints(1L, PointsType.TASK_COMPLETION, 5, "完成日常任务");

        assertEquals(1L, result.getUserId());
        assertEquals(PointsType.TASK_COMPLETION, result.getPointsType());
        assertEquals(5, result.getAmount());
        assertEquals("完成日常任务", result.getDescription());
    }

    @Test
    void addPoints_shouldRejectZeroAmount() {
        when(userRepository.existsById(1L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pointsService.addPoints(1L, PointsType.TASK_COMPLETION, 0, "test"));
        assertEquals(400, ex.getCode());
    }

    @Test
    void addPoints_shouldRejectNegativeAmount() {
        when(userRepository.existsById(1L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pointsService.addPoints(1L, PointsType.TASK_COMPLETION, -5, "test"));
        assertEquals(400, ex.getCode());
    }

    @Test
    void addPoints_shouldRejectNonExistentUser() {
        when(userRepository.existsById(99L)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pointsService.addPoints(99L, PointsType.TASK_COMPLETION, 5, "test"));
        assertEquals(404, ex.getCode());
        assertEquals("用户不存在", ex.getMessage());
    }

    // --- deductPoints tests ---

    @Test
    void deductPoints_shouldCreateNegativeRecord() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(pointsRecordRepository.save(any(PointsRecord.class))).thenAnswer(inv -> {
            PointsRecord r = inv.getArgument(0);
            r.setId(2L);
            return r;
        });

        PointsRecord result = pointsService.deductPoints(1L, PointsType.CHECKIN, 20, "签到不足扣分");

        assertEquals(1L, result.getUserId());
        assertEquals(PointsType.CHECKIN, result.getPointsType());
        assertEquals(-20, result.getAmount());
        assertEquals("签到不足扣分", result.getDescription());
    }

    @Test
    void deductPoints_shouldRejectZeroAmount() {
        when(userRepository.existsById(1L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pointsService.deductPoints(1L, PointsType.CHECKIN, 0, "test"));
        assertEquals(400, ex.getCode());
    }

    @Test
    void deductPoints_shouldRejectNonExistentUser() {
        when(userRepository.existsById(99L)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pointsService.deductPoints(99L, PointsType.CHECKIN, 10, "test"));
        assertEquals(404, ex.getCode());
    }

    // --- getPointsRecords tests ---

    @Test
    void getPointsRecords_shouldReturnRecordsOrderedByTimeDesc() {
        when(userRepository.existsById(1L)).thenReturn(true);

        PointsRecord r1 = PointsRecord.builder().id(1L).userId(1L).pointsType(PointsType.TASK_COMPLETION)
                .amount(5).createdAt(LocalDateTime.of(2024, 1, 1, 10, 0)).build();
        PointsRecord r2 = PointsRecord.builder().id(2L).userId(1L).pointsType(PointsType.ANNOUNCEMENT)
                .amount(5).createdAt(LocalDateTime.of(2024, 1, 2, 10, 0)).build();

        when(pointsRecordRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(r2, r1));

        List<PointsRecord> records = pointsService.getPointsRecords(1L);

        assertEquals(2, records.size());
        assertEquals(2L, records.get(0).getId());
        assertEquals(1L, records.get(1).getId());
    }

    @Test
    void getPointsRecords_shouldReturnEmptyListForUserWithNoRecords() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(pointsRecordRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        List<PointsRecord> records = pointsService.getPointsRecords(1L);

        assertTrue(records.isEmpty());
    }

    @Test
    void getPointsRecords_shouldRejectNonExistentUser() {
        when(userRepository.existsById(99L)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pointsService.getPointsRecords(99L));
        assertEquals(404, ex.getCode());
    }

    // --- getTotalPoints tests ---

    @Test
    void getTotalPoints_shouldSumAllRecords() {
        when(userRepository.existsById(1L)).thenReturn(true);

        PointsRecord r1 = PointsRecord.builder().userId(1L).amount(50).build();
        PointsRecord r2 = PointsRecord.builder().userId(1L).amount(-20).build();
        PointsRecord r3 = PointsRecord.builder().userId(1L).amount(10).build();

        when(pointsRecordRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(r1, r2, r3));

        int total = pointsService.getTotalPoints(1L);

        assertEquals(40, total);
    }

    @Test
    void getTotalPoints_shouldReturnZeroForNoRecords() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(pointsRecordRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        int total = pointsService.getTotalPoints(1L);

        assertEquals(0, total);
    }

    @Test
    void getTotalPoints_shouldRejectNonExistentUser() {
        when(userRepository.existsById(99L)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pointsService.getTotalPoints(99L));
        assertEquals(404, ex.getCode());
    }

    // --- Amount range validation tests ---

    @Test
    void addPoints_communityActivity_shouldAcceptValidRange() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(pointsRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> pointsService.addPoints(1L, PointsType.COMMUNITY_ACTIVITY, 1, "活跃"));
        assertDoesNotThrow(() -> pointsService.addPoints(1L, PointsType.COMMUNITY_ACTIVITY, 100, "活跃"));
    }

    @Test
    void addPoints_communityActivity_shouldRejectOutOfRange() {
        when(userRepository.existsById(1L)).thenReturn(true);

        assertThrows(BusinessException.class,
                () -> pointsService.addPoints(1L, PointsType.COMMUNITY_ACTIVITY, 101, "超出范围"));
    }

    @Test
    void addPoints_violationHandling_shouldOnlyAcceptThree() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(pointsRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> pointsService.addPoints(1L, PointsType.VIOLATION_HANDLING, 3, "处理违规"));
        assertThrows(BusinessException.class,
                () -> pointsService.addPoints(1L, PointsType.VIOLATION_HANDLING, 5, "错误数额"));
    }

    @Test
    void addPoints_announcement_shouldOnlyAcceptFive() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(pointsRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> pointsService.addPoints(1L, PointsType.ANNOUNCEMENT, 5, "发布公告"));
        assertThrows(BusinessException.class,
                () -> pointsService.addPoints(1L, PointsType.ANNOUNCEMENT, 3, "错误数额"));
    }

    @Test
    void addPoints_birthdayBonus_shouldOnlyAcceptTwentyFive() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(pointsRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> pointsService.addPoints(1L, PointsType.BIRTHDAY_BONUS, 25, "生日福利"));
        assertThrows(BusinessException.class,
                () -> pointsService.addPoints(1L, PointsType.BIRTHDAY_BONUS, 10, "错误数额"));
    }

    @Test
    void addPoints_taskCompletion_shouldAcceptValidRange() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(pointsRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> pointsService.addPoints(1L, PointsType.TASK_COMPLETION, 1, "任务"));
        assertDoesNotThrow(() -> pointsService.addPoints(1L, PointsType.TASK_COMPLETION, 10, "任务"));
        assertThrows(BusinessException.class,
                () -> pointsService.addPoints(1L, PointsType.TASK_COMPLETION, 11, "超出范围"));
    }

    @Test
    void addPoints_eventHosting_shouldAcceptValidRange() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(pointsRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> pointsService.addPoints(1L, PointsType.EVENT_HOSTING, 5, "活动"));
        assertDoesNotThrow(() -> pointsService.addPoints(1L, PointsType.EVENT_HOSTING, 25, "活动"));
        assertThrows(BusinessException.class,
                () -> pointsService.addPoints(1L, PointsType.EVENT_HOSTING, 26, "超出范围"));
    }

    @Test
    void addPoints_monthlyExcellent_shouldAcceptValidRange() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(pointsRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> pointsService.addPoints(1L, PointsType.MONTHLY_EXCELLENT, 10, "优秀"));
        assertDoesNotThrow(() -> pointsService.addPoints(1L, PointsType.MONTHLY_EXCELLENT, 30, "优秀"));
        assertThrows(BusinessException.class,
                () -> pointsService.addPoints(1L, PointsType.MONTHLY_EXCELLENT, 31, "超出范围"));
    }

    @Test
    void addPoints_checkin_shouldAcceptValidRange() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(pointsRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> pointsService.addPoints(1L, PointsType.CHECKIN, 50, "签到"));
        assertThrows(BusinessException.class,
                () -> pointsService.addPoints(1L, PointsType.CHECKIN, 51, "超出范围"));
    }

    // --- Verify save is called with correct data ---

    @Test
    void addPoints_shouldSaveRecordWithCorrectFields() {
        when(userRepository.existsById(1L)).thenReturn(true);
        ArgumentCaptor<PointsRecord> captor = ArgumentCaptor.forClass(PointsRecord.class);
        when(pointsRecordRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        pointsService.addPoints(1L, PointsType.EVENT_HOSTING, 15, "举办活动");

        PointsRecord saved = captor.getValue();
        assertEquals(1L, saved.getUserId());
        assertEquals(PointsType.EVENT_HOSTING, saved.getPointsType());
        assertEquals(15, saved.getAmount());
        assertEquals("举办活动", saved.getDescription());
    }

    @Test
    void deductPoints_shouldSaveRecordWithNegativeAmount() {
        when(userRepository.existsById(1L)).thenReturn(true);
        ArgumentCaptor<PointsRecord> captor = ArgumentCaptor.forClass(PointsRecord.class);
        when(pointsRecordRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        pointsService.deductPoints(1L, PointsType.CHECKIN, 20, "签到不足");

        PointsRecord saved = captor.getValue();
        assertEquals(-20, saved.getAmount());
    }

    // --- calculateCheckinPoints tests ---

    @Test
    void calculateCheckinPoints_lessThan20_shouldReturnMinusTwenty() {
        assertEquals(-20, pointsService.calculateCheckinPoints(0));
        assertEquals(-20, pointsService.calculateCheckinPoints(10));
        assertEquals(-20, pointsService.calculateCheckinPoints(19));
    }

    @Test
    void calculateCheckinPoints_20to29_shouldReturnMinusTen() {
        assertEquals(-10, pointsService.calculateCheckinPoints(20));
        assertEquals(-10, pointsService.calculateCheckinPoints(25));
        assertEquals(-10, pointsService.calculateCheckinPoints(29));
    }

    @Test
    void calculateCheckinPoints_30to39_shouldReturnZero() {
        assertEquals(0, pointsService.calculateCheckinPoints(30));
        assertEquals(0, pointsService.calculateCheckinPoints(35));
        assertEquals(0, pointsService.calculateCheckinPoints(39));
    }

    @Test
    void calculateCheckinPoints_40to49_shouldReturnThirty() {
        assertEquals(30, pointsService.calculateCheckinPoints(40));
        assertEquals(30, pointsService.calculateCheckinPoints(45));
        assertEquals(30, pointsService.calculateCheckinPoints(49));
    }

    @Test
    void calculateCheckinPoints_50orMore_shouldReturnFifty() {
        assertEquals(50, pointsService.calculateCheckinPoints(50));
        assertEquals(50, pointsService.calculateCheckinPoints(60));
        assertEquals(50, pointsService.calculateCheckinPoints(100));
    }

    @Test
    void calculateCheckinPoints_boundaryValues() {
        // Exact boundary transitions
        assertEquals(-20, pointsService.calculateCheckinPoints(19));
        assertEquals(-10, pointsService.calculateCheckinPoints(20));
        assertEquals(-10, pointsService.calculateCheckinPoints(29));
        assertEquals(0, pointsService.calculateCheckinPoints(30));
        assertEquals(0, pointsService.calculateCheckinPoints(39));
        assertEquals(30, pointsService.calculateCheckinPoints(40));
        assertEquals(30, pointsService.calculateCheckinPoints(49));
        assertEquals(50, pointsService.calculateCheckinPoints(50));
    }

    @Test
    void calculateCheckinPoints_negativeInput_shouldReturnMinusTwenty() {
        assertEquals(-20, pointsService.calculateCheckinPoints(-1));
    }

    // --- convertPointsToMiniCoins tests ---

    @Test
    void convertPointsToMiniCoins_shouldReturnDoubleThePoints() {
        assertEquals(200, pointsService.convertPointsToMiniCoins(100));
        assertEquals(10, pointsService.convertPointsToMiniCoins(5));
        assertEquals(2, pointsService.convertPointsToMiniCoins(1));
    }

    @Test
    void convertPointsToMiniCoins_zeroPoints_shouldReturnZero() {
        assertEquals(0, pointsService.convertPointsToMiniCoins(0));
    }

    @Test
    void convertPointsToMiniCoins_negativePoints_shouldReturnNegativeDouble() {
        assertEquals(-40, pointsService.convertPointsToMiniCoins(-20));
    }

    @Test
    void convertPointsToMiniCoins_largeValue_shouldReturnCorrectResult() {
        assertEquals(2000, pointsService.convertPointsToMiniCoins(1000));
    }
}
