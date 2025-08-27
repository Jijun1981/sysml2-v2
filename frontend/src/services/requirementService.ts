import axios from 'axios'

const API_BASE_URL = 'http://localhost:8080/api/v1'

export interface RequirementDefinition {
  id: string
  reqId: string
  declaredName: string
  declaredShortName?: string
  text: string
  status?: string
  isAbstract?: boolean
  tags?: string[]
}

export interface RequirementUsage {
  id: string
  definition?: string
  declaredName: string
  text: string
  parameterValues?: Record<string, any>
  renderedText?: string
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