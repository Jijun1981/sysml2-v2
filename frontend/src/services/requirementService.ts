import axios from 'axios'

const API_BASE_URL = 'http://localhost:8080/api/v1'

export interface RequirementDefinition {
  elementId: string
  reqId: string
  declaredName: string
  declaredShortName?: string
  documentation: string  // 使用documentation替代text
  status?: string
  priority?: string      // 添加优先级字段
  verificationMethod?: string  // 添加验证方法字段
  isAbstract?: boolean
  tags?: string[]
  eClass?: string  // 添加eClass字段标识类型
}

export interface RequirementUsage {
  elementId: string
  requirementDefinition?: string  // 使用requirementDefinition替代of（字段标准化）
  declaredName: string
  documentation: string  // 使用documentation替代text
  parameterValues?: Record<string, any>
  renderedText?: string
  status?: string
  priority?: string      // 添加优先级字段
  verificationMethod?: string  // 添加验证方法字段
  eClass?: string  // 添加eClass字段标识类型
}

class RequirementService {
  // RequirementDefinition CRUD
  async createRequirementDefinition(data: Partial<RequirementDefinition>) {
    const response = await axios.post(`${API_BASE_URL}/requirements`, data)
    return response.data
  }

  async getRequirements(page = 0, size = 50) {
    const response = await axios.get(`${API_BASE_URL}/requirements`, {
      params: { page, size }
    })
    return response.data
  }

  async getRequirementById(id: string) {
    const response = await axios.get(`${API_BASE_URL}/requirements/${id}`)
    return response.data
  }

  // 获取所有Definition（用于下拉选择）
  async getDefinitions() {
    const response = await axios.get(`${API_BASE_URL}/requirements`)
    return response.data
  }

  // 更新RequirementDefinition
  async updateDefinition(id: string, data: Partial<RequirementDefinition>) {
    const response = await axios.patch(`${API_BASE_URL}/requirements/${id}`, data)
    return response.data
  }

  // 更新RequirementUsage
  async updateUsage(id: string, data: Partial<RequirementUsage>) {
    const response = await axios.patch(`${API_BASE_URL}/requirements/usages/${id}`, data)
    return response.data
  }

  async updateRequirement(id: string, data: Partial<RequirementDefinition>) {
    const response = await axios.put(`${API_BASE_URL}/requirements/${id}`, data)
    return response.data
  }

  async deleteRequirement(id: string) {
    const response = await axios.delete(`${API_BASE_URL}/requirements/${id}`)
    return response.data
  }

  // RequirementUsage operations
  async createRequirementUsage(data: Partial<RequirementUsage>) {
    const response = await axios.post(`${API_BASE_URL}/requirements/usages`, data)
    return response.data
  }

  async getRequirementUsages(page = 0, size = 50) {
    const response = await axios.get(`${API_BASE_URL}/requirements/usages`, {
      params: { page, size }
    })
    return response.data
  }

  // 获取所有需求（包括Definition和Usage）
  async getAllRequirements(page = 0, size = 50) {
    const response = await axios.get(`${API_BASE_URL}/elements`, {
      params: { page, size }
    })
    return response.data
  }

  // 获取Definition列表（仅RequirementDefinition）
  async getRequirementDefinitions(page = 0, size = 100) {
    const response = await axios.get(`${API_BASE_URL}/requirements`, {
      params: { page, size }
    })
    return response.data
  }

  // Template operations (future Phase 1)
  async getTemplates() {
    const response = await axios.get(`${API_BASE_URL}/requirements`, {
      params: { 
        filter: 'isAbstract:true',
        size: 100 
      }
    })
    return response.data
  }

  async instantiateTemplate(templateId: string, parameters: Record<string, any>) {
    const response = await axios.post(
      `${API_BASE_URL}/requirements/templates/${templateId}/instantiate`,
      parameters
    )
    return response.data
  }
}

export const requirementService = new RequirementService()