import axios, { AxiosInstance, AxiosError } from 'axios'
import { RequirementDefinition, RequirementUsage, Trace } from '../types/models'

/**
 * API服务客户端
 */
class ApiClient {
  private client: AxiosInstance
  
  constructor() {
    this.client = axios.create({
      baseURL: '/api/v1',
      timeout: 30000,
      headers: {
        'Content-Type': 'application/json'
      }
    })
    
    // 响应拦截器
    this.client.interceptors.response.use(
      response => response,
      (error: AxiosError) => {
        const message = error.response?.data?.error?.message || error.message
        console.error('API Error:', message)
        return Promise.reject(new Error(message))
      }
    )
  }
  
  // 健康检查
  async checkHealth() {
    const { data } = await this.client.get('/health')
    return data
  }
  
  // 需求管理
  async getRequirements(projectId: string) {
    const { data } = await this.client.get(`/projects/${projectId}/requirements`)
    return data.content || []
  }
  
  async createRequirement(projectId: string, requirement: Partial<RequirementDefinition>) {
    const { data } = await this.client.post(`/projects/${projectId}/requirements`, requirement)
    return data
  }
  
  async updateRequirement(projectId: string, id: string, updates: Partial<RequirementDefinition>) {
    const { data } = await this.client.put(`/projects/${projectId}/requirements/${id}`, updates)
    return data
  }
  
  async deleteRequirement(projectId: string, id: string) {
    await this.client.delete(`/projects/${projectId}/requirements/${id}`)
  }
  
  // 追溯管理
  async getTraces(projectId: string) {
    const { data } = await this.client.get(`/projects/${projectId}/traces`)
    return data.traces || []
  }
  
  async createTrace(projectId: string, fromId: string, trace: { toId: string; type: string }) {
    const { data } = await this.client.post(
      `/projects/${projectId}/requirements/${fromId}/traces`,
      trace
    )
    return data
  }
  
  async deleteTrace(projectId: string, traceId: string) {
    await this.client.delete(`/projects/${projectId}/traces/${traceId}`)
  }
  
  // 视图数据
  async getTreeData(projectId: string) {
    const { data } = await this.client.get(`/projects/${projectId}/tree`)
    return data
  }
  
  async getTableData(projectId: string, params?: any) {
    const { data } = await this.client.get(`/projects/${projectId}/table`, { params })
    return data
  }
  
  async getGraphData(projectId: string, rootId?: string) {
    const { data } = await this.client.get(`/projects/${projectId}/graph`, {
      params: { rootId }
    })
    return data
  }
  
  // 项目管理
  async exportProject(projectId: string) {
    const { data } = await this.client.get(`/projects/${projectId}/export`)
    return data
  }
  
  async importProject(projectId: string, projectData: any) {
    const { data } = await this.client.post(`/projects/${projectId}/import`, projectData)
    return data
  }
  
  // 校验
  async validateStatic(projectId: string, ids?: string[]) {
    const { data } = await this.client.post(`/projects/${projectId}/validate/static`, {
      ids: ids || []
    })
    return data
  }
}

// 创建单例
const apiClient = new ApiClient()

// 导出方法
export const checkHealth = apiClient.checkHealth.bind(apiClient)
export const getRequirements = apiClient.getRequirements.bind(apiClient)
export const createRequirement = apiClient.createRequirement.bind(apiClient)
export const updateRequirement = apiClient.updateRequirement.bind(apiClient)
export const deleteRequirement = apiClient.deleteRequirement.bind(apiClient)
export const getTraces = apiClient.getTraces.bind(apiClient)
export const createTrace = apiClient.createTrace.bind(apiClient)
export const deleteTrace = apiClient.deleteTrace.bind(apiClient)
export const getTreeData = apiClient.getTreeData.bind(apiClient)
export const getTableData = apiClient.getTableData.bind(apiClient)
export const getGraphData = apiClient.getGraphData.bind(apiClient)
export const exportProject = apiClient.exportProject.bind(apiClient)
export const importProject = apiClient.importProject.bind(apiClient)
export const validateStatic = apiClient.validateStatic.bind(apiClient)

export default apiClient