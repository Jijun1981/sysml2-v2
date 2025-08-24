import { describe, test, expect, vi, beforeEach } from 'vitest'
import axios from 'axios'
import universalApiClient, { 
  queryAllElements, 
  queryElementsByType, 
  createUniversalElement 
} from '../universalApi'

// Mock axios
vi.mock('axios')
const mockedAxios = vi.mocked(axios, true)

describe('UniversalApi Integration - API Path Configuration', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockedAxios.create.mockReturnValue(mockedAxios as any)
  })

  test('should configure correct baseURL pointing to backend server', () => {
    // 验证axios client配置了正确的baseURL
    expect(mockedAxios.create).toHaveBeenCalledWith({
      baseURL: expect.stringContaining('8080'), // 后端端口应该是8080，不是3002
      timeout: 30000,
      headers: {
        'Content-Type': 'application/json',
        'X-Project-Id': 'default'
      }
    })
  })

  test('should call correct backend endpoint for queryAllElements', async () => {
    const mockResponse = {
      data: {
        data: [],
        meta: { page: 0, size: 50, total: 0 }
      }
    }
    mockedAxios.get.mockResolvedValueOnce(mockResponse)

    await queryAllElements()

    // 验证调用的是正确的API端点
    expect(mockedAxios.get).toHaveBeenCalledWith('/elements', { params: undefined })
  })

  test('should call correct backend endpoint for queryElementsByType', async () => {
    const mockResponse = {
      data: {
        data: [],
        meta: { page: 0, size: 50, total: 0 }
      }
    }
    mockedAxios.get.mockResolvedValueOnce(mockResponse)

    await queryElementsByType('RequirementDefinition')

    // 验证调用的是正确的API端点和参数
    expect(mockedAxios.get).toHaveBeenCalledWith('/elements', {
      params: {
        type: 'RequirementDefinition'
      }
    })
  })

  test('should call correct backend endpoint for createElement', async () => {
    const mockResponse = {
      data: {
        data: {
          id: 'test-001',
          eClass: 'RequirementDefinition',
          attributes: {}
        }
      }
    }
    mockedAxios.post.mockResolvedValueOnce(mockResponse)

    await createUniversalElement('RequirementDefinition', {
      declaredName: 'Test Requirement'
    })

    // 验证调用的是正确的POST端点
    expect(mockedAxios.post).toHaveBeenCalledWith('/elements', {
      eClass: 'RequirementDefinition',
      attributes: {
        declaredName: 'Test Requirement'
      }
    })
  })

  test('should handle API client configuration for cross-origin requests', () => {
    // 验证API客户端配置能够处理跨源请求
    // 前端在localhost:3002，后端在localhost:8080
    const clientConfig = mockedAxios.create.mock.calls[0]?.[0]
    
    expect(clientConfig).toMatchObject({
      baseURL: expect.stringMatching(/localhost:8080|http:\/\/localhost:8080/),
      timeout: expect.any(Number),
      headers: expect.objectContaining({
        'Content-Type': 'application/json'
      })
    })
  })
})