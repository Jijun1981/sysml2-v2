/**
 * Mock for advancedQueryApi
 */
import { vi } from 'vitest'

export interface QueryParams {
  query?: string
  eClass?: string
  [key: string]: any
}

export interface AdvancedQueryResponse {
  content: any[]
  totalElements: number
  totalPages: number
  number: number
  page: number
  size: number
  first: boolean
  last: boolean
}

// 模拟的测试数据 - 使用标准化字段
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
    status: 'implemented',
    priority: 'P0'
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

const mockAllElements = [
  ...mockRequirementDefinitions,
  ...mockRequirementUsages
]

export const queryAdvanced = vi.fn(async (params: QueryParams): Promise<AdvancedQueryResponse> => {
  let filteredContent = mockAllElements
  
  // 应用eClass过滤
  if (params.eClass) {
    filteredContent = filteredContent.filter(item => item.eClass === params.eClass)
  }
  
  // 应用分页
  const page = params.page || 0
  const size = params.size || 50
  const startIndex = page * size
  const endIndex = startIndex + size
  const paginatedContent = filteredContent.slice(startIndex, endIndex)
  
  return {
    content: paginatedContent,
    totalElements: filteredContent.length,
    totalPages: Math.ceil(filteredContent.length / size),
    number: page,
    page,
    size,
    first: page === 0,
    last: page >= Math.ceil(filteredContent.length / size) - 1
  }
})

export const queryByType = vi.fn(async (eClass: string, page = 0, size = 20): Promise<AdvancedQueryResponse> => {
  const filteredContent = mockAllElements.filter(item => item.eClass === eClass)
  const startIndex = page * size
  const paginatedContent = filteredContent.slice(startIndex, startIndex + size)
  
  return {
    content: paginatedContent,
    totalElements: filteredContent.length,
    totalPages: Math.ceil(filteredContent.length / size),
    number: page,
    page,
    size,
    first: page === 0,
    last: page >= Math.ceil(filteredContent.length / size) - 1
  }
})

export const searchRequirements = vi.fn(async (query: string, page = 0, size = 20): Promise<AdvancedQueryResponse> => {
  const filteredContent = mockAllElements.filter(item => 
    item.declaredName?.toLowerCase().includes(query.toLowerCase()) ||
    item.documentation?.toLowerCase().includes(query.toLowerCase()) ||
    item.reqId?.toLowerCase().includes(query.toLowerCase())
  )
  const startIndex = page * size
  const paginatedContent = filteredContent.slice(startIndex, startIndex + size)
  
  return {
    content: paginatedContent,
    totalElements: filteredContent.length,
    totalPages: Math.ceil(filteredContent.length / size),
    number: page,
    page,
    size,
    first: page === 0,
    last: page >= Math.ceil(filteredContent.length / size) - 1
  }
})

export const getApprovedRequirements = vi.fn(async (page = 0, size = 20): Promise<AdvancedQueryResponse> => {
  const filteredContent = mockAllElements.filter(item => item.status === 'approved')
  const startIndex = page * size
  const paginatedContent = filteredContent.slice(startIndex, startIndex + size)
  
  return {
    content: paginatedContent,
    totalElements: filteredContent.length,
    totalPages: Math.ceil(filteredContent.length / size),
    number: page,
    page,
    size,
    first: page === 0,
    last: page >= Math.ceil(filteredContent.length / size) - 1
  }
})