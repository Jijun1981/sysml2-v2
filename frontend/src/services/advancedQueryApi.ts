/**
 * 高级查询API客户端 - TDD第五阶段
 * 
 * 实现【REQ-D0-3】前端自动加载数据的高级查询功能
 * 调用后端 /api/v1/elements/advanced 接口
 */

import axios, { AxiosError } from 'axios'

// 基础URL配置
const API_BASE_URL = 'http://localhost:8080/api/v1'

// ElementDTO类型定义（与后端对应）
export interface ElementDTO {
  elementId: string
  eClass: string  
  properties: Record<string, any>
}

// 排序参数
export interface SortParam {
  field: string
  direction: 'asc' | 'desc'
}

// 过滤参数
export interface FilterParam {
  field: string
  value: string
}

// 查询参数接口
export interface QueryParams {
  page?: number
  size?: number
  sort?: SortParam[]
  filter?: FilterParam[]
  search?: string
}

// 高级查询响应接口（对应后端AdvancedQueryController的响应格式）
export interface AdvancedQueryResponse {
  content: ElementDTO[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
  sort?: Record<string, string>
  filter?: Record<string, string>
  search?: string
}

// API错误类型
export class AdvancedQueryError extends Error {
  constructor(
    message: string,
    public statusCode?: number,
    public originalError?: any
  ) {
    super(message)
    this.name = 'AdvancedQueryError'
  }
}

/**
 * 高级查询接口客户端
 * 
 * 【REQ-D0-3】前端自动加载数据：
 * - 支持分页、排序、过滤、搜索
 * - 类型安全的参数和响应
 * - 统一的错误处理
 */
export async function queryAdvanced(params: QueryParams = {}): Promise<AdvancedQueryResponse> {
  try {
    // 构建查询参数
    const queryParams: Record<string, any> = {
      page: params.page ?? 0,
      size: params.size ?? 50
    }

    // 处理排序参数：将 SortParam[] 转换为 string[]
    if (params.sort && params.sort.length > 0) {
      queryParams.sort = params.sort.map(s => `${s.field},${s.direction}`)
    }

    // 处理过滤参数：将 FilterParam[] 转换为 string[]
    if (params.filter && params.filter.length > 0) {
      queryParams.filter = params.filter.map(f => `${f.field}:${f.value}`)
    }

    // 处理搜索参数：过滤空字符串
    if (params.search && params.search.trim() !== '') {
      queryParams.search = params.search
    }

    // 发起请求
    const response = await axios.get(`${API_BASE_URL}/elements/advanced`, {
      params: queryParams
    })

    return response.data as AdvancedQueryResponse
  } catch (error) {
    // 统一错误处理
    throw handleError(error)
  }
}

/**
 * 错误处理函数
 */
function handleError(error: any): AdvancedQueryError {
  // 处理模拟的axios错误（用于测试）
  if (error.response) {
    const responseData = error.response.data
    const message = responseData?.message || responseData?.error || 'Server error'
    return new AdvancedQueryError(message, error.response.status, error)
  }
  
  if (error instanceof AxiosError) {
    if (error.response) {
      // HTTP错误响应
      const responseData = error.response.data
      const message = responseData?.message || responseData?.error || 'Server error'
      return new AdvancedQueryError(message, error.response.status, error)
    } else if (error.request) {
      // 网络错误
      return new AdvancedQueryError('Network Error: Unable to connect to server', 0, error)
    }
  }
  
  // 其他错误
  return new AdvancedQueryError(
    error instanceof Error ? error.message : 'Unknown error',
    0,
    error
  )
}

/**
 * 便利查询方法 - 按类型过滤
 */
export async function queryByType(
  eClass: string, 
  params: Omit<QueryParams, 'filter'> = {}
): Promise<AdvancedQueryResponse> {
  return queryAdvanced({
    ...params,
    filter: [{ field: 'eClass', value: eClass }]
  })
}

/**
 * 便利查询方法 - 搜索需求
 */
export async function searchRequirements(
  searchTerm: string,
  params: Omit<QueryParams, 'search'> = {}
): Promise<AdvancedQueryResponse> {
  return queryAdvanced({
    ...params,
    search: searchTerm
  })
}

/**
 * 便利查询方法 - 获取已批准的需求
 */
export async function getApprovedRequirements(
  params: Omit<QueryParams, 'filter'> = {}
): Promise<AdvancedQueryResponse> {
  return queryAdvanced({
    ...params,
    filter: [{ field: 'status', value: 'approved' }]
  })
}

/**
 * 高级查询钩子工厂 - 为React组件提供状态管理
 * 
 * 使用示例：
 * ```typescript
 * const { data, loading, error, refetch } = useAdvancedQuery({
 *   page: 0,
 *   size: 20,
 *   sort: [{ field: 'declaredName', direction: 'asc' }],
 *   filter: [{ field: 'eClass', value: 'RequirementDefinition' }],
 *   search: '电池'
 * })
 * ```
 */
export interface UseAdvancedQueryResult {
  data: AdvancedQueryResponse | null
  loading: boolean
  error: AdvancedQueryError | null
  refetch: () => Promise<void>
}

/**
 * 导出类型供其他模块使用
 */
export type {
  ElementDTO,
  QueryParams,
  SortParam,
  FilterParam,
  AdvancedQueryResponse
}