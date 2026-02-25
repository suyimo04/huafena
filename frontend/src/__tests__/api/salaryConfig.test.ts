import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/api/axios', () => ({
  default: {
    get: vi.fn(),
    put: vi.fn(),
  },
}))

import http from '@/api/axios'
import {
  getSalaryConfig,
  updateSalaryConfig,
  getCheckinTiers,
  updateCheckinTiers,
  type CheckinTier,
  type RotationThresholds,
  type SalaryConfigMap,
} from '@/api/salaryConfig'

describe('salaryConfig API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getSalaryConfig calls GET /salary-config', async () => {
    const mockData = { code: 200, message: 'ok', data: { salary_pool_total: '2000' } }
    ;(http.get as ReturnType<typeof vi.fn>).mockResolvedValueOnce(mockData)

    const result = await getSalaryConfig()

    expect(http.get).toHaveBeenCalledWith('/salary-config')
    expect(result.data).toEqual({ salary_pool_total: '2000' })
  })

  it('updateSalaryConfig calls PUT /salary-config with config map', async () => {
    const config: SalaryConfigMap = { salary_pool_total: '3000', formal_member_count: '6' }
    const mockResp = { code: 200, message: 'ok', data: undefined }
    ;(http.put as ReturnType<typeof vi.fn>).mockResolvedValueOnce(mockResp)

    await updateSalaryConfig(config)

    expect(http.put).toHaveBeenCalledWith('/salary-config', config)
  })

  it('getCheckinTiers calls GET /salary-config/checkin-tiers', async () => {
    const tiers: CheckinTier[] = [
      { minCount: 0, maxCount: 19, points: -20, label: '不合格' },
      { minCount: 50, maxCount: 999, points: 50, label: '优秀' },
    ]
    ;(http.get as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ code: 200, message: 'ok', data: tiers })

    const result = await getCheckinTiers()

    expect(http.get).toHaveBeenCalledWith('/salary-config/checkin-tiers')
    expect(result.data).toHaveLength(2)
    expect(result.data[0].label).toBe('不合格')
  })

  it('updateCheckinTiers calls PUT /salary-config/checkin-tiers with tiers array', async () => {
    const tiers: CheckinTier[] = [
      { minCount: 0, maxCount: 19, points: -20, label: '不合格' },
    ]
    ;(http.put as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ code: 200, message: 'ok', data: undefined })

    await updateCheckinTiers(tiers)

    expect(http.put).toHaveBeenCalledWith('/salary-config/checkin-tiers', tiers)
  })

  it('types are correctly exported', () => {
    // Verify type exports compile correctly
    const tier: CheckinTier = { minCount: 0, maxCount: 19, points: -20, label: 'test' }
    const thresholds: RotationThresholds = {
      promotionPointsThreshold: 100,
      demotionSalaryThreshold: 150,
      demotionConsecutiveMonths: 2,
      dismissalPointsThreshold: 100,
      dismissalConsecutiveMonths: 2,
    }
    const configMap: SalaryConfigMap = { key: 'value' }

    expect(tier.minCount).toBe(0)
    expect(thresholds.promotionPointsThreshold).toBe(100)
    expect(configMap.key).toBe('value')
  })
})
