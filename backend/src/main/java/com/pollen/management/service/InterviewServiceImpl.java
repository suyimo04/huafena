package com.pollen.management.service;

import com.pollen.management.dto.InterviewArchiveRecord;
import com.pollen.management.dto.InterviewScenario;
import com.pollen.management.entity.*;
import com.pollen.management.entity.enums.ApplicationStatus;
import com.pollen.management.entity.enums.InterviewStatus;
import com.pollen.management.entity.enums.Role;
import com.pollen.management.repository.*;
import com.pollen.management.repository.RoleChangeHistoryRepository;
import com.pollen.management.util.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 面试对话引擎实现 - 使用模板/规则驱动的模拟 AI 回复（非真实 LLM）
 */
@Service
@Slf4j
public class InterviewServiceImpl implements InterviewService {

    private final InterviewRepository interviewRepository;
    private final InterviewMessageRepository messageRepository;
    private final InterviewReportRepository reportRepository;
    private final ApplicationRepository applicationRepository;
    private final InterviewScenarioService scenarioService;
    private final UserRepository userRepository;
    private final InternshipService internshipService;
    private final RoleChangeHistoryRepository roleChangeHistoryRepository;

    public InterviewServiceImpl(InterviewRepository interviewRepository,
                                InterviewMessageRepository messageRepository,
                                InterviewReportRepository reportRepository,
                                ApplicationRepository applicationRepository,
                                InterviewScenarioService scenarioService,
                                UserRepository userRepository,
                                InternshipService internshipService,
                                RoleChangeHistoryRepository roleChangeHistoryRepository) {
        this.interviewRepository = interviewRepository;
        this.messageRepository = messageRepository;
        this.reportRepository = reportRepository;
        this.applicationRepository = applicationRepository;
        this.scenarioService = scenarioService;
        this.userRepository = userRepository;
        this.internshipService = internshipService;
        this.roleChangeHistoryRepository = roleChangeHistoryRepository;
    }

    @Override
    @Transactional
    public Interview startInterview(Long applicationId, String scenarioId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(404, "申请记录不存在"));

        // Check if interview already exists for this application
        interviewRepository.findByApplicationId(applicationId).ifPresent(existing -> {
            if (existing.getStatus() == InterviewStatus.IN_PROGRESS) {
                throw new BusinessException(400, "该申请已有进行中的面试");
            }
        });

        // Find the scenario
        InterviewScenario scenario = findScenario(scenarioId);

        // Create interview record
        Interview interview = Interview.builder()
                .applicationId(applicationId)
                .userId(application.getUserId())
                .scenarioId(scenarioId)
                .status(InterviewStatus.IN_PROGRESS)
                .difficultyLevel(String.valueOf(scenario.getDifficulty()))
                .build();
        interview = interviewRepository.save(interview);

        // Send initial AI message from scenario
        InterviewMessage initialMessage = InterviewMessage.builder()
                .interviewId(interview.getId())
                .role("AI")
                .content(scenario.getInitialPrompt())
                .timestamp(LocalDateTime.now())
                .timeLimitSeconds(60)
                .build();
        messageRepository.save(initialMessage);

        // Update application status
        application.setStatus(ApplicationStatus.AI_INTERVIEW_IN_PROGRESS);
        applicationRepository.save(application);

