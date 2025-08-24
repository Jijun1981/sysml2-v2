import { describe, test, expect, beforeEach, vi } from 'vitest'
import axios from 'axios'
import { 
  createUniversalElement, 
  queryElementsByType, 
  updateUniversalElement,
  deleteUniversalElement,
  handleUniversalApiError 
} from '../universalApi'

// Mock axios
vi.mock('axios')
const mockedAxios = vi.mocked(axios, true)

// Properly mock the universalApi module exports
vi.mock('../universalApi', () => ({
  createUniversalElement: vi.fn(),
  queryElementsByType: vi.fn(),
  queryAllElements: vi.fn(),
  getElementById: vi.fn(),
  updateUniversalElement: vi.fn(),
  deleteUniversalElement: vi.fn(),
  validateStatic: vi.fn(),
  checkUniversalHealth: vi.fn(),
  setProjectId: vi.fn(),
  handleUniversalApiError: vi.fn(),
  default: {
    createElement: vi.fn(),
    queryElementsByType: vi.fn(),
    queryAllElements: vi.fn(),
    getElementById: vi.fn(),
    updateElement: vi.fn(),
    deleteElement: vi.fn(),
    validateStatic: vi.fn(),
    checkHealth: vi.fn(),
    setProjectId: vi.fn()
  }
}))

