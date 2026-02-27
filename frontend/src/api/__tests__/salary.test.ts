import { describe, it, expect, vi, beforeEach } from 'vitest'
import {
  getSalaryPeriods,
  createSalaryPeriod,
  getSalaryMembers,
  batchSaveSalary,
  calculateAndDistribute,
  archiveSalary,
  getSalaryReport,
} from '../salary'
import http from '../axios'

vi.mock('../axios', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}))

const mockGet = vi.mocked(http.get)
const mockPost = vi.mocked(http.post)

beforeEach(() => {
  vi.clearAllMocks()
})

describe('salary API - period parameter passing', () => {
  it('getSalaryPeriods calls GET /salary/periods', async () => {
    const response = { code: 200, data: [], message: 'ok' }
    mockGet.mockResolvedValue(response)

    await getSalaryPeriods()

    expect(mockGet).toHaveBeenCalledWith('/salary/periods')
  })

  it('createSalaryPeriod sends period in request body', async () => {
    const response = { code: 200, data: [], message: 'ok' }
    mockPost.mockResolvedValue(response)

    await createSalaryPeriod('2025-07')

    expect(mockPost).toHaveBeenCalledWith('/salary/periods', { period: '2025-07' })
  })

  it('getSalaryMembers passes period as query param', async () => {
    const response = { code: 200, data: [], message: 'ok' }
    mockGet.mockResolvedValue(response)

    await getSalaryMembers('2025-06')

    expect(mockGet).toHaveBeenCalledWith('/salary/members', { params: { period: '2025-06' } })
  })

  it('getSalaryMembers passes undefined when no period given', async () => {
    const response = { code: 200, data: [], message: 'ok' }
    mockGet.mockResolvedValue(response)

    await getSalaryMembers()

    expect(mockGet).toHaveBeenCalledWith('/salary/members', { params: { period: undefined } })
  })

  it('batchSaveSalary passes period as query param', async () => {
    const response = { code: 200, data: { success: true }, message: 'ok' }
    mockPost.mockResolvedValue(response)

    const payload = { records: [], operatorId: 1 }
    await batchSaveSalary(payload, '2025-07')

    expect(mockPost).toHaveBeenCalledWith('/salary/batch-save', payload, { params: { period: '2025-07' } })
  })

  it('calculateAndDistribute passes period as query param', async () => {
    const response = { code: 200, data: [], message: 'ok' }
    mockPost.mockResolvedValue(response)

    await calculateAndDistribute('2025-07')

    expect(mockPost).toHaveBeenCalledWith('/salary/calculate-distribute', null, { params: { period: '2025-07' } })
  })

  it('archiveSalary passes period as query param', async () => {
    const response = { code: 200, data: 5, message: 'ok' }
    mockPost.mockResolvedValue(response)

    await archiveSalary(1, '2025-07')

    expect(mockPost).toHaveBeenCalledWith('/salary/archive', { operatorId: 1 }, { params: { period: '2025-07' } })
  })

  it('getSalaryReport passes period as query param', async () => {
    const response = { code: 200, data: {}, message: 'ok' }
    mockGet.mockResolvedValue(response)

    await getSalaryReport('2025-07')

    expect(mockGet).toHaveBeenCalledWith('/salary/report', { params: { period: '2025-07' } })
  })
})
