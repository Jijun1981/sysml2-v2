import axios, { AxiosInstance, AxiosError } from 'axios'
import { RequirementDefinition, RequirementUsage, Trace } from '../types/models'
import { 
  createUniversalElement, 
  queryElementsByType, 
  queryAllElements,
  updateUniversalElement,
  deleteUniversalElement,
  ElementData,
  setProjectId as setUniversalProjectId
} from './universalApi'

/**
 * API服务客户端 - 兼容性层
 * 为了向后兼容，保留原有接口，内部调用通用接口
 * @deprecated 新代码应该直接使用universalApi
 */
class ApiClient {
  private client: AxiosInstance
  
  constructor() {
    this.client = axios.create({
      baseURL: 'http://localhost:8080/api/v1',
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
  
  // 需求管理 - 通过通用接口实现
  async getRequirements(projectId: string) {
    setUniversalProjectId(projectId)
    
    // 获取所有需求相关类型
    const [definitions, usages] = await Promise.all([
      queryElementsByType('RequirementDefinition'),
      queryElementsByType('RequirementUsage')
    ])
    
    return [...definitions.data, ...usages.data]
  }
  
  async createRequirement(projectId: string, requirement: Partial<RequirementDefinition>) {
    setUniversalProjectId(projectId)
    
    // 根据type字段判断创建Definition还是Usage
    const eClass = requirement.type === 'usage' ? 'RequirementUsage' : 'RequirementDefinition'
    
    // 转换字段映射：reqId -> declaredShortName, name -> declaredName, text -> documentation
    const attributes: Record<string, any> = {}
    
    if (requirement.reqId) {
      attributes.declaredShortName = requirement.reqId
    }
    if (requirement.name) {
      attributes.declaredName = requirement.name
    }
    if (requirement.text) {
      attributes.documentation = [{ body: requirement.text }]
    }
    if ((requirement as any).of) {
      attributes.of = (requirement as any).of
    }
    
    return await createUniversalElement(eClass, attributes)
  }
  
  async updateRequirement(projectId: string, id: string, updates: Partial<RequirementDefinition>) {
    setUniversalProjectId(projectId)
    
    // 转换字段映射
    const attributes: Record<string, any> = {}
    
    if (updates.reqId) {
      attributes.declaredShortName = updates.reqId
    }
    if (updates.name) {
      attributes.declaredName = updates.name
    }
    if (updates.text) {
      attributes.documentation = [{ body: updates.text }]
    }
    
    return await updateUniversalElement(id, attributes)
  }
  
  async deleteRequirement(projectId: string, id: string) {
    setUniversalProjectId(projectId)
    await deleteUniversalElement(id)
  }
  
  // 追溯管理 - 通过SysML标准依赖类型实现
  async getTraces(projectId: string) {
    setUniversalProjectId(projectId)
    
    // 获取所有追溯相关的依赖类型
    const [satisfy, derive, refine] = await Promise.all([
      queryElementsByType('Satisfy'),
      queryElementsByType('DeriveRequirement'),
      queryElementsByType('Refine')
    ])
    
    return [...satisfy.data, ...derive.data, ...refine.data]
  }
  
  async createTrace(projectId: string, fromId: string, trace: { toId: string; type: string }) {
    setUniversalProjectId(projectId)
    
    // 映射trace类型到SysML标准依赖类型
    const eClassMap: Record<string, string> = {
      'satisfy': 'Satisfy',
      'derive': 'DeriveRequirement', 
      'refine': 'Refine',
      'trace': 'Dependency' // 通用依赖类型
    }
    
    const eClass = eClassMap[trace.type] || 'Dependency'
    
    return await createUniversalElement(eClass, {
      source: fromId,
      target: trace.toId
    })
  }
  
  async deleteTrace(projectId: string, traceId: string) {
    setUniversalProjectId(projectId)
    await deleteUniversalElement(traceId)
  }
  
  // 视图数据 - 保留原有路径但内部可能调用通用接口
  async getTreeData(projectId: string) {
    // 优先使用现有的ViewController接口，因为它已经实现了树结构逻辑
    const { data } = await this.client.get(`/projects/${projectId}/tree`)
    return data
  }
  
  async getTableData(projectId: string, params?: any) {
    // 优先使用现有的ViewController接口，因为它已经实现了表格逻辑
    const { data } = await this.client.get(`/projects/${projectId}/table`, { params })
    return data
  }
  
  async getGraphData(projectId: string, rootId?: string) {
    // 优先使用现有的ViewController接口，因为它已经实现了图数据逻辑
    const { data } = await this.client.get(`/projects/${projectId}/graph`, {
      params: { rootId }
    })
    return data
  }

  // 新增：直接通过通用接口获取原始元素数据
  async getAllElements(projectId: string, params?: any) {
    setUniversalProjectId(projectId)
    return await queryAllElements(params)
  }

  async getElementsByType(projectId: string, eClass: string, params?: any) {
    setUniversalProjectId(projectId)
    return await queryElementsByType(eClass, params)
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
  
  // 校验 - 使用通用接口的校验功能  
  async validateStatic(projectId: string, ids?: string[]) {
    setUniversalProjectId(projectId)
    // 先尝试使用通用接口的校验
    try {
      return await validateStatic(ids)
    } catch (error) {
      // 回退到原有接口
      const { data } = await this.client.post(`/projects/${projectId}/validate/static`, {
        ids: ids || []
      })
      return data
    }
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

// 新增：通用接口的便利导出
export const getAllElements = apiClient.getAllElements.bind(apiClient)
export const getElementsByType = apiClient.getElementsByType.bind(apiClient)

// 直接导出通用接口方法（推荐新代码使用）
export {
  createUniversalElement,
  queryElementsByType,
  queryAllElements,
  getElementById,
  updateUniversalElement,
  deleteUniversalElement,
  setProjectId,
  checkUniversalHealth
} from './universalApi'

export default apiClient