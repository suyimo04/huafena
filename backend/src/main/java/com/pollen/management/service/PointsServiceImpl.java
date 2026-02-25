package com.pollen.management.service;

import com.pollen.management.config.RedisConfig;
import com.pollen.management.entity.PointsRecord;
import com.pollen.management.entity.enums.PointsType;
import com.pollen.management.repository.PointsRecordRepository;
import com.pollen.management.repository.UserRepository;
import com.pollen.management.util.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointsServiceImpl implements PointsService {

    private final PointsRecordRepository pointsRecordRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = RedisConfig.CACHE_DASHBOARD, allEntries = true),
        @CacheEvict(value = RedisConfig.CACHE_MEMBERS, allEntries = true)
    })
    public PointsRecord addPoints(Long userId, PointsType pointsType, int amount, String description) {
        validateUser(userId);
        if (amount <= 0) {
            throw new BusinessException(400, "增加积分数额必须为正数");
        }
        validateAmountRange(pointsType, amount);

        PointsRecord record = PointsRecord.builder()
                .userId(userId)
                .pointsType(pointsType)
                .amount(amount)
                .description(description)
                .build();

        return pointsRecordRepository.save(record);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = RedisConfig.CACHE_DASHBOARD, allEntries = true),
        @CacheEvict(value = RedisConfig.CACHE_MEMBERS, allEntries = true)
    })
    public PointsRecord deductPoints(Long userId, PointsType pointsType, int amount, String description) {
        validateUser(userId);
        if (amount <= 0) {
            throw new BusinessException(400, "扣减积分数额必须为正数");
        }
        validateAmountRange(pointsType, amount);

        PointsRecord record = PointsRecord.builder()
                .userId(userId)
                .pointsType(pointsType)
                .amount(-amount)
                .description(description)
                .build();

        return pointsRecordRepository.save(record);
    }

    @Override
    public List<PointsRecord> getPointsRecords(Long userId) {
        validateUser(userId);
        return pointsRecordRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public int getTotalPoints(Long userId) {
        validateUser(userId);
        List<PointsRecord> records = pointsRecordRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return records.stream().mapToInt(PointsRecord::getAmount).sum();
    }

    private void validateUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(404, "用户不存在");
        }
    }

    @Override
    public int calculateCheckinPoints(int checkinCount) {
        if (checkinCount < 20) {
            return -20;
        } else if (checkinCount < 30) {
            return -10;
        } else if (checkinCount < 40) {
            return 0;
        } else if (checkinCount < 50) {
            return 30;
        } else {
            return 50;
        }
    }

    @Override
    public int convertPointsToMiniCoins(int points) {
        return points * 2;
    }

    /**
     * 验证积分数额是否在该类型允许的范围内
     */
    private void validateAmountRange(PointsType pointsType, int amount) {
        switch (pointsType) {
            case COMMUNITY_ACTIVITY:
                if (amount < 0 || amount > 100) {
                    throw new BusinessException(400, "社群活跃度积分范围为 0-100");
                }
                break;
            case CHECKIN:
                if (amount < 0 || amount > 50) {
                    throw new BusinessException(400, "签到奖惩积分范围为 0-50");
                }
                break;
            case VIOLATION_HANDLING:
                if (amount != 3) {
                    throw new BusinessException(400, "处理违规积分固定为 3 分");
                }
                break;
            case TASK_COMPLETION:
                if (amount < 1 || amount > 10) {
                    throw new BusinessException(400, "完成任务积分范围为 1-10");
                }
                break;
            case ANNOUNCEMENT:
                if (amount != 5) {
                    throw new BusinessException(400, "发布公告积分固定为 5 分");
                }
                break;
            case EVENT_HOSTING:
                if (amount < 5 || amount > 25) {
                    throw new BusinessException(400, "举办活动积分范围为 5-25");
                }
                break;
            case BIRTHDAY_BONUS:
                if (amount != 25) {
                    throw new BusinessException(400, "生日福利积分固定为 25 分");
                }
                break;
            case MONTHLY_EXCELLENT:
                if (amount < 10 || amount > 30) {
                    throw new BusinessException(400, "月度优秀积分范围为 10-30");
                }
                break;
        }
    }


}
