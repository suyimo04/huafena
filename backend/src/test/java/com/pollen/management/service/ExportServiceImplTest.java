package com.pollen.management.service;

import com.pollen.management.entity.Activity;
import com.pollen.management.entity.PointsRecord;
import com.pollen.management.entity.SalaryRecord;
import com.pollen.management.entity.User;
import com.pollen.management.entity.enums.*;
import com.pollen.management.repository.ActivityRepository;
import com.pollen.management.repository.PointsRecordRepository;
import com.pollen.management.repository.SalaryRecordRepository;
import com.pollen.management.repository.UserRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PointsRecordRepository pointsRecordRepository;
    @Mock
    private SalaryRecordRepository salaryRecordRepository;
    @Mock
    private ActivityRepository activityRepository;

    @InjectMocks
    private ExportServiceImpl exportService;

    private User testUser;
    private PointsRecord testPointsRecord;
    private SalaryRecord testSalaryRecord;
    private Activity testActivity;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .password("encoded")
                .role(Role.MEMBER)
                .enabled(true)
                .onlineStatus(OnlineStatus.ONLINE)
                .build();
        testUser.setCreatedAt(LocalDateTime.of(2024, 1, 15, 10, 0));

        testPointsRecord = PointsRecord.builder()
                .id(1L)
                .userId(1L)
                .pointsType(PointsType.TASK_COMPLETION)
                .amount(10)
                .description("完成任务")
                .build();
        testPointsRecord.setCreatedAt(LocalDateTime.of(2024, 1, 15, 10, 0));

        testSalaryRecord = SalaryRecord.builder()
                .id(1L)
                .userId(1L)
                .basePoints(100)
                .bonusPoints(20)
                .deductions(5)
                .totalPoints(115)
                .miniCoins(230)
                .salaryAmount(new BigDecimal("300.00"))
                .remark("月度薪资")
                .build();
        testSalaryRecord.setCreatedAt(LocalDateTime.of(2024, 1, 15, 10, 0));

        testActivity = Activity.builder()
                .id(1L)
                .name("团建活动")
                .description("年度团建")
                .activityType(ActivityType.TEAM_BUILDING)
                .activityTime(LocalDateTime.of(2024, 2, 1, 14, 0))
                .location("会议室A")
                .registrationCount(10)
                .status(ActivityStatus.UPCOMING)
                .createdBy(1L)
                .build();
        testActivity.setCreatedAt(LocalDateTime.of(2024, 1, 10, 10, 0));
    }

    @Test
    void exportMembers_returnsValidExcel() throws Exception {
        when(userRepository.findAll()).thenReturn(List.of(testUser));

        byte[] result = exportService.exportMembers();

        assertNotNull(result);
        assertTrue(result.length > 0);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = wb.getSheetAt(0);
            assertEquals("成员列表", sheet.getSheetName());
            // Header row
            Row header = sheet.getRow(0);
            assertEquals("ID", header.getCell(0).getStringCellValue());
            assertEquals("用户名", header.getCell(1).getStringCellValue());
            assertEquals("角色", header.getCell(2).getStringCellValue());
            // Data row
            Row dataRow = sheet.getRow(1);
            assertEquals(1.0, dataRow.getCell(0).getNumericCellValue());
            assertEquals("testuser", dataRow.getCell(1).getStringCellValue());
            assertEquals("MEMBER", dataRow.getCell(2).getStringCellValue());
            assertEquals("启用", dataRow.getCell(3).getStringCellValue());
        }
    }

    @Test
    void exportMembers_emptyList_returnsExcelWithHeaderOnly() throws Exception {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        byte[] result = exportService.exportMembers();

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = wb.getSheetAt(0);
            assertNotNull(sheet.getRow(0)); // header exists
            assertNull(sheet.getRow(1));    // no data rows
        }
    }

    @Test
    void exportPoints_returnsValidExcel() throws Exception {
        when(pointsRecordRepository.findAll()).thenReturn(List.of(testPointsRecord));

        byte[] result = exportService.exportPoints();

        assertNotNull(result);
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = wb.getSheetAt(0);
            assertEquals("积分记录", sheet.getSheetName());
            Row dataRow = sheet.getRow(1);
            assertEquals(1.0, dataRow.getCell(0).getNumericCellValue());
            assertEquals("TASK_COMPLETION", dataRow.getCell(2).getStringCellValue());
            assertEquals(10.0, dataRow.getCell(3).getNumericCellValue());
            assertEquals("完成任务", dataRow.getCell(4).getStringCellValue());
        }
    }

    @Test
    void exportSalary_returnsValidExcel() throws Exception {
        when(salaryRecordRepository.findAll()).thenReturn(List.of(testSalaryRecord));

        byte[] result = exportService.exportSalary();

        assertNotNull(result);
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = wb.getSheetAt(0);
            assertEquals("薪资记录", sheet.getSheetName());
            Row header = sheet.getRow(0);
            assertEquals("基础积分", header.getCell(2).getStringCellValue());
            Row dataRow = sheet.getRow(1);
            assertEquals(100.0, dataRow.getCell(2).getNumericCellValue());
            assertEquals(230.0, dataRow.getCell(6).getNumericCellValue());
            assertEquals(300.0, dataRow.getCell(7).getNumericCellValue());
        }
    }

    @Test
    void exportActivities_returnsValidExcel() throws Exception {
        when(activityRepository.findAll()).thenReturn(List.of(testActivity));

        byte[] result = exportService.exportActivities();

        assertNotNull(result);
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = wb.getSheetAt(0);
            assertEquals("活动记录", sheet.getSheetName());
            Row dataRow = sheet.getRow(1);
            assertEquals("团建活动", dataRow.getCell(1).getStringCellValue());
            assertEquals("TEAM_BUILDING", dataRow.getCell(3).getStringCellValue());
            assertEquals("会议室A", dataRow.getCell(5).getStringCellValue());
            assertEquals(10.0, dataRow.getCell(6).getNumericCellValue());
        }
    }

    @Test
    void exportWithDateRange_members_filtersCorrectly() throws Exception {
        User oldUser = User.builder()
                .id(2L).username("olduser").password("x").role(Role.INTERN).enabled(false).build();
        oldUser.setCreatedAt(LocalDateTime.of(2023, 6, 1, 10, 0));

        when(userRepository.findAll()).thenReturn(List.of(testUser, oldUser));

        byte[] result = exportService.exportWithDateRange("members",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = wb.getSheetAt(0);
            assertNotNull(sheet.getRow(1));  // testUser (2024-01-15) included
            assertNull(sheet.getRow(2));     // oldUser (2023-06-01) excluded
        }
    }

    @Test
    void exportWithDateRange_points_filtersCorrectly() throws Exception {
        PointsRecord oldRecord = PointsRecord.builder()
                .id(2L).userId(1L).pointsType(PointsType.CHECKIN).amount(5).build();
        oldRecord.setCreatedAt(LocalDateTime.of(2023, 3, 1, 10, 0));

        when(pointsRecordRepository.findAll()).thenReturn(List.of(testPointsRecord, oldRecord));

        byte[] result = exportService.exportWithDateRange("points",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = wb.getSheetAt(0);
            assertNotNull(sheet.getRow(1));
            assertNull(sheet.getRow(2));
        }
    }

    @Test
    void exportWithDateRange_salary_filtersCorrectly() throws Exception {
        when(salaryRecordRepository.findAll()).thenReturn(List.of(testSalaryRecord));

        byte[] result = exportService.exportWithDateRange("salary",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = wb.getSheetAt(0);
            assertNotNull(sheet.getRow(1));
        }
    }

    @Test
    void exportWithDateRange_activities_filtersCorrectly() throws Exception {
        when(activityRepository.findAll()).thenReturn(List.of(testActivity));

        byte[] result = exportService.exportWithDateRange("activities",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = wb.getSheetAt(0);
            assertNotNull(sheet.getRow(1)); // testActivity created 2024-01-10
        }
    }

    @Test
    void exportWithDateRange_unsupportedType_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                exportService.exportWithDateRange("unknown", LocalDate.now(), LocalDate.now()));
    }

    @Test
    void generateFileName_withDateRange() {
        String name = exportService.generateFileName("members",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));
        assertEquals("members_20240101_20240131.xlsx", name);
    }

    @Test
    void generateFileName_withoutDateRange() {
        String name = exportService.generateFileName("points", null, null);
        String today = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        assertEquals("points_" + today + ".xlsx", name);
    }
}
