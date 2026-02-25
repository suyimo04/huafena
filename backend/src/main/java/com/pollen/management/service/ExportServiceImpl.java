package com.pollen.management.service;

import com.pollen.management.entity.Activity;
import com.pollen.management.entity.PointsRecord;
import com.pollen.management.entity.SalaryRecord;
import com.pollen.management.entity.User;
import com.pollen.management.repository.ActivityRepository;
import com.pollen.management.repository.PointsRecordRepository;
import com.pollen.management.repository.SalaryRecordRepository;
import com.pollen.management.repository.UserRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExportServiceImpl implements ExportService {

    private final UserRepository userRepository;
    private final PointsRecordRepository pointsRecordRepository;
    private final SalaryRecordRepository salaryRecordRepository;
    private final ActivityRepository activityRepository;

    public ExportServiceImpl(UserRepository userRepository,
                             PointsRecordRepository pointsRecordRepository,
                             SalaryRecordRepository salaryRecordRepository,
                             ActivityRepository activityRepository) {
        this.userRepository = userRepository;
        this.pointsRecordRepository = pointsRecordRepository;
        this.salaryRecordRepository = salaryRecordRepository;
        this.activityRepository = activityRepository;
    }

    @Override
    public byte[] exportMembers() {
        List<User> users = userRepository.findAll();
        return buildMembersExcel(users);
    }

    @Override
    public byte[] exportPoints() {
        List<PointsRecord> records = pointsRecordRepository.findAll();
        return buildPointsExcel(records);
    }

    @Override
    public byte[] exportSalary() {
        List<SalaryRecord> records = salaryRecordRepository.findAll();
        return buildSalaryExcel(records);
    }

    @Override
    public byte[] exportActivities() {
        List<Activity> activities = activityRepository.findAll();
        return buildActivitiesExcel(activities);
    }

    @Override
    public byte[] exportWithDateRange(String dataType, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        return switch (dataType.toLowerCase()) {
            case "members" -> {
                List<User> users = userRepository.findAll().stream()
                        .filter(u -> u.getCreatedAt() != null
                                && !u.getCreatedAt().isBefore(start)
                                && !u.getCreatedAt().isAfter(end))
                        .toList();
                yield buildMembersExcel(users);
            }
            case "points" -> {
                List<PointsRecord> records = pointsRecordRepository.findAll().stream()
                        .filter(r -> r.getCreatedAt() != null
                                && !r.getCreatedAt().isBefore(start)
                                && !r.getCreatedAt().isAfter(end))
                        .toList();
                yield buildPointsExcel(records);
            }
            case "salary" -> {
                List<SalaryRecord> records = salaryRecordRepository.findAll().stream()
                        .filter(r -> r.getCreatedAt() != null
                                && !r.getCreatedAt().isBefore(start)
                                && !r.getCreatedAt().isAfter(end))
                        .toList();
                yield buildSalaryExcel(records);
            }
            case "activities" -> {
                List<Activity> activities = activityRepository.findAll().stream()
                        .filter(a -> a.getCreatedAt() != null
                                && !a.getCreatedAt().isBefore(start)
                                && !a.getCreatedAt().isAfter(end))
                        .toList();
                yield buildActivitiesExcel(activities);
            }
            default -> throw new IllegalArgumentException("Unsupported data type: " + dataType);
        };
    }

    @Override
    public String generateFileName(String dataType, LocalDate startDate, LocalDate endDate) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");
        if (startDate != null && endDate != null) {
            return dataType + "_" + startDate.format(fmt) + "_" + endDate.format(fmt) + ".xlsx";
        }
        return dataType + "_" + LocalDate.now().format(fmt) + ".xlsx";
    }

    // --- Excel builders ---

    private byte[] buildMembersExcel(List<User> users) {
        String[] headers = {"ID", "用户名", "角色", "状态", "在线状态", "创建时间"};
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("成员列表");
            CellStyle headerStyle = createHeaderStyle(workbook);
            createHeaderRow(sheet, headers, headerStyle);

            int rowIdx = 1;
            for (User user : users) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(user.getId() != null ? user.getId() : 0);
                row.createCell(1).setCellValue(user.getUsername() != null ? user.getUsername() : "");
                row.createCell(2).setCellValue(user.getRole() != null ? user.getRole().name() : "");
                row.createCell(3).setCellValue(Boolean.TRUE.equals(user.getEnabled()) ? "启用" : "禁用");
                row.createCell(4).setCellValue(user.getOnlineStatus() != null ? user.getOnlineStatus().name() : "");
                row.createCell(5).setCellValue(user.getCreatedAt() != null ? user.getCreatedAt().toString() : "");
            }

            autoSizeColumns(sheet, headers.length);
            return toByteArray(workbook);
        } catch (IOException e) {
            throw new RuntimeException("Failed to export members Excel", e);
        }
    }

    private byte[] buildPointsExcel(List<PointsRecord> records) {
        String[] headers = {"ID", "用户ID", "积分类型", "数额", "描述", "创建时间"};
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("积分记录");
            CellStyle headerStyle = createHeaderStyle(workbook);
            createHeaderRow(sheet, headers, headerStyle);

            int rowIdx = 1;
            for (PointsRecord record : records) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(record.getId() != null ? record.getId() : 0);
                row.createCell(1).setCellValue(record.getUserId() != null ? record.getUserId() : 0);
                row.createCell(2).setCellValue(record.getPointsType() != null ? record.getPointsType().name() : "");
                row.createCell(3).setCellValue(record.getAmount() != null ? record.getAmount() : 0);
                row.createCell(4).setCellValue(record.getDescription() != null ? record.getDescription() : "");
                row.createCell(5).setCellValue(record.getCreatedAt() != null ? record.getCreatedAt().toString() : "");
            }

            autoSizeColumns(sheet, headers.length);
            return toByteArray(workbook);
        } catch (IOException e) {
            throw new RuntimeException("Failed to export points Excel", e);
        }
    }

    private byte[] buildSalaryExcel(List<SalaryRecord> records) {
        String[] headers = {"ID", "用户ID", "基础积分", "奖励积分", "扣减", "总积分", "迷你币", "薪资金额", "备注", "创建时间"};
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("薪资记录");
            CellStyle headerStyle = createHeaderStyle(workbook);
            createHeaderRow(sheet, headers, headerStyle);

            int rowIdx = 1;
            for (SalaryRecord record : records) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(record.getId() != null ? record.getId() : 0);
                row.createCell(1).setCellValue(record.getUserId() != null ? record.getUserId() : 0);
                row.createCell(2).setCellValue(record.getBasePoints() != null ? record.getBasePoints() : 0);
                row.createCell(3).setCellValue(record.getBonusPoints() != null ? record.getBonusPoints() : 0);
                row.createCell(4).setCellValue(record.getDeductions() != null ? record.getDeductions() : 0);
                row.createCell(5).setCellValue(record.getTotalPoints() != null ? record.getTotalPoints() : 0);
                row.createCell(6).setCellValue(record.getMiniCoins() != null ? record.getMiniCoins() : 0);
                row.createCell(7).setCellValue(record.getSalaryAmount() != null ? record.getSalaryAmount().doubleValue() : 0);
                row.createCell(8).setCellValue(record.getRemark() != null ? record.getRemark() : "");
                row.createCell(9).setCellValue(record.getCreatedAt() != null ? record.getCreatedAt().toString() : "");
            }

            autoSizeColumns(sheet, headers.length);
            return toByteArray(workbook);
        } catch (IOException e) {
            throw new RuntimeException("Failed to export salary Excel", e);
        }
    }

    private byte[] buildActivitiesExcel(List<Activity> activities) {
        String[] headers = {"ID", "活动名称", "描述", "活动类型", "活动时间", "地点", "报名人数", "状态", "创建时间"};
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("活动记录");
            CellStyle headerStyle = createHeaderStyle(workbook);
            createHeaderRow(sheet, headers, headerStyle);

            int rowIdx = 1;
            for (Activity activity : activities) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(activity.getId() != null ? activity.getId() : 0);
                row.createCell(1).setCellValue(activity.getName() != null ? activity.getName() : "");
                row.createCell(2).setCellValue(activity.getDescription() != null ? activity.getDescription() : "");
                row.createCell(3).setCellValue(activity.getActivityType() != null ? activity.getActivityType().name() : "");
                row.createCell(4).setCellValue(activity.getActivityTime() != null ? activity.getActivityTime().toString() : "");
                row.createCell(5).setCellValue(activity.getLocation() != null ? activity.getLocation() : "");
                row.createCell(6).setCellValue(activity.getRegistrationCount() != null ? activity.getRegistrationCount() : 0);
                row.createCell(7).setCellValue(activity.getStatus() != null ? activity.getStatus().name() : "");
                row.createCell(8).setCellValue(activity.getCreatedAt() != null ? activity.getCreatedAt().toString() : "");
            }

            autoSizeColumns(sheet, headers.length);
            return toByteArray(workbook);
        } catch (IOException e) {
            throw new RuntimeException("Failed to export activities Excel", e);
        }
    }

    // --- Utility methods ---

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private void createHeaderRow(Sheet sheet, String[] headers, CellStyle style) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private byte[] toByteArray(Workbook workbook) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
