package com.pollen.management.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pollen.management.dto.InterviewScenario;
import com.pollen.management.util.BusinessException;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 面试场景加载服务实现 - 从 classpath 下的 JSON 文件加载场景数据
 */
@Service
public class InterviewScenarioServiceImpl implements InterviewScenarioService {

    private static final String SCENARIO_FILE = "interview-scenarios.json";

    private final ObjectMapper objectMapper;
    private List<InterviewScenario> allScenarios;

    public InterviewScenarioServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void loadScenarios() {
        ClassPathResource resource = new ClassPathResource(SCENARIO_FILE);
        if (!resource.exists()) {
            throw new BusinessException(500, "面试场景文件不存在: " + SCENARIO_FILE);
        }
        try (InputStream is = resource.getInputStream()) {
            allScenarios = objectMapper.readValue(is, new TypeReference<List<InterviewScenario>>() {});
        } catch (IOException e) {
            throw new BusinessException(500, "面试场景文件格式错误: " + e.getMessage());
        }
    }

    @Override
    public List<InterviewScenario> getScenarios(boolean isStudent) {
        if (allScenarios == null) {
            throw new BusinessException(500, "面试场景数据未加载");
        }
        return allScenarios.stream()
                .filter(s -> isStudent || !s.isStudentOnly())
                .collect(Collectors.toList());
    }

    @Override
    public List<InterviewScenario> getScenariosByDifficulty(int difficulty, boolean isStudent) {
        return getScenarios(isStudent).stream()
                .filter(s -> s.getDifficulty() == difficulty)
                .collect(Collectors.toList());
    }
}
