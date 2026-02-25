package com.pollen.management.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pollen.management.dto.CheckinTier;
import com.pollen.management.dto.RotationThresholds;
import com.pollen.management.entity.SalaryConfig;
import com.pollen.management.repository.SalaryConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SalaryConfigServiceImplTest {

    private SalaryConfigRepository repository;
    private SalaryConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(SalaryConfigRepository.class);
        service = new SalaryConfigServiceImpl(repository, new ObjectMapper());
    }

    // --- getAllConfig ---

    @Test
    void getAllConfig_returnsAllStoredConfigs() {
        when(repository.findAll()).thenReturn(List.of(
                SalaryConfig.builder().configKey("salary_pool_total").configValue("3000").build(),
                SalaryConfig.builder().configKey("formal_member_count").configValue("6").build()
        ));

        Map<String, String> result = service.getAllConfig();

        assertThat(result).hasSize(2);
        assertThat(result.get("salary_pool_total")).isEqualTo("3000");
        assertThat(result.get("formal_member_count")).isEqualTo("6");
    }

    @Test
    void getAllConfig_emptyDb_returnsEmptyMap() {
        when(repository.findAll()).thenReturn(Collections.emptyList());
        assertThat(service.getAllConfig()).isEmpty();
    }

    // --- getConfigValue ---

    @Test
    void getConfigValue_existingKey_returnsStoredValue() {
        when(repository.findByConfigKey("salary_pool_total"))
                .thenReturn(Optional.of(SalaryConfig.builder().configValue("3000").build()));

        assertThat(service.getConfigValue("salary_pool_total", "2000")).isEqualTo("3000");
    }

    @Test
    void getConfigValue_missingKey_returnsDefault() {
        when(repository.findByConfigKey("nonexistent")).thenReturn(Optional.empty());
        assertThat(service.getConfigValue("nonexistent", "fallback")).isEqualTo("fallback");
    }

    // --- getIntConfig ---

    @Test
    void getIntConfig_validNumber_returnsParsedValue() {
        when(repository.findByConfigKey("salary_pool_total"))
                .thenReturn(Optional.of(SalaryConfig.builder().configValue("3000").build()));

        assertThat(service.getIntConfig("salary_pool_total", 2000)).isEqualTo(3000);
    }

    @Test
    void getIntConfig_invalidNumber_returnsDefault() {
        when(repository.findByConfigKey("salary_pool_total"))
                .thenReturn(Optional.of(SalaryConfig.builder().configValue("not_a_number").build()));

        assertThat(service.getIntConfig("salary_pool_total", 2000)).isEqualTo(2000);
    }

    @Test
    void getIntConfig_missingKey_returnsDefault() {
        when(repository.findByConfigKey("missing")).thenReturn(Optional.empty());
        assertThat(service.getIntConfig("missing", 42)).isEqualTo(42);
    }

    // --- Convenience getters with defaults ---

    @Test
    void getSalaryPoolTotal_noConfig_returnsDefault() {
        when(repository.findByConfigKey("salary_pool_total")).thenReturn(Optional.empty());
        assertThat(service.getSalaryPoolTotal()).isEqualTo(2000);
    }

    @Test
    void getFormalMemberCount_noConfig_returnsDefault() {
        when(repository.findByConfigKey("formal_member_count")).thenReturn(Optional.empty());
        assertThat(service.getFormalMemberCount()).isEqualTo(5);
    }

    @Test
    void getMiniCoinsRange_noConfig_returnsDefaults() {
        when(repository.findByConfigKey("mini_coins_min")).thenReturn(Optional.empty());
        when(repository.findByConfigKey("mini_coins_max")).thenReturn(Optional.empty());
        assertThat(service.getMiniCoinsRange()).isEqualTo(new int[]{200, 400});
    }

    @Test
    void getPointsToCoinsRatio_noConfig_returnsDefault() {
        when(repository.findByConfigKey("points_to_coins_ratio")).thenReturn(Optional.empty());
        assertThat(service.getPointsToCoinsRatio()).isEqualTo(2);
    }

    // --- getCheckinTiers ---

    @Test
    void getCheckinTiers_noConfig_returnsDefaultTiers() {
        when(repository.findByConfigKey("checkin_tiers")).thenReturn(Optional.empty());

        List<CheckinTier> tiers = service.getCheckinTiers();

        assertThat(tiers).hasSize(5);
        assertThat(tiers.get(0).getMinCount()).isEqualTo(0);
        assertThat(tiers.get(0).getMaxCount()).isEqualTo(19);
        assertThat(tiers.get(0).getPoints()).isEqualTo(-20);
        assertThat(tiers.get(4).getMinCount()).isEqualTo(50);
        assertThat(tiers.get(4).getPoints()).isEqualTo(50);
    }

    @Test
    void getCheckinTiers_customConfig_parsesJson() {
        String json = "[{\"minCount\":0,\"maxCount\":10,\"points\":-30,\"label\":\"差\"}]";
        when(repository.findByConfigKey("checkin_tiers"))
                .thenReturn(Optional.of(SalaryConfig.builder().configValue(json).build()));

        List<CheckinTier> tiers = service.getCheckinTiers();

        assertThat(tiers).hasSize(1);
        assertThat(tiers.get(0).getPoints()).isEqualTo(-30);
    }

    @Test
    void getCheckinTiers_invalidJson_throwsIllegalState() {
        when(repository.findByConfigKey("checkin_tiers"))
                .thenReturn(Optional.of(SalaryConfig.builder().configValue("not json").build()));

        assertThatThrownBy(() -> service.getCheckinTiers())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("签到奖惩表配置 JSON 解析失败");
    }

    // --- getRotationThresholds ---

    @Test
    void getRotationThresholds_noConfig_returnsDefaults() {
        when(repository.findByConfigKey(anyString())).thenReturn(Optional.empty());

        RotationThresholds thresholds = service.getRotationThresholds();

        assertThat(thresholds.getPromotionPointsThreshold()).isEqualTo(100);
        assertThat(thresholds.getDemotionSalaryThreshold()).isEqualTo(150);
        assertThat(thresholds.getDemotionConsecutiveMonths()).isEqualTo(2);
        assertThat(thresholds.getDismissalPointsThreshold()).isEqualTo(100);
        assertThat(thresholds.getDismissalConsecutiveMonths()).isEqualTo(2);
    }

    // --- saveConfig validation ---

    @Test
    void saveConfig_validConfig_savesSuccessfully() {
        when(repository.findByConfigKey(anyString())).thenReturn(Optional.empty());

        Map<String, String> config = Map.of(
                "salary_pool_total", "2000",
                "formal_member_count", "5",
                "base_allocation", "400"
        );

        assertThatCode(() -> service.saveConfig(config)).doesNotThrowAnyException();
        verify(repository, times(3)).save(any(SalaryConfig.class));
    }

    @Test
    void saveConfig_minGreaterThanMax_throwsIllegalArgument() {
        when(repository.findByConfigKey(anyString())).thenReturn(Optional.empty());

        Map<String, String> config = Map.of(
                "mini_coins_min", "500",
                "mini_coins_max", "200"
        );

        assertThatThrownBy(() -> service.saveConfig(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("个人最低迷你币");
    }

    @Test
    void saveConfig_allocationExceedsPool_throwsIllegalArgument() {
        when(repository.findByConfigKey(anyString())).thenReturn(Optional.empty());

        Map<String, String> config = Map.of(
                "base_allocation", "500",
                "formal_member_count", "5",
                "salary_pool_total", "2000"
        );

        assertThatThrownBy(() -> service.saveConfig(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("超过薪酬池总额");
    }

    @Test
    void saveConfig_negativePromotionThreshold_throwsIllegalArgument() {
        when(repository.findByConfigKey(anyString())).thenReturn(Optional.empty());

        Map<String, String> config = Map.of("promotion_points_threshold", "-1");

        assertThatThrownBy(() -> service.saveConfig(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("转正积分阈值不能为负数");
    }

    @Test
    void saveConfig_negativeDemotionThreshold_throwsIllegalArgument() {
        when(repository.findByConfigKey(anyString())).thenReturn(Optional.empty());

        Map<String, String> config = Map.of("demotion_salary_threshold", "-10");

        assertThatThrownBy(() -> service.saveConfig(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("降级薪酬阈值不能为负数");
    }

    @Test
    void saveConfig_negativeDismissalThreshold_throwsIllegalArgument() {
        when(repository.findByConfigKey(anyString())).thenReturn(Optional.empty());

        Map<String, String> config = Map.of("dismissal_points_threshold", "-5");

        assertThatThrownBy(() -> service.saveConfig(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("开除积分阈值不能为负数");
    }

    @Test
    void saveConfig_updatesExistingConfig() {
        SalaryConfig existing = SalaryConfig.builder()
                .id(1L)
                .configKey("salary_pool_total")
                .configValue("2000")
                .build();
        when(repository.findByConfigKey("salary_pool_total")).thenReturn(Optional.of(existing));

        service.saveConfig(Map.of("salary_pool_total", "3000"));

        verify(repository).save(argThat(config ->
                config.getConfigValue().equals("3000") && config.getId().equals(1L)));
    }

    @Test
    void saveConfig_allocationEqualsPool_doesNotThrow() {
        when(repository.findByConfigKey(anyString())).thenReturn(Optional.empty());

        // 400 × 5 = 2000 = pool → should be OK (not strictly greater)
        Map<String, String> config = Map.of(
                "base_allocation", "400",
                "formal_member_count", "5",
                "salary_pool_total", "2000"
        );

        assertThatCode(() -> service.saveConfig(config)).doesNotThrowAnyException();
    }

    @Test
    void saveConfig_zeroThresholds_doesNotThrow() {
        when(repository.findByConfigKey(anyString())).thenReturn(Optional.empty());

        Map<String, String> config = Map.of(
                "promotion_points_threshold", "0",
                "demotion_salary_threshold", "0",
                "dismissal_points_threshold", "0"
        );

        assertThatCode(() -> service.saveConfig(config)).doesNotThrowAnyException();
    }
}
