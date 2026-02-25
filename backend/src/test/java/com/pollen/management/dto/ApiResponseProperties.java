package com.pollen.management.dto;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: pollen-group-management, Property 26: 统一 API 响应格式
 * **Validates: Requirements 11.1, 11.4**
 *
 * For any API 响应，JSON 结构应包含 code、message 和 data 三个字段。
 * 成功时 code 为 200，失败时 code 为对应错误码。
 */
class ApiResponseProperties {

    @Property(tries = 100)
    void successResponseHasCode200AndCorrectData(@ForAll String data) {
        ApiResponse<String> response = ApiResponse.success(data);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getMessage()).isEqualTo("success");
        assertThat(response.getData()).isEqualTo(data);
    }

    @Property(tries = 100)
    void successResponseWithIntegerData(@ForAll Integer data) {
        ApiResponse<Integer> response = ApiResponse.success(data);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getMessage()).isEqualTo("success");
        assertThat(response.getData()).isEqualTo(data);
    }

    @Example
    void successResponseWithNullDataHasCode200() {
        ApiResponse<Object> response = ApiResponse.success(null);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getMessage()).isEqualTo("success");
        assertThat(response.getData()).isNull();
    }

    @Property(tries = 100)
    void errorResponseHasGivenCodeAndMessage(
            @ForAll @IntRange(min = 400, max = 599) int code,
            @ForAll @NotBlank String message) {
        ApiResponse<Object> response = ApiResponse.error(code, message);

        assertThat(response.getCode()).isEqualTo(code);
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getData()).isNull();
    }

    @Property(tries = 100)
    void responseAlwaysHasThreeFields(@ForAll String data) {
        ApiResponse<String> success = ApiResponse.success(data);

        // Verify all three fields are accessible (code, message, data)
        assertThat(success).hasFieldOrProperty("code");
        assertThat(success).hasFieldOrProperty("message");
        assertThat(success).hasFieldOrProperty("data");
    }

    @Property(tries = 100)
    void successCodeIsAlways200ErrorCodeIsNever200(
            @ForAll @IntRange(min = 400, max = 599) int errorCode,
            @ForAll String data) {
        ApiResponse<String> success = ApiResponse.success(data);
        ApiResponse<Object> error = ApiResponse.error(errorCode, "error");

        assertThat(success.getCode()).isEqualTo(200);
        assertThat(error.getCode()).isNotEqualTo(200);
    }
}
