/**
 * Mock for requirementService - 完全不依赖axios避免序列化问题
 */
import { vi } from 'vitest'

// 模拟数据定义
const mockRequirementDefinitions = [
  {
    elementId: 'req-def-001',
    eClass: 'RequirementDefinition',
    reqId: 'REQ-001',
    declaredName: '充电时间需求',
    declaredShortName: 'REQ-001',
    documentation: '充电时间不超过30分钟',
    status: 'approved',
    priority: 'P0',
    verificationMethod: 'test'
  },
  {
    elementId: 'req-def-002',
    eClass: 'RequirementDefinition',
    reqId: 'REQ-002',
    declaredName: '电池容量需求',
    declaredShortName: 'REQ-002',
    documentation: '电池容量不低于75kWh',
    status: 'draft',
    priority: 'P1',
    verificationMethod: 'analysis'
  }
]

const mockRequirementUsages = [
  {
    elementId: 'req-usage-001',
    eClass: 'RequirementUsage',
    requirementDefinition: 'req-def-001',
    declaredName: '快充场景',
    documentation: '在快充模式下的充电时间需求',
    status: 'implemented',
    priority: 'P0'
  },
  {
    elementId: 'req-usage-002',
    eClass: 'RequirementUsage',
    requirementDefinition: 'req-def-001',
    declaredName: '慢充场景',
    documentation: '在慢充模式下的充电时间需求',
    status: 'draft',
    priority: 'P2'
  },
  {
    elementId: 'req-usage-003',
    eClass: 'RequirementUsage',
    requirementDefinition: 'req-def-002',
    declaredName: '标准电池包',
    documentation: '标准配置的电池容量',
    status: 'approved',
    priority: 'P1'
  }
]

// 创建一个类来模拟RequirementService
class MockRequirementService {
  createRequirementDefinition = vi.fn().mockImplementation(() => 
    Promise.resolve(mockRequirementDefinitions[0])
  )
  
  getRequirements = vi.fn().mockImplementation(() => 
    Promise.resolve({
      content: mockRequirementDefinitions,
      totalElements: 2,
      totalPages: 1,
      number: 0
    })
  )
  
  getRequirementById = vi.fn().mockImplementation((id: string) => {
    const found = mockRequirementDefinitions.find(d => d.elementId === id)
    return Promise.resolve(found || null)
  })
  
  updateRequirement = vi.fn().mockImplementation(() => 
    Promise.resolve(mockRequirementDefinitions[0])
  )
  
  deleteRequirement = vi.fn().mockImplementation(() => 
    Promise.resolve(true)
  )
  
  createRequirementUsage = vi.fn().mockImplementation(() => 
    Promise.resolve(mockRequirementUsages[0])
  )
  
  getRequirementUsages = vi.fn().mockImplementation(() => 
    Promise.resolve({
      content: mockRequirementUsages,
      totalElements: 3,
      totalPages: 1,
      number: 0
    })
  )
  
  getAllRequirements = vi.fn().mockImplementation(() => 
    Promise.resolve({
      content: [...mockRequirementDefinitions, ...mockRequirementUsages],
      totalElements: 5,
      totalPages: 1,
      number: 0
    })
  )
  
  getRequirementDefinitions = vi.fn().mockImplementation(() => 
    Promise.resolve({
      content: mockRequirementDefinitions,
      totalElements: 2,
      totalPages: 1,
      number: 0
    })
  )
  
  getTemplates = vi.fn().mockImplementation(() => 
    Promise.resolve({
      content: [],
      totalElements: 0
    })
  )
  
  instantiateTemplate = vi.fn().mockImplementation(() => 
    Promise.resolve(null)
  )
}

// 导出mock实例
export const requirementService = new MockRequirementService()

// 导出mock数据供测试使用
export { mockRequirementDefinitions, mockRequirementUsages }