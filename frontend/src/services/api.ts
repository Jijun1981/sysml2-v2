/**
 * API服务 - 完全基于通用接口实现
 * 符合需求：REQ-B5-1到B5-4 通用元素接口
 * 
 * 注意：这个文件仅为向后兼容保留
 * 新代码应该直接使用 universalApi.ts
 */

import { 
  createUniversalElement, 
  queryElementsByType, 
  queryAllElements,
  updateUniversalElement,
  deleteUniversalElement,
  getElementById,
  setProjectId as setUniversalProjectId
} from './universalApi'

/**
 * API兼容层 - 内部全部调用通用接口
 * @deprecated 使用 universalApi 代替
 */
class ApiClient {
  
  // 健康检查 - 直接使用通用接口
  async checkHealth() {
    // 通过查询元素来验证系统健康
    try {
      await queryAllElements({ page: 0, size: 1 })
      return { status: 'UP', timestamp: Date.now() }
    } catch (error) {
      return { status: 'DOWN', timestamp: Date.now(), error: error.message }
    }
  }
  
  // 需求管理 - 全部通过通用接口实现
  async getRequirements(projectId: string) {
    setUniversalProjectId(projectId)
    
    // 获取所有需求相关类型
    const [definitions, usages] = await Promise.all([
      queryElementsByType('RequirementDefinition'),
      queryElementsByType('RequirementUsage')
    ])
    
    return [...definitions.data, ...usages.data]
  }
  
  async createRequirement(projectId: string, requirement: any) {
    setUniversalProjectId(projectId)
    
    const eClass = requirement.type === 'usage' ? 'RequirementUsage' : 'RequirementDefinition'
    
    const attributes: Record<string, any> = {}
    if (requirement.reqId) attributes.declaredShortName = requirement.reqId
    if (requirement.name) attributes.declaredName = requirement.name
    if (requirement.text) attributes.documentation = [{ body: requirement.text }]
    if (requirement.of) attributes.of = requirement.of
    
    return await createUniversalElement(eClass, attributes)
  }
  
  async updateRequirement(projectId: string, id: string, updates: any) {
    setUniversalProjectId(projectId)
    
    const attributes: Record<string, any> = {}
    if (updates.reqId) attributes.declaredShortName = updates.reqId
    if (updates.name) attributes.declaredName = updates.name
    if (updates.text) attributes.documentation = [{ body: updates.text }]
    
    return await updateUniversalElement(id, attributes)
  }
  
  async deleteRequirement(projectId: string, id: string) {
    setUniversalProjectId(projectId)
    await deleteUniversalElement(id)
  }
  
  // 追溯管理 - 通过通用接口实现
  async getTraces(projectId: string) {
    setUniversalProjectId(projectId)
    
    const [satisfy, derive, refine] = await Promise.all([
      queryElementsByType('Satisfy'),
      queryElementsByType('DeriveRequirement'),
      queryElementsByType('Refine')
    ])
    
    return [...satisfy.data, ...derive.data, ...refine.data]
  }
  
  async createTrace(projectId: string, fromId: string, trace: { toId: string; type: string }) {
    setUniversalProjectId(projectId)
    
    const eClassMap: Record<string, string> = {
      'satisfy': 'Satisfy',
      'derive': 'DeriveRequirement', 
      'refine': 'Refine',
      'trace': 'Dependency'
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
  
  // 视图数据 - 完全基于通用接口，在前端构建视图投影
  async getTreeData(projectId: string) {
    setUniversalProjectId(projectId)
    
    // REQ-A1-2: 视图为投影 - 从通用接口获取数据，在前端构建树结构
    const [definitions, usages] = await Promise.all([
      queryElementsByType('RequirementDefinition'),
      queryElementsByType('RequirementUsage')
    ])
    
    // 构建树结构
    const tree = definitions.data.map(def => ({
      id: def.elementId,
      label: def.declaredName || def.declaredShortName || def.elementId,
      type: 'definition',
      children: usages.data
        .filter(usage => usage.of === def.elementId)
        .map(usage => ({
          id: usage.elementId,
          label: usage.declaredName || usage.declaredShortName || usage.elementId,
          type: 'usage'
        }))
    }))
    
    return { definitions: tree }
  }
  
  async getTableData(projectId: string, params?: any) {
    setUniversalProjectId(projectId)
    
    // REQ-A1-2: 视图为投影 - 直接返回所有元素作为表格数据
    const response = await queryAllElements(params)
    return response.data
  }
  
  async getGraphData(projectId: string, rootId?: string) {
    setUniversalProjectId(projectId)
    
    // REQ-A1-2: 视图为投影 - 从通用接口构建图数据
    const allElements = await queryAllElements()
    
    const nodes = allElements.data.map(el => ({
      id: el.elementId,
      label: el.declaredName || el.declaredShortName || el.elementId,
      type: el.eClass.toLowerCase()
    }))
    
    const edges: any[] = []
    
    // 构建关系边
    allElements.data.forEach(el => {
      if (el.of) {
        edges.push({
          id: `of-${el.elementId}`,
          source: el.of,
          target: el.elementId,
          type: 'of'
        })
      }
      if (el.source && el.target) {
        edges.push({
          id: el.elementId,
          source: el.source,
          target: el.target,
          type: el.eClass.toLowerCase()
        })
      }
    })
    
    return { nodes, edges }
  }

  // 导入导出 - 基于通用接口
  async exportProject(projectId: string) {
    setUniversalProjectId(projectId)
    const allData = await queryAllElements()
    return {
      projectId,
      exportTime: new Date().toISOString(),
      elements: allData.data
    }
  }
  
  async importProject(projectId: string, projectData: any) {
    setUniversalProjectId(projectId)
    
    // 批量创建元素
    const results = []
    for (const element of projectData.elements) {
      const created = await createUniversalElement(element.eClass, element)
      results.push(created)
    }
    
    return { imported: results.length, elements: results }
  }
  
  // 校验 - 基于通用接口
  async validateStatic(projectId: string, ids?: string[]) {
    setUniversalProjectId(projectId)
    
    // 基于通用接口的简单校验
    const violations = []
    const elements = await queryAllElements()
    
    // REQ-B3-1: reqId唯一性校验
    const reqIds = new Set()
    elements.data.forEach(el => {
      if (el.declaredShortName && reqIds.has(el.declaredShortName)) {
        violations.push({
          elementId: el.elementId,
          rule: 'REQ-B3-1',
          message: `重复的reqId: ${el.declaredShortName}`
        })
      }
      reqIds.add(el.declaredShortName)
    })
    
    return { violations }
  }
  
  // 通用接口直接导出
  async getAllElements(projectId: string, params?: any) {
    setUniversalProjectId(projectId)
    return await queryAllElements(params)
  }

  async getElementsByType(projectId: string, eClass: string, params?: any) {
    setUniversalProjectId(projectId)
    return await queryElementsByType(eClass, params)
  }
}

// 创建单例
const apiClient = new ApiClient()

// 导出兼容方法（标记为废弃）
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
export const getAllElements = apiClient.getAllElements.bind(apiClient)
export const getElementsByType = apiClient.getElementsByType.bind(apiClient)

// 直接导出通用接口方法（推荐使用）
export {
  createUniversalElement,
  queryElementsByType,
  queryAllElements,
  getElementById,
  updateUniversalElement,
  deleteUniversalElement,
  setProjectId
} from './universalApi'

export default apiClient