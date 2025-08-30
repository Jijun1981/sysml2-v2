import axios, { AxiosInstance, AxiosError } from 'axios'

/**
 * 通用元素接口客户端
 * 基于REQ-B5-1到REQ-B5-4实现，调用后端通用接口 /api/v1/elements
 */

// 元素数据接口
export interface ElementData {
  id: string
  eClass: string
  attributes: Record<string, any>
}

// 查询响应接口
export interface QueryResponse<T> {
  data: T[]
  meta: {
    page: number
    size: number
    total: number
  }
  timestamp: string
}

// API错误接口
export interface ApiError {
  message: string
  statusCode: number
  type?: string
  details?: any[]
}

// 通用接口客户端类
export class UniversalApiClient {
  private client: AxiosInstance
  private projectId: string = 'default'  // 存储当前projectId

  constructor() {
    this.client = axios.create({
      baseURL: 'http://localhost:8080/api/v1',
      timeout: 30000,
      headers: {
        'Content-Type': 'application/json'
      }
    })

    // 响应拦截器 - 统一错误处理
    this.client.interceptors.response.use(
      response => response,
      (error: AxiosError) => {
        throw this.handleError(error)
      }
    )
  }

  /**
   * REQ-B5-1: 通用元素创建
   * POST /api/v1/elements {eClass, attributes}
   */
  async createElement(eClass: string, attributes: Record<string, any>): Promise<ElementData> {
    const { data } = await this.client.post(`/elements?projectId=${this.projectId}`, {
      eClass,
      ...attributes  // 直接传递属性，不嵌套
    })
    return data  // 后端直接返回元素对象
  }

  /**
   * REQ-B5-2: 按类型查询元素  
   * GET /api/v1/elements?type={eClass}&page={page}&size={size}
   */
  async queryElementsByType(
    eClass: string,
    params?: {
      page?: number
      size?: number
      expand?: string
      view?: string
      fields?: string
      [key: string]: any
    }
  ): Promise<QueryResponse<ElementData>> {
    const { data } = await this.client.get('/elements', {
      params: {
        projectId: this.projectId,
        type: eClass,
        ...params
      }
    })
    // 直接返回数组，不是包装的响应
    return { data: data || [], meta: { page: 0, size: 100, total: data?.length || 0 }, timestamp: new Date().toISOString() }
  }

  /**
   * 查询所有元素（不限制类型）
   */
  async queryAllElements(params?: {
    page?: number
    size?: number
    expand?: string
    fields?: string
  }): Promise<QueryResponse<ElementData>> {
    const { data } = await this.client.get('/elements', { 
      params: {
        projectId: this.projectId,
        ...params
      }
    })
    // 直接返回数组，不是包装的响应
    return { data: data || [], meta: { page: 0, size: 100, total: data?.length || 0 }, timestamp: new Date().toISOString() }
  }

  /**
   * 根据ID获取单个元素
   * GET /api/v1/elements/{id}
   */
  async getElementById(id: string, expand?: string): Promise<ElementData> {
    const { data } = await this.client.get(`/elements/${id}`, {
      params: { projectId: this.projectId, ...(expand ? { expand } : {}) }
    })
    return data  // 后端直接返回元素对象
  }

  /**
   * REQ-B5-3: 通用PATCH更新
   * PATCH /api/v1/elements/{id} {attributes}
   */
  async updateElement(id: string, attributes: Record<string, any>): Promise<ElementData> {
    console.log('=== universalApi.updateElement 开始 ===')
    console.log('ID:', id)
    console.log('attributes:', attributes)
    console.log('projectId:', this.projectId)
    console.log('URL:', `/elements/${id}?projectId=${this.projectId}`)
    
    const { data } = await this.client.patch(`/elements/${id}?projectId=${this.projectId}`, attributes)
    console.log('=== universalApi.updateElement 响应 ===')
    console.log('response data:', data)
    return data  // 后端直接返回元素对象
  }

  /**
   * 删除元素
   * DELETE /api/v1/elements/{id}
   */
  async deleteElement(id: string): Promise<void> {
    await this.client.delete(`/elements/${id}?projectId=${this.projectId}`)
  }

  /**
   * 统一错误处理
   */
  private handleError(error: AxiosError): ApiError {
    if (error.response) {
      // 服务器响应错误
      const responseData = error.response.data as any
      return {
        message: responseData.title || responseData.detail || 'Server Error',
        statusCode: error.response.status,
        type: responseData.type,
        details: responseData.errors || []
      }
    } else if (error.request) {
      // 网络错误
      return {
        message: 'Network Error: Unable to connect to server',
        statusCode: 0
      }
    } else {
      // 其他错误
      return {
        message: error.message || 'Unknown Error',
        statusCode: 0
      }
    }
  }

  /**
   * 设置项目ID（支持多项目切换）
   */
  setProjectId(projectId: string) {
    this.projectId = projectId
  }

  /**
   * 健康检查
   */
  async checkHealth() {
    const { data } = await this.client.get('/health')
    return data
  }

  /**
   * 静态校验
   * POST /api/v1/validate/static
   */
  async validateStatic(ids?: string[]) {
    const { data } = await this.client.post('/validate/static', {
      ids: ids || []
    })
    return data
  }
}

// 创建单例实例
const universalApiClient = new UniversalApiClient()

// 导出便利方法
export const createUniversalElement = universalApiClient.createElement.bind(universalApiClient)
export const queryElementsByType = universalApiClient.queryElementsByType.bind(universalApiClient)
export const queryAllElements = universalApiClient.queryAllElements.bind(universalApiClient)
export const getElementById = universalApiClient.getElementById.bind(universalApiClient)
export const updateUniversalElement = universalApiClient.updateElement.bind(universalApiClient)
export const deleteUniversalElement = universalApiClient.deleteElement.bind(universalApiClient)
export const validateStatic = universalApiClient.validateStatic.bind(universalApiClient)
export const checkUniversalHealth = universalApiClient.checkHealth.bind(universalApiClient)
export const setProjectId = universalApiClient.setProjectId.bind(universalApiClient)

// 导出错误处理函数供测试使用
export const handleUniversalApiError = (error: any): ApiError => {
  return universalApiClient['handleError'](error)
}

// 导出客户端实例供高级用法
export default universalApiClient

// 便利类型定义导出（已在文件顶部导出，这里移除重复）

/**
 * REQ-B5-4: 零代码扩展能力
 * 这个API客户端支持所有182个SysML类型，无需为每个类型编写专门的代码
 * 
 * 使用示例：
 * 
 * // 创建RequirementDefinition
 * const reqDef = await createUniversalElement('RequirementDefinition', {
 *   declaredShortName: 'REQ-001',
 *   declaredName: '系统需求',
 *   documentation: [{ body: '系统应该...' }]
 * })
 * 
 * // 创建PartUsage（零代码扩展）
 * const partUsage = await createUniversalElement('PartUsage', {
 *   declaredName: '电池组件',
 *   declaredShortName: 'BATTERY'
 * })
 * 
 * // 创建Satisfy关系  
 * const satisfy = await createUniversalElement('Satisfy', {
 *   source: 'part-001',
 *   target: 'usage-001'
 * })
 */