        return interview;
    }

    @Override
    @Transactional
    public InterviewMessage processMessage(Long interviewId, String userMessage) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new BusinessException(404, "面试记录不存在"));

        if (interview.getStatus() != InterviewStatus.IN_PROGRESS) {
            throw new BusinessException(400, "面试不在进行中状态");
        }

        // Record user message
        InterviewMessage userMsg = InterviewMessage.builder()
                .interviewId(interviewId)
                .role("USER")
                .content(userMessage)
                .timestamp(LocalDateTime.now())
                .timeLimitSeconds(60)
                .build();
        messageRepository.save(userMsg);

        // Generate simulated AI response based on scenario context
        String aiResponse = generateSimulatedResponse(interview, userMessage);

        InterviewMessage aiMsg = InterviewMessage.builder()
                .interviewId(interviewId)
                .role("AI")
                .content(aiResponse)
                .timestamp(LocalDateTime.now())
                .timeLimitSeconds(60)
                .build();
        messageRepository.save(aiMsg);

        return aiMsg;
    }

    @Override
    @Transactional
    public InterviewReport endInterview(Long interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new BusinessException(404, "面试记录不存在"));

        if (interview.getStatus() != InterviewStatus.IN_PROGRESS) {
            throw new BusinessException(400, "面试不在进行中状态");
        }

        // Get all messages for evaluation
        List<InterviewMessage> messages = messageRepository.findByInterviewIdOrderByTimestamp(interviewId);

        // Generate evaluation report
        InterviewReport report = generateEvaluationReport(interviewId, messages);
        report.setRecommendationLabel(getRecommendationLabel(report.getTotalScore()));
        report = reportRepository.save(report);

        // Update interview status to COMPLETED
        interview.setStatus(InterviewStatus.COMPLETED);
        interview.setCompletedAt(LocalDateTime.now());
        interviewRepository.save(interview);

        // Update application status to PENDING_REVIEW
        Application application = applicationRepository.findById(interview.getApplicationId())
                .orElseThrow(() -> new BusinessException(404, "申请记录不存在"));
        application.setStatus(ApplicationStatus.PENDING_REVIEW);
        applicationRepository.save(application);

        return report;
    }

    @Override
    public Interview getInterview(Long interviewId) {
        return interviewRepository.findById(interviewId)
                .orElseThrow(() -> new BusinessException(404, "面试记录不存在"));
    }

    @Override
    public List<InterviewMessage> getMessages(Long interviewId) {
        // Verify interview exists
        interviewRepository.findById(interviewId)
                .orElseThrow(() -> new BusinessException(404, "面试记录不存在"));
        return messageRepository.findByInterviewIdOrderByTimestamp(interviewId);
    }

    @Override
    public InterviewReport getReport(Long interviewId) {
        return reportRepository.findByInterviewId(interviewId)
                .orElseThrow(() -> new BusinessException(404, "面试报告不存在"));
    }

    @Override
    public String getRecommendationLabel(int totalScore) {
        if (totalScore >= 8) {
            return "建议通过";
        } else if (totalScore >= 6) {
            return "重点审查对话内容";
        } else {
            return "建议拒绝";
        }
    }

    @Override
    @Transactional
    public InterviewReport manualReview(Long interviewId, boolean approved, String reviewComment, String suggestedMentor, Long suggestedMentorId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new BusinessException(404, "面试记录不存在"));

        if (interview.getStatus() != InterviewStatus.COMPLETED) {
            throw new BusinessException(400, "面试尚未完成，无法进行人工复审");
        }

        InterviewReport report = reportRepository.findByInterviewId(interviewId)
                .orElseThrow(() -> new BusinessException(404, "面试报告不存在"));

        // Store review details on the report
        report.setManualApproved(approved);
        report.setReviewerComment(reviewComment);
        report.setSuggestedMentor(suggestedMentor);
        report.setReviewResult(approved ? "通过" : "拒绝");
        report.setReviewedAt(LocalDateTime.now());

        // Update interview status to REVIEWED
        interview.setStatus(InterviewStatus.REVIEWED);
        interviewRepository.save(interview);

        // Update application and user based on review decision
        Application application = applicationRepository.findById(interview.getApplicationId())
                .orElseThrow(() -> new BusinessException(404, "申请记录不存在"));

        User user = userRepository.findById(interview.getUserId())
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        if (approved) {
            // 复审通过：发送实习邀请通知，角色变更为 INTERN
            application.setStatus(ApplicationStatus.INTERN_OFFERED);
            Role oldRole = user.getRole();
            user.setRole(Role.INTERN);
            user.setEnabled(true);
            applicationRepository.save(application);
            userRepository.save(user);

            // 记录角色变更历史
            roleChangeHistoryRepository.save(RoleChangeHistory.builder()
                    .userId(user.getId())
                    .oldRole(oldRole)
                    .newRole(Role.INTERN)
                    .changedBy("system")
                    .build());

            // 自动创建实习记录
            try {
                Internship internship = internshipService.createForNewIntern(user.getId());
                // 如果建议了导师，自动指派
                if (suggestedMentorId != null) {
                    try {
                        internshipService.assignMentor(internship.getId(), suggestedMentorId);
                    } catch (Exception e) {
                        log.warn("自动指派导师失败: internshipId={}, mentorId={}, error={}",
                                internship.getId(), suggestedMentorId, e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.warn("自动创建实习记录失败: userId={}, error={}", user.getId(), e.getMessage());
            }
        } else {
            // 复审拒绝：发送委婉拒绝通知
            application.setStatus(ApplicationStatus.REJECTED);
            applicationRepository.save(application);
            userRepository.save(user);
        }

        return reportRepository.save(report);
    }

    @Override
    public InterviewArchiveRecord getFullArchivedRecord(Long interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new BusinessException(404, "面试记录不存在"));

        List<InterviewMessage> messages = messageRepository.findByInterviewIdOrderByTimestamp(interviewId);

        InterviewReport report = reportRepository.findByInterviewId(interviewId).orElse(null);

        return InterviewArchiveRecord.builder()
                .interview(interview)
                .messages(messages)
                .report(report)
                .build();
    }

    // --- Private helper methods ---

    private InterviewScenario findScenario(String scenarioId) {
        // Search across all scenarios (student + non-student)
        List<InterviewScenario> allScenarios = scenarioService.getScenarios(true);
        return allScenarios.stream()
                .filter(s -> s.getId().equals(scenarioId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(404, "面试场景不存在: " + scenarioId));
    }

    /**
     * 生成模拟 AI 回复 - 基于场景和用户消息的模板/规则驱动回复
     */
    String generateSimulatedResponse(Interview interview, String userMessage) {
        String scenarioId = interview.getScenarioId();
        String lowerMsg = userMessage.toLowerCase();

        // Rule-based response generation based on scenario category and user input
        if (scenarioId != null && scenarioId.startsWith("conflict-resolution")) {
            return generateConflictResponse(lowerMsg);
        } else if (scenarioId != null && scenarioId.startsWith("violation-judgment")) {
            return generateViolationResponse(lowerMsg);
        } else if (scenarioId != null && scenarioId.startsWith("complaint-handling")) {
            return generateComplaintResponse(lowerMsg);
        } else if (scenarioId != null && scenarioId.startsWith("study-work-balance")) {
            return generateStudyWorkResponse(lowerMsg);
        }

        return "好的，我理解你的回答。请继续说明你的处理方案。";
    }

    private String generateConflictResponse(String userMessage) {
        if (userMessage.contains("冷静") || userMessage.contains("理解") || userMessage.contains("沟通")) {
            return "嗯...你说得有道理，但是对方确实太过分了。你能保证公平处理吗？";
        } else if (userMessage.contains("警告") || userMessage.contains("处罚") || userMessage.contains("禁言")) {
            return "你就知道处罚！难道不应该先了解清楚情况吗？我觉得你这样处理不公平。";
        } else if (userMessage.contains("规则") || userMessage.contains("群规")) {
            return "群规我知道，但有些情况群规没有明确规定啊。你觉得这种情况应该怎么灵活处理？";
        }
        return "我不太满意你的回答。能不能更具体地说说你打算怎么处理这件事？";
    }

    private String generateViolationResponse(String userMessage) {
        if (userMessage.contains("查看") || userMessage.contains("核实") || userMessage.contains("证据")) {
            return "好吧，那你去查。但我可以告诉你，我发的内容完全是正常分享，不是广告。你们不能随便给人扣帽子。";
        } else if (userMessage.contains("道歉") || userMessage.contains("理解") || userMessage.contains("抱歉")) {
            return "算了，我也不想闹大。但希望以后判断违规能更谨慎一些，别冤枉好人。";
        } else if (userMessage.contains("规定") || userMessage.contains("群规") || userMessage.contains("标准")) {
            return "那你把具体的群规条款发给我看看，我倒要看看我到底违反了哪一条。";
        }
        return "你这个回答没有说服力。我还是觉得自己没有违规，请给我一个合理的解释。";
    }

    private String generateComplaintResponse(String userMessage) {
        if (userMessage.contains("抱歉") || userMessage.contains("对不起") || userMessage.contains("改进")) {
            return "光说抱歉有什么用？我要看到实际的改进措施。你们打算怎么解决我的问题？";
        } else if (userMessage.contains("方案") || userMessage.contains("解决") || userMessage.contains("处理")) {
            return "这个方案听起来还行，但我怎么知道你们会真的执行？之前也说过要改进，结果呢？";
        } else if (userMessage.contains("反馈") || userMessage.contains("记录") || userMessage.contains("跟进")) {
            return "好吧，那我再给你们一次机会。但如果这次还是没有改善，我就真的要采取行动了。";
        }
        return "你的态度让我更生气了。我需要一个明确的答复，不是敷衍。";
    }

    private String generateStudyWorkResponse(String userMessage) {
        if (userMessage.contains("安排") || userMessage.contains("计划") || userMessage.contains("时间")) {
            return "听起来你有在认真考虑这个问题。那具体到每周，你觉得能抽出多少时间来处理群里的事务？";
        } else if (userMessage.contains("学业") || userMessage.contains("考试") || userMessage.contains("学习")) {
            return "学业确实很重要，我们也理解。但群里的工作也不能完全放下，你觉得有没有折中的办法？";
        } else if (userMessage.contains("交接") || userMessage.contains("代替") || userMessage.contains("帮忙")) {
            return "找人暂时代替是个好主意。你有推荐的人选吗？交接的时候需要注意哪些事项？";
        }
        return "我理解你的困难，但我们需要一个更具体的方案。你能详细说说你的想法吗？";
    }

    /**
     * 生成多维评估报告 - 基于对话内容的规则评分
     */
    InterviewReport generateEvaluationReport(Long interviewId, List<InterviewMessage> messages) {
        long userMessageCount = messages.stream().filter(m -> "USER".equals(m.getRole())).count();

        // Calculate scores based on message analysis
        int ruleFamiliarity = calculateRuleFamiliarity(messages);
        int communicationScore = calculateCommunicationScore(messages);
        int pressureScore = calculatePressureScore(messages);
        int totalScore = Math.round((ruleFamiliarity + communicationScore + pressureScore) / 3.0f);

        // Clamp all scores to 0-10
        ruleFamiliarity = clampScore(ruleFamiliarity);
        communicationScore = clampScore(communicationScore);
        pressureScore = clampScore(pressureScore);
        totalScore = clampScore(totalScore);

        String aiComment = generateAiComment(ruleFamiliarity, communicationScore, pressureScore, userMessageCount);

        return InterviewReport.builder()
                .interviewId(interviewId)
                .ruleFamiliarity(ruleFamiliarity)
                .communicationScore(communicationScore)
                .pressureScore(pressureScore)
                .totalScore(totalScore)
                .aiComment(aiComment)
                .build();
    }

    private int calculateRuleFamiliarity(List<InterviewMessage> messages) {
        int score = 5; // Base score
        for (InterviewMessage msg : messages) {
            if (!"USER".equals(msg.getRole())) continue;
            String content = msg.getContent().toLowerCase();
            if (content.contains("群规") || content.contains("规则") || content.contains("规定")) {
                score += 1;
            }
            if (content.contains("违规") || content.contains("处罚") || content.contains("警告")) {
                score += 1;
            }
        }
        return clampScore(score);
    }

    private int calculateCommunicationScore(List<InterviewMessage> messages) {
        int score = 5; // Base score
        for (InterviewMessage msg : messages) {
            if (!"USER".equals(msg.getRole())) continue;
            String content = msg.getContent().toLowerCase();
            if (content.contains("理解") || content.contains("沟通") || content.contains("倾听")) {
                score += 1;
            }
            if (content.contains("抱歉") || content.contains("对不起") || content.contains("同理")) {
                score += 1;
            }
            if (content.length() > 50) {
                score += 1; // Detailed responses show better communication
            }
        }
        return clampScore(score);
    }

    private int calculatePressureScore(List<InterviewMessage> messages) {
        int score = 5; // Base score
        long userMsgCount = messages.stream().filter(m -> "USER".equals(m.getRole())).count();
        if (userMsgCount >= 3) {
            score += 1; // Persisted through multiple rounds
        }
        for (InterviewMessage msg : messages) {
            if (!"USER".equals(msg.getRole())) continue;
            String content = msg.getContent().toLowerCase();
            if (content.contains("冷静") || content.contains("耐心") || content.contains("理性")) {
                score += 1;
            }
            if (content.contains("方案") || content.contains("解决") || content.contains("处理")) {
                score += 1;
            }
        }
        return clampScore(score);
    }

    private int clampScore(int score) {
        return Math.max(0, Math.min(10, score));
    }

    private String generateAiComment(int ruleFamiliarity, int communicationScore, int pressureScore, long messageCount) {
        StringBuilder comment = new StringBuilder();
        comment.append("面试共进行了").append(messageCount).append("轮对话。");

        if (ruleFamiliarity >= 8) {
            comment.append("候选人对群规非常熟悉，能准确引用相关规定。");
        } else if (ruleFamiliarity >= 6) {
            comment.append("候选人对群规有基本了解，但部分细节掌握不够。");
        } else {
            comment.append("候选人对群规熟悉度不足，建议加强学习。");
        }

        if (communicationScore >= 8) {
            comment.append("沟通能力优秀，表现出良好的耐心和同理心。");
        } else if (communicationScore >= 6) {
            comment.append("沟通能力尚可，有一定的沟通技巧。");
        } else {
            comment.append("沟通能力有待提升，建议加强沟通技巧训练。");
        }

        if (pressureScore >= 8) {
            comment.append("抗压能力强，面对压力场景能保持冷静理性。");
        } else if (pressureScore >= 6) {
            comment.append("抗压能力一般，在压力下基本能维持正常应对。");
        } else {
            comment.append("抗压能力较弱，面对压力场景容易慌乱。");
        }

        return comment.toString();
    }
}