describe('UniversalElementAPI', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockedAxios.create.mockReturnValue(mockedAxios as any)
  })

  describe('createUniversalElement', () => {
    test('should create RequirementDefinition via universal API', async () => {
      // Arrange
      const mockResponse = {
        data: {
          data: {
            id: 'def-001',
            eClass: 'RequirementDefinition',
            attributes: {
              declaredShortName: 'EBS-L1-001',
              declaredName: '电池系统性能需求',
              documentation: [{ body: '电池系统应在${temperature}℃环境下...' }]
            }
          }
        }
      }
      mockedAxios.post.mockResolvedValueOnce(mockResponse)

      // Act
      const result = await createUniversalElement('RequirementDefinition', {
        declaredShortName: 'EBS-L1-001',
        declaredName: '电池系统性能需求',
        documentation: [{ body: '电池系统应在${temperature}℃环境下...' }]
      })

      // Assert
      expect(mockedAxios.post).toHaveBeenCalledWith('/elements', {
        eClass: 'RequirementDefinition',
        attributes: {
          declaredShortName: 'EBS-L1-001',
          declaredName: '电池系统性能需求',
          documentation: [{ body: '电池系统应在${temperature}℃环境下...' }]
        }
      })
      expect(result).toEqual(mockResponse.data.data)
    })

    test('should create RequirementUsage with subject validation', async () => {
      // Arrange  
      const mockResponse = {
        data: {
          data: {
            id: 'usage-001',
            eClass: 'RequirementUsage',
            attributes: {
              declaredShortName: 'EBS-L1-001-U1',
              of: 'def-001',
              subject: 'bms-part-001',
              parameters: { temperature: '45℃' }
            }
          }
        }
      }
      mockedAxios.post.mockResolvedValueOnce(mockResponse)

      // Act
      const result = await createUniversalElement('RequirementUsage', {
        declaredShortName: 'EBS-L1-001-U1',
        of: 'def-001',
        subject: 'bms-part-001',
        parameters: { temperature: '45℃' }
      })

      // Assert
      expect(result.attributes.subject).toBeDefined()
      expect(result.attributes.of).toBe('def-001')
    })
  })

  describe('queryElementsByType', () => {
    test('should query elements by type with pagination', async () => {
      // Arrange
      const mockResponse = {
        data: {
          data: [
            { id: 'def-001', eClass: 'RequirementDefinition' },
            { id: 'def-002', eClass: 'RequirementDefinition' }
          ],
          meta: { page: 0, size: 50, total: 2 }
        }
      }
      mockedAxios.get.mockResolvedValueOnce(mockResponse)

      // Act
      const result = await queryElementsByType('RequirementDefinition', {
        page: 0,
        size: 10
      })

      // Assert
      expect(mockedAxios.get).toHaveBeenCalledWith('/elements', {
        params: {
          type: 'RequirementDefinition',
          page: 0,
          size: 10
        }
      })
      expect(result.data).toHaveLength(2)
      expect(result.meta.total).toBe(2)
    })

    test('should handle empty results gracefully', async () => {
      // Arrange
      const mockResponse = {
        data: {
          data: [],
          meta: { page: 0, size: 50, total: 0 }
        }
      }
      mockedAxios.get.mockResolvedValueOnce(mockResponse)

      // Act
      const result = await queryElementsByType('NonExistentType')

      // Assert
      expect(result.data).toEqual([])
      expect(result.meta.total).toBe(0)
    })
  })

  describe('updateUniversalElement', () => {
    test('should update element via PATCH with partial attributes', async () => {
      // Arrange
      const mockResponse = {
        data: {
          data: {
            id: 'def-001',
            eClass: 'RequirementDefinition',
            attributes: {
              declaredName: '更新后的名称',
              declaredShortName: 'EBS-L1-001' // 保持不变
            }
          }
        }
      }
      mockedAxios.patch.mockResolvedValueOnce(mockResponse)

      // Act
      const result = await updateUniversalElement('def-001', {
        declaredName: '更新后的名称'
      })

      // Assert
      expect(mockedAxios.patch).toHaveBeenCalledWith('/elements/def-001', {
        declaredName: '更新后的名称'
      })
      expect(result.attributes.declaredName).toBe('更新后的名称')
      expect(result.attributes.declaredShortName).toBe('EBS-L1-001') // 未变化
    })
  })

  describe('deleteUniversalElement', () => {
    test('should delete element successfully', async () => {
      // Arrange
      mockedAxios.delete.mockResolvedValueOnce({ status: 204 })

      // Act & Assert
      await expect(deleteUniversalElement('def-001')).resolves.not.toThrow()
      expect(mockedAxios.delete).toHaveBeenCalledWith('/elements/def-001')
    })

    test('should handle delete conflict (409)', async () => {
      // Arrange
      const mockError = {
        response: {
          status: 409,
          data: {
            type: 'https://api/errors/constraint-violation',
            title: 'Element referenced by others',
            detail: 'Cannot delete element that is referenced by other elements',
            blockedBy: ['usage-001', 'usage-002']
          }
        }
      }
      mockedAxios.delete.mockRejectedValueOnce(mockError)

      // Act & Assert
      await expect(deleteUniversalElement('def-001')).rejects.toThrow('Element referenced by others')
    })
  })

  describe('handleUniversalApiError', () => {
    test('should handle API error responses correctly', async () => {
      // Arrange
      const mockError = {
        response: {
          status: 400,
          data: {
            type: 'https://api/errors/validation',
            title: 'Validation failed',
            detail: "field 'subject' is required for RequirementUsage",
            errors: [{ field: 'subject', code: 'REQUIRED' }]
          }
        }
      }

      // Act
      const error = handleUniversalApiError(mockError)

      // Assert
      expect(error.message).toContain('Validation failed')
      expect(error.details).toEqual([{ field: 'subject', code: 'REQUIRED' }])
      expect(error.statusCode).toBe(400)
    })

    test('should handle network errors gracefully', async () => {
      // Arrange
      const mockError = {
        message: 'Network Error',
        code: 'NETWORK_ERROR'
      }

      // Act
      const error = handleUniversalApiError(mockError)

      // Assert
      expect(error.message).toContain('Network Error')
      expect(error.statusCode).toBe(0)
    })
  })

  describe('Integration with existing API patterns', () => {
    test('should maintain backward compatibility with existing view calls', async () => {
      // Arrange
      const mockTreeData = {
        data: {
          data: {
            nodes: [
              { id: 'def-001', type: 'definition', children: ['usage-001'] }
            ]
          }
        }
      }
      mockedAxios.get.mockResolvedValueOnce(mockTreeData)

      // Act - 测试新API可以支持旧的视图数据调用
      const treeData = await queryElementsByType('RequirementDefinition', {
        expand: 'children',
        view: 'tree'
      })

      // Assert
      expect(mockedAxios.get).toHaveBeenCalledWith('/elements', {
        params: {
          type: 'RequirementDefinition',
          expand: 'children',
          view: 'tree'
        }
      })
    })
  })
})