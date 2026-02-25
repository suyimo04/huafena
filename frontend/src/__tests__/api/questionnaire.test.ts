import { describe, it, expect, vi, beforeEach } from 'vitest'
import http from '@/api/axios'
import {
  getTemplates,
  getTemplate,
  createTemplate,
  updateTemplate,
  publishVersion,
  getVersionHistory,
  getVersion,
} from '@/api/questionnaire'

vi.mock('@/api/axios', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}))

const mockHttp = http as unknown as {
  get: ReturnType<typeof vi.fn>
  post: ReturnType<typeof vi.fn>
  put: ReturnType<typeof vi.fn>
}

describe('questionnaire API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getTemplates calls GET /questionnaire/templates', async () => {
    mockHttp.get.mockResolvedValue({ code: 200, data: [] })
    await getTemplates()
    expect(mockHttp.get).toHaveBeenCalledWith('/questionnaire/templates')
  })

  it('getTemplate calls GET /questionnaire/templates/:id', async () => {
    mockHttp.get.mockResolvedValue({ code: 200, data: {} })
    await getTemplate(5)
    expect(mockHttp.get).toHaveBeenCalledWith('/questionnaire/templates/5')
  })

  it('createTemplate calls POST /questionnaire/templates', async () => {
    const data = { title: 'Test', schemaDefinition: { groups: [], fields: [] } }
    mockHttp.post.mockResolvedValue({ code: 200, data: {} })
    await createTemplate(data)
    expect(mockHttp.post).toHaveBeenCalledWith('/questionnaire/templates', data)
  })

  it('updateTemplate calls PUT /questionnaire/templates/:id', async () => {
    const data = { schemaDefinition: { groups: [], fields: [] } }
    mockHttp.put.mockResolvedValue({ code: 200, data: {} })
    await updateTemplate(3, data)
    expect(mockHttp.put).toHaveBeenCalledWith('/questionnaire/templates/3', data)
  })

  it('publishVersion calls POST /questionnaire/templates/:id/publish', async () => {
    mockHttp.post.mockResolvedValue({ code: 200, data: {} })
    await publishVersion(7)
    expect(mockHttp.post).toHaveBeenCalledWith('/questionnaire/templates/7/publish')
  })

  it('getVersionHistory calls GET /questionnaire/templates/:id/versions', async () => {
    mockHttp.get.mockResolvedValue({ code: 200, data: [] })
    await getVersionHistory(7)
    expect(mockHttp.get).toHaveBeenCalledWith('/questionnaire/templates/7/versions')
  })

  it('getVersion calls GET /questionnaire/versions/:versionId', async () => {
    mockHttp.get.mockResolvedValue({ code: 200, data: {} })
    await getVersion(42)
    expect(mockHttp.get).toHaveBeenCalledWith('/questionnaire/versions/42')
  })
})
