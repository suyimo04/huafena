package com.pollen.management.controller;

import com.pollen.management.dto.AddPointsRequest;
import com.pollen.management.dto.ApiResponse;
import com.pollen.management.dto.DeductPointsRequest;
import com.pollen.management.entity.PointsRecord;
import com.pollen.management.entity.enums.PointsType;
import com.pollen.management.service.PointsService;
import com.pollen.management.util.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointsControllerTest {

    @Mock
    private PointsService pointsService;

    @InjectMocks
    private PointsController controller;

    // --- POST /api/points/add ---

    @Test
    void addPoints_shouldDelegateToServiceAndReturnSuccess() {
        var request = AddPointsRequest.builder()
                .userId(1L)
                .pointsType(PointsType.TASK_COMPLETION)
                .amount(5)
                .description("完成日常任务")
                .build();
        var record = PointsRecord.builder()
                .id(10L)
                .userId(1L)
                .pointsType(PointsType.TASK_COMPLETION)
                .amount(5)
                .description("完成日常任务")
                .createdAt(LocalDateTime.now())
                .build();
        when(pointsService.addPoints(1L, PointsType.TASK_COMPLETION, 5, "完成日常任务"))
                .thenReturn(record);

        ApiResponse<PointsRecord> response = controller.addPoints(request);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getId()).isEqualTo(10L);
        assertThat(response.getData().getUserId()).isEqualTo(1L);
        assertThat(response.getData().getPointsType()).isEqualTo(PointsType.TASK_COMPLETION);
        assertThat(response.getData().getAmount()).isEqualTo(5);
        verify(pointsService).addPoints(1L, PointsType.TASK_COMPLETION, 5, "完成日常任务");
    }

    @Test
    void addPoints_userNotFound_shouldPropagateException() {
        var request = AddPointsRequest.builder()
                .userId(99L)
                .pointsType(PointsType.ANNOUNCEMENT)
                .amount(5)
                .description("发布公告")
                .build();
        when(pointsService.addPoints(99L, PointsType.ANNOUNCEMENT, 5, "发布公告"))
                .thenThrow(new BusinessException(404, "用户不存在"));

        assertThatThrownBy(() -> controller.addPoints(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户不存在");
    }

    @Test
    void addPoints_invalidAmountRange_shouldPropagateException() {
        var request = AddPointsRequest.builder()
                .userId(1L)
                .pointsType(PointsType.TASK_COMPLETION)
                .amount(50)
                .description("超出范围")
                .build();
        when(pointsService.addPoints(1L, PointsType.TASK_COMPLETION, 50, "超出范围"))
                .thenThrow(new BusinessException(400, "完成任务积分范围为 1-10"));

        assertThatThrownBy(() -> controller.addPoints(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("完成任务积分范围为 1-10");
    }

    // --- POST /api/points/deduct ---

    @Test
    void deductPoints_shouldDelegateToServiceAndReturnSuccess() {
        var request = DeductPointsRequest.builder()
                .userId(2L)
                .pointsType(PointsType.CHECKIN)
                .amount(10)
                .description("签到扣分")
                .build();
        var record = PointsRecord.builder()
                .id(20L)
                .userId(2L)
                .pointsType(PointsType.CHECKIN)
                .amount(-10)
                .description("签到扣分")
                .createdAt(LocalDateTime.now())
                .build();
        when(pointsService.deductPoints(2L, PointsType.CHECKIN, 10, "签到扣分"))
                .thenReturn(record);

        ApiResponse<PointsRecord> response = controller.deductPoints(request);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getId()).isEqualTo(20L);
        assertThat(response.getData().getAmount()).isEqualTo(-10);
        verify(pointsService).deductPoints(2L, PointsType.CHECKIN, 10, "签到扣分");
    }

    @Test
    void deductPoints_userNotFound_shouldPropagateException() {
        var request = DeductPointsRequest.builder()
                .userId(99L)
                .pointsType(PointsType.CHECKIN)
                .amount(10)
                .description("扣分")
                .build();
        when(pointsService.deductPoints(99L, PointsType.CHECKIN, 10, "扣分"))
                .thenThrow(new BusinessException(404, "用户不存在"));

        assertThatThrownBy(() -> controller.deductPoints(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户不存在");
    }

    // --- GET /api/points/records/{userId} ---

    @Test
    void getPointsRecords_shouldReturnRecordsList() {
        var records = List.of(
                PointsRecord.builder().id(1L).userId(1L).pointsType(PointsType.TASK_COMPLETION)
                        .amount(5).description("任务1").createdAt(LocalDateTime.now()).build(),
                PointsRecord.builder().id(2L).userId(1L).pointsType(PointsType.ANNOUNCEMENT)
                        .amount(5).description("公告1").createdAt(LocalDateTime.now().minusDays(1)).build()
        );
        when(pointsService.getPointsRecords(1L)).thenReturn(records);

        ApiResponse<List<PointsRecord>> response = controller.getPointsRecords(1L);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).hasSize(2);
        assertThat(response.getData().get(0).getPointsType()).isEqualTo(PointsType.TASK_COMPLETION);
        verify(pointsService).getPointsRecords(1L);
    }

    @Test
    void getPointsRecords_emptyList_shouldReturnEmptySuccess() {
        when(pointsService.getPointsRecords(5L)).thenReturn(List.of());

        ApiResponse<List<PointsRecord>> response = controller.getPointsRecords(5L);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isEmpty();
    }

    @Test
    void getPointsRecords_userNotFound_shouldPropagateException() {
        when(pointsService.getPointsRecords(99L))
                .thenThrow(new BusinessException(404, "用户不存在"));

        assertThatThrownBy(() -> controller.getPointsRecords(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户不存在");
    }

    // --- GET /api/points/total/{userId} ---

    @Test
    void getTotalPoints_shouldReturnTotalSum() {
        when(pointsService.getTotalPoints(1L)).thenReturn(85);

        ApiResponse<Integer> response = controller.getTotalPoints(1L);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isEqualTo(85);
        verify(pointsService).getTotalPoints(1L);
    }

    @Test
    void getTotalPoints_noRecords_shouldReturnZero() {
        when(pointsService.getTotalPoints(5L)).thenReturn(0);

        ApiResponse<Integer> response = controller.getTotalPoints(5L);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isEqualTo(0);
    }

    @Test
    void getTotalPoints_userNotFound_shouldPropagateException() {
        when(pointsService.getTotalPoints(99L))
                .thenThrow(new BusinessException(404, "用户不存在"));

        assertThatThrownBy(() -> controller.getTotalPoints(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户不存在");
    }

    // --- GET /api/points/checkin-calculate ---

    @Test
    void calculateCheckinPoints_lessThan20_shouldReturnNegative20() {
        when(pointsService.calculateCheckinPoints(15)).thenReturn(-20);

        ApiResponse<Integer> response = controller.calculateCheckinPoints(15);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isEqualTo(-20);
    }

    @Test
    void calculateCheckinPoints_between20And29_shouldReturnNegative10() {
        when(pointsService.calculateCheckinPoints(25)).thenReturn(-10);

        ApiResponse<Integer> response = controller.calculateCheckinPoints(25);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isEqualTo(-10);
    }

    @Test
    void calculateCheckinPoints_between30And39_shouldReturnZero() {
        when(pointsService.calculateCheckinPoints(35)).thenReturn(0);

        ApiResponse<Integer> response = controller.calculateCheckinPoints(35);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isEqualTo(0);
    }

    @Test
    void calculateCheckinPoints_between40And49_shouldReturn30() {
        when(pointsService.calculateCheckinPoints(45)).thenReturn(30);

        ApiResponse<Integer> response = controller.calculateCheckinPoints(45);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isEqualTo(30);
    }

    @Test
    void calculateCheckinPoints_50OrMore_shouldReturn50() {
        when(pointsService.calculateCheckinPoints(55)).thenReturn(50);

        ApiResponse<Integer> response = controller.calculateCheckinPoints(55);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isEqualTo(50);
    }

    // --- GET /api/points/convert ---

    @Test
    void convertPointsToMiniCoins_shouldReturnDoubleValue() {
        when(pointsService.convertPointsToMiniCoins(100)).thenReturn(200);

        ApiResponse<Integer> response = controller.convertPointsToMiniCoins(100);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isEqualTo(200);
    }

    @Test
    void convertPointsToMiniCoins_zeroPoints_shouldReturnZero() {
        when(pointsService.convertPointsToMiniCoins(0)).thenReturn(0);

        ApiResponse<Integer> response = controller.convertPointsToMiniCoins(0);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isEqualTo(0);
    }
}
