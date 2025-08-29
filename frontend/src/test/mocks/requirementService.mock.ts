/**
 * Mock implementation for requirementService
 * 用于测试的requirementService模拟实现
 */
import { vi } from 'vitest'

// 模拟数据
export const mockRequirementDefinitions = [
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

export const mockRequirementUsages = [
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

// 创建mock服务
export const createMockRequirementService = () => ({
  createRequirementDefinition: vi.fn().mockResolvedValue(mockRequirementDefinitions[0]),
  
  getRequirements: vi.fn().mockResolvedValue({
    content: mockRequirementDefinitions,
    totalElements: 2,
    totalPages: 1,
    number: 0
  }),
  
  getRequirementById: vi.fn().mockImplementation((id: string) => {
    const found = mockRequirementDefinitions.find(d => d.elementId === id)
    return Promise.resolve(found || null)
  }),
  
  updateRequirement: vi.fn().mockResolvedValue(mockRequirementDefinitions[0]),
  
  deleteRequirement: vi.fn().mockResolvedValue(true),
  
  createRequirementUsage: vi.fn().mockResolvedValue(mockRequirementUsages[0]),
  
  getRequirementUsages: vi.fn().mockResolvedValue({
    content: mockRequirementUsages,
    totalElements: 3,
    totalPages: 1,
    number: 0
  }),
  
  getAllRequirements: vi.fn().mockResolvedValue({
    content: [...mockRequirementDefinitions, ...mockRequirementUsages],
    totalElements: 5,
    totalPages: 1,
    number: 0
  }),
  
  getRequirementDefinitions: vi.fn().mockResolvedValue({
    content: mockRequirementDefinitions,
    totalElements: 2,
    totalPages: 1,
    number: 0
  }),
  
  getTemplates: vi.fn().mockResolvedValue({
    content: [],
    totalElements: 0
  }),
  
  instantiateTemplate: vi.fn().mockResolvedValue(null)
})

// 默认mock实例
export const mockRequirementService = createMockRequirementService()

// 为vi.mock提供默认导出
export default {
  requirementService: mockRequirementService
}