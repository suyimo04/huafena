package com.pollen.management.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pollen.management.dto.ApiResponse;
import com.pollen.management.dto.CheckinTier;
import com.pollen.management.service.SalaryConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalaryConfigControllerTest {

    @Mock
    private SalaryConfigService salaryConfigService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SalaryConfigController controller;

    // --- GET /api/salary-config ---

    @Test
    void getAllConfig_shouldReturnAllConfigEntries() {
        Map<String, String> config = Map.of(
                "salary_pool_total", "2000",
                "formal_member_count", "5",
                "mini_coins_min", "200"
        );
        when(salaryConfigService.getAllConfig()).thenReturn(config);

        ApiResponse<Map<String, String>> response = controller.getAllConfig();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).hasSize(3);
        assertThat(response.getData().get("salary_pool_total")).isEqualTo("2000");
        verify(salaryConfigService).getAllConfig();
    }

    @Test
    void getAllConfig_empty_shouldReturnEmptyMap() {
        when(salaryConfigService.getAllConfig()).thenReturn(Map.of());

        ApiResponse<Map<String, String>> response = controller.getAllConfig();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isEmpty();
    }

    // --- PUT /api/salary-config ---

    @Test
    void updateConfig_validConfig_shouldSucceed() {
        Map<String, String> configMap = Map.of(
                "salary_pool_total", "3000",
                "mini_coins_min", "100"
        );

        ApiResponse<Void> response = controller.updateConfig(configMap);

        assertThat(response.getCode()).isEqualTo(200);
        verify(salaryConfigService).saveConfig(configMap);
    }

    @Test
    void updateConfig_invalidConfig_shouldPropagateException() {
        Map<String, String> configMap = Map.of(
                "mini_coins_min", "500",
                "mini_coins_max", "200"
        );
        doThrow(new IllegalArgumentException("个人最低迷你币(500)不能大于个人最高迷你币(200)"))
                .when(salaryConfigService).saveConfig(configMap);

        assertThatThrownBy(() -> controller.updateConfig(configMap))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("个人最低迷你币");
    }

    // --- GET /api/salary-config/checkin-tiers ---

    @Test
    void getCheckinTiers_shouldReturnTierList() {
        List<CheckinTier> tiers = List.of(
                CheckinTier.builder().minCount(0).maxCount(19).points(-20).label("不合格").build(),
                CheckinTier.builder().minCount(20).maxCount(29).points(-10).label("需改进").build(),
                CheckinTier.builder().minCount(30).maxCount(39).points(0).label("合格").build(),
                CheckinTier.builder().minCount(40).maxCount(49).points(30).label("良好").build(),
                CheckinTier.builder().minCount(50).maxCount(999).points(50).label("优秀").build()
        );
        when(salaryConfigService.getCheckinTiers()).thenReturn(tiers);

        ApiResponse<List<CheckinTier>> response = controller.getCheckinTiers();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).hasSize(5);
        assertThat(response.getData().get(0).getLabel()).isEqualTo("不合格");
        assertThat(response.getData().get(4).getPoints()).isEqualTo(50);
        verify(salaryConfigService).getCheckinTiers();
    }

    @Test
    void getCheckinTiers_empty_shouldReturnEmptyList() {
        when(salaryConfigService.getCheckinTiers()).thenReturn(List.of());

        ApiResponse<List<CheckinTier>> response = controller.getCheckinTiers();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isEmpty();
    }

    // --- PUT /api/salary-config/checkin-tiers ---

    @Test
    void updateCheckinTiers_validTiers_shouldSucceed() throws JsonProcessingException {
        List<CheckinTier> tiers = List.of(
                CheckinTier.builder().minCount(0).maxCount(14).points(-30).label("不合格").build(),
                CheckinTier.builder().minCount(15).maxCount(29).points(-10).label("需改进").build()
        );
        String json = "[{\"minCount\":0,\"maxCount\":14,\"points\":-30,\"label\":\"不合格\"}]";
        when(objectMapper.writeValueAsString(tiers)).thenReturn(json);

        ApiResponse<Void> response = controller.updateCheckinTiers(tiers);

        assertThat(response.getCode()).isEqualTo(200);
        verify(salaryConfigService).saveConfig(Map.of("checkin_tiers", json));
    }

    @Test
    void updateCheckinTiers_serializationFailure_shouldReturn400() throws JsonProcessingException {
        List<CheckinTier> tiers = List.of(
                CheckinTier.builder().minCount(0).maxCount(19).points(-20).label("不合格").build()
        );
        when(objectMapper.writeValueAsString(tiers))
                .thenThrow(new JsonProcessingException("mock error") {});

        ApiResponse<Void> response = controller.updateCheckinTiers(tiers);

        assertThat(response.getCode()).isEqualTo(400);
        assertThat(response.getMessage()).contains("签到奖惩表序列化失败");
        verify(salaryConfigService, never()).saveConfig(any());
    }

    @Test
    void updateCheckinTiers_configValidationFailure_shouldPropagateException() throws JsonProcessingException {
        List<CheckinTier> tiers = List.of(
                CheckinTier.builder().minCount(0).maxCount(19).points(-20).label("不合格").build()
        );
        String json = "[{\"minCount\":0,\"maxCount\":19,\"points\":-20,\"label\":\"不合格\"}]";
        when(objectMapper.writeValueAsString(tiers)).thenReturn(json);
        doThrow(new IllegalArgumentException("配置校验失败"))
                .when(salaryConfigService).saveConfig(Map.of("checkin_tiers", json));

        assertThatThrownBy(() -> controller.updateCheckinTiers(tiers))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("配置校验失败");
    }
}
