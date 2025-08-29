/**
 * Mock for universalApi - 避免axios被导入
 */
import { vi } from 'vitest'

export class UniversalApiClient {
  private baseURL: string
  
  constructor(baseURL: string) {
    this.baseURL = baseURL
  }
  
  async createElement(data: any) {
    return Promise.resolve({ elementId: 'mock-id', ...data })
  }
  
  async getElements(params?: any) {
    return Promise.resolve({ content: [], totalElements: 0 })
  }
  
  async getElementById(id: string) {
    return Promise.resolve({ elementId: id })
  }
  
  async updateElement(id: string, data: any) {
    return Promise.resolve({ elementId: id, ...data })
  }
  
  async deleteElement(id: string) {
    return Promise.resolve({ success: true })
  }
  
  async searchElements(params: any) {
    return Promise.resolve({ content: [], totalElements: 0 })
  }
  
  async validateElement(id: string) {
    return Promise.resolve({ valid: true, errors: [] })
  }
  
  async bulkOperation(operation: string, ids: string[]) {
    return Promise.resolve({ success: true, processedIds: ids })
  }
}

// 导出单例
export const universalApi = new UniversalApiClient('http://localhost:8080/api/v1')

// 导出辅助函数
export const setProjectId = vi.fn((projectId: string) => {
  // Mock implementation
})

export const createUniversalElement = vi.fn(async (data: any) => {
  return Promise.resolve({ elementId: 'mock-id', ...data })
})

export const updateUniversalElement = vi.fn(async (id: string, data: any) => {
  return Promise.resolve({ elementId: id, ...data })
})

export const deleteUniversalElement = vi.fn(async (id: string) => {
  return Promise.resolve({ success: true })
})

// ElementData type
export interface ElementData {
  elementId?: string
  eClass?: string
  [key: string]: any
}