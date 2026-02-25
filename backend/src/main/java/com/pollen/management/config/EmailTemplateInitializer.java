package com.pollen.management.config;

import com.pollen.management.entity.EmailTemplate;
import com.pollen.management.entity.enums.EmailTemplateCode;
import com.pollen.management.repository.EmailTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 初始化默认邮件模板（面试通知、审核结果、实习邀请、拒绝通知、转正通知、周报通知）。
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class EmailTemplateInitializer implements CommandLineRunner {

    private final EmailTemplateRepository emailTemplateRepository;

    @Override
    public void run(String... args) {
        initTemplate(EmailTemplateCode.INTERVIEW_NOTIFICATION.name(),
                "【花粉小组】AI 面试通知 - {{applicantName}}",
                interviewNotificationHtml());

        initTemplate(EmailTemplateCode.REVIEW_RESULT_APPROVED.name(),
                "【花粉小组】审核结果通知 - 恭喜通过",
                reviewApprovedHtml());

        initTemplate(EmailTemplateCode.REVIEW_RESULT_REJECTED.name(),
                "【花粉小组】申请结果通知",
                reviewRejectedHtml());

        initTemplate(EmailTemplateCode.INTERNSHIP_INVITATION.name(),
                "【花粉小组】实习邀请 - 欢迎加入",
                internshipInvitationHtml());

        initTemplate(EmailTemplateCode.CONVERSION_NOTIFICATION.name(),
                "【花粉小组】转正通知 - 恭喜成为正式成员",
                conversionNotificationHtml());

        initTemplate(EmailTemplateCode.WEEKLY_REPORT.name(),
                "【花粉小组】周报通知 - {{weekRange}}",
                weeklyReportHtml());
    }

    private void initTemplate(String code, String subject, String body) {
        if (emailTemplateRepository.findByTemplateCode(code).isEmpty()) {
            emailTemplateRepository.save(EmailTemplate.builder()
                    .templateCode(code)
                    .subjectTemplate(subject)
                    .bodyTemplate(body)
                    .build());
            log.info("初始化邮件模板: {}", code);
        }
    }

    private String interviewNotificationHtml() {
        return """
                <!DOCTYPE html>
                <html><head><meta charset="UTF-8"></head>
                <body style="font-family:Arial,sans-serif;background:#f5f5f5;padding:20px;">
                <div style="max-width:600px;margin:0 auto;background:#fff;border-radius:12px;padding:30px;box-shadow:0 2px 8px rgba(0,0,0,0.1);">
                  <h2 style="color:#10b981;">🌸 花粉小组 AI 面试通知</h2>
                  <p>亲爱的 <strong>{{applicantName}}</strong>，您好！</p>
                  <p>您的入组申请已通过初审，现邀请您参加 AI 面试环节。</p>
                  <div style="background:#f0fdf4;border-left:4px solid #10b981;padding:15px;margin:20px 0;border-radius:4px;">
                    <p style="margin:0;"><strong>面试说明：</strong></p>
                    <ul style="margin:10px 0;">
                      <li>面试以文字对话形式进行</li>
                      <li>AI 将模拟社群场景，请根据花小楼群规作答</li>
                      <li>每轮回答限时 60 秒</li>
                    </ul>
                  </div>
                  <p>请登录系统开始面试，祝您顺利！</p>
                  <p style="color:#6b7280;font-size:12px;margin-top:30px;">此邮件由花粉小组管理系统自动发送，请勿回复。</p>
                </div>
                </body></html>
                """;
    }

    private String reviewApprovedHtml() {
        return """
                <!DOCTYPE html>
                <html><head><meta charset="UTF-8"></head>
                <body style="font-family:Arial,sans-serif;background:#f5f5f5;padding:20px;">
                <div style="max-width:600px;margin:0 auto;background:#fff;border-radius:12px;padding:30px;box-shadow:0 2px 8px rgba(0,0,0,0.1);">
                  <h2 style="color:#10b981;">🎉 审核通过通知</h2>
                  <p>亲爱的 <strong>{{applicantName}}</strong>，您好！</p>
                  <p>恭喜您，您的申请已通过审核！</p>
                  <div style="background:#f0fdf4;border-left:4px solid #10b981;padding:15px;margin:20px 0;border-radius:4px;">
                    <p style="margin:0;">审核意见：{{reviewComment}}</p>
                  </div>
                  <p>请登录系统查看后续安排。</p>
                  <p style="color:#6b7280;font-size:12px;margin-top:30px;">此邮件由花粉小组管理系统自动发送，请勿回复。</p>
                </div>
                </body></html>
                """;
    }

    private String reviewRejectedHtml() {
        return """
                <!DOCTYPE html>
                <html><head><meta charset="UTF-8"></head>
                <body style="font-family:Arial,sans-serif;background:#f5f5f5;padding:20px;">
                <div style="max-width:600px;margin:0 auto;background:#fff;border-radius:12px;padding:30px;box-shadow:0 2px 8px rgba(0,0,0,0.1);">
                  <h2 style="color:#f59e0b;">📋 申请结果通知</h2>
                  <p>亲爱的 <strong>{{applicantName}}</strong>，您好！</p>
                  <p>感谢您对花粉小组的关注和申请。经过认真评估，我们暂时无法为您提供入组机会。</p>
                  <p>这并不代表对您能力的否定，我们鼓励您在未来继续关注花粉小组的招募信息。</p>
                  <p>祝您一切顺利！</p>
                  <p style="color:#6b7280;font-size:12px;margin-top:30px;">此邮件由花粉小组管理系统自动发送，请勿回复。</p>
                </div>
                </body></html>
                """;
    }

    private String internshipInvitationHtml() {
        return """
                <!DOCTYPE html>
                <html><head><meta charset="UTF-8"></head>
                <body style="font-family:Arial,sans-serif;background:#f5f5f5;padding:20px;">
                <div style="max-width:600px;margin:0 auto;background:#fff;border-radius:12px;padding:30px;box-shadow:0 2px 8px rgba(0,0,0,0.1);">
                  <h2 style="color:#10b981;">🌟 实习邀请</h2>
                  <p>亲爱的 <strong>{{applicantName}}</strong>，您好！</p>
                  <p>恭喜您通过面试！我们诚挚邀请您加入花粉小组，开始为期 30 天的实习。</p>
                  <div style="background:#f0fdf4;border-left:4px solid #10b981;padding:15px;margin:20px 0;border-radius:4px;">
                    <p style="margin:0;"><strong>实习信息：</strong></p>
                    <ul style="margin:10px 0;">
                      <li>实习导师：{{mentorName}}</li>
                      <li>实习时长：30 天</li>
                      <li>请登录系统查看实习任务清单</li>
                    </ul>
                  </div>
                  <p>期待您的出色表现！</p>
                  <p style="color:#6b7280;font-size:12px;margin-top:30px;">此邮件由花粉小组管理系统自动发送，请勿回复。</p>
                </div>
                </body></html>
                """;
    }

    private String conversionNotificationHtml() {
        return """
                <!DOCTYPE html>
                <html><head><meta charset="UTF-8"></head>
                <body style="font-family:Arial,sans-serif;background:#f5f5f5;padding:20px;">
                <div style="max-width:600px;margin:0 auto;background:#fff;border-radius:12px;padding:30px;box-shadow:0 2px 8px rgba(0,0,0,0.1);">
                  <h2 style="color:#10b981;">🎊 转正通知</h2>
                  <p>亲爱的 <strong>{{memberName}}</strong>，您好！</p>
                  <p>恭喜您顺利完成实习期，正式成为花粉小组的正式成员！</p>
                  <div style="background:#f0fdf4;border-left:4px solid #10b981;padding:15px;margin:20px 0;border-radius:4px;">
                    <p style="margin:0;">您现在可以享受正式成员的全部权益，包括薪资分配和更多管理功能。</p>
                  </div>
                  <p>感谢您的努力和付出，期待您继续为花粉小组做出贡献！</p>
                  <p style="color:#6b7280;font-size:12px;margin-top:30px;">此邮件由花粉小组管理系统自动发送，请勿回复。</p>
                </div>
                </body></html>
                """;
    }

    private String weeklyReportHtml() {
        return """
                <!DOCTYPE html>
                <html><head><meta charset="UTF-8"></head>
                <body style="font-family:Arial,sans-serif;background:#f5f5f5;padding:20px;">
                <div style="max-width:600px;margin:0 auto;background:#fff;border-radius:12px;padding:30px;box-shadow:0 2px 8px rgba(0,0,0,0.1);">
                  <h2 style="color:#10b981;">📊 周报通知</h2>
                  <p>以下是 <strong>{{weekRange}}</strong> 的运营周报摘要：</p>
                  <table style="width:100%;border-collapse:collapse;margin:20px 0;">
                    <tr style="background:#f0fdf4;">
                      <td style="padding:10px;border:1px solid #e5e7eb;">新增申请数</td>
                      <td style="padding:10px;border:1px solid #e5e7eb;text-align:right;"><strong>{{newApplications}}</strong></td>
                    </tr>
                    <tr>
                      <td style="padding:10px;border:1px solid #e5e7eb;">面试完成数</td>
                      <td style="padding:10px;border:1px solid #e5e7eb;text-align:right;"><strong>{{interviewsCompleted}}</strong></td>
                    </tr>
                    <tr style="background:#f0fdf4;">
                      <td style="padding:10px;border:1px solid #e5e7eb;">新增成员数</td>
                      <td style="padding:10px;border:1px solid #e5e7eb;text-align:right;"><strong>{{newMembers}}</strong></td>
                    </tr>
                    <tr>
                      <td style="padding:10px;border:1px solid #e5e7eb;">活动举办数</td>
                      <td style="padding:10px;border:1px solid #e5e7eb;text-align:right;"><strong>{{activitiesHeld}}</strong></td>
                    </tr>
                    <tr style="background:#f0fdf4;">
                      <td style="padding:10px;border:1px solid #e5e7eb;">积分发放总量</td>
                      <td style="padding:10px;border:1px solid #e5e7eb;text-align:right;"><strong>{{totalPointsIssued}}</strong></td>
                    </tr>
                  </table>
                  <p>请登录系统查看详细报告。</p>
                  <p style="color:#6b7280;font-size:12px;margin-top:30px;">此邮件由花粉小组管理系统自动发送，请勿回复。</p>
                </div>
                </body></html>
                """;
    }
}
