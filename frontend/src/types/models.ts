/**
 * SysML v2 MVP 模型类型定义 - TDD第五阶段增强版
 * 
 * 与后端DTO严格对应，支持第四阶段高级查询功能
 */

// ========== 基础ElementDTO类型 ==========

/**
 * 通用元素DTO - 与后端ElementDTO对应
 * 
 * 对应后端: com.sysml.mvp.dto.ElementDTO
 */
export interface ElementDTO {
  /** 元素唯一标识符 */
  elementId: string
  /** SysML元素类型标识，例如：RequirementDefinition, RequirementUsage, Dependency */
  eClass: string
  /** 动态属性Map，存储所有其他属性 */
  properties: Record<string, any>
}

// ========== 专门的DTO类型 ==========

/**
 * 需求定义DTO
 */
export interface RequirementDefinitionDTO extends ElementDTO {
  eClass: 'RequirementDefinition'
  properties: {
    reqId?: string
    declaredName?: string
    declaredShortName?: string
    text?: string
    documentation?: string
    createdAt?: string
    updatedAt?: string
    [key: string]: any
  }
}

/**
 * 需求使用DTO
 */
export interface RequirementUsageDTO extends ElementDTO {
  eClass: 'RequirementUsage'
  properties: {
    of?: string  // 引用的Definition ID
    declaredName?: string
    declaredShortName?: string
    text?: string
    status?: 'draft' | 'approved' | 'rejected' | 'implemented' | 'verified' | 'deprecated'
    createdAt?: string
    updatedAt?: string
    [key: string]: any
  }
}

/**
 * 依赖关系DTO（包含Satisfy, DeriveRequirement, Refine等）
 */
export interface DependencyDTO extends ElementDTO {
  eClass: 'Dependency'
  properties: {
    fromId?: string
    toId?: string
    source?: string  // 兼容字段
    target?: string  // 兼容字段
    type?: 'derive' | 'satisfy' | 'refine' | 'trace'
    createdAt?: string
    [key: string]: any
  }
}

// ========== 高级查询相关类型 ==========

/**
 * 排序参数
 */
export interface SortParam {
  field: string
  direction: 'asc' | 'desc'
}

/**
 * 过滤参数
 */
export interface FilterParam {
  field: string
  value: string
}

/**
 * 查询参数 - 对应后端AdvancedQueryController参数
 */
export interface QueryParams {
  page?: number
  size?: number
  sort?: SortParam[]
  filter?: FilterParam[]
  search?: string
}

/**
 * 高级查询响应 - 对应后端AdvancedQueryController响应格式
 */
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

// ========== 视图数据类型 ==========

/**
 * 树节点数据
 */
export interface TreeNodeData {
  id: string
  label: string
  type: string
  children?: TreeNodeData[]
  usages?: TreeNodeData[]
}

/**
 * 树视图数据
 */
export interface TreeViewData {
  definitions: TreeNodeData[]
}

/**
 * 表格行数据
 */
export interface TableRowData {
  id: string
  eClass: string
  declaredShortName: string
  declaredName: string
  status?: string
  [key: string]: any  // 支持动态列
}

/**
 * 图节点数据 - ReactFlow节点格式
 */
export interface GraphNodeData {
  id: string
  position: {
    x: number
    y: number
  }
  data: {
    label: string
    type: string
    status?: string
    properties?: Record<string, any>
  }
  type?: string
  style?: Record<string, any>
}

/**
 * 图边数据 - ReactFlow边格式
 */
export interface GraphEdgeData {
  id: string
  source: string
  target: string
  type: string
  label?: string
  animated?: boolean
  style?: Record<string, any>
  markerEnd?: any
}

/**
 * 图视图数据
 */
export interface GraphViewData {
  nodes: GraphNodeData[]
  edges: GraphEdgeData[]
}

/**
 * 简化的图节点类型 (兼容旧版本)
 */
export interface GraphNode extends GraphNodeData {}

/**
 * 简化的图边类型 (兼容旧版本)
 */
export interface GraphEdge extends GraphEdgeData {}

// ========== 验证相关类型 ==========

/**
 * 违规信息
 */
export interface Violation {
  elementId: string
  rule: string  // 例如：REQ-B3-1, REQ-E1-2
  message: string
  severity: 'error' | 'warning' | 'info'
  details?: Record<string, any>
}

/**
 * 验证结果摘要
 */
export interface ValidationSummary {
  totalElements: number
  errorCount: number
  warningCount: number
  validElements: number
}

/**
 * 验证结果
 */
export interface ValidationResult {
  violations: Violation[]
  summary: ValidationSummary
}

// ========== 向后兼容的类型别名 ==========

/**
 * @deprecated 使用 RequirementDefinitionDTO 代替
 */
export interface RequirementDefinition {
  id: string
  eClass: 'RequirementDefinition'
  reqId: string
  name: string
  text: string
  doc?: string
  tags?: string[]
  createdAt: string
  updatedAt: string
  _version?: string
}

/**
 * @deprecated 使用 RequirementUsageDTO 代替
 */
export interface RequirementUsage {
  id: string
  eClass: 'RequirementUsage'
  of: string  // 引用的Definition ID
  name: string
  text?: string
  status: 'draft' | 'approved' | 'rejected'
  tags?: string[]
  createdAt: string
  updatedAt: string
  _version?: string
}

/**
 * @deprecated 使用 DependencyDTO 代替
 */
export interface Trace {
  id: string
  fromId: string
  toId: string
  type: 'derive' | 'satisfy' | 'refine' | 'trace'
  createdAt: string
}

/**
 * @deprecated 使用 TreeNodeData 代替
 */
export interface TreeNode {
  id: string
  label: string
  type: 'definition' | 'usage'
  children?: TreeNode[]
}

/**
 * @deprecated 使用 GraphNodeData 代替
 */
export interface GraphNode {
  id: string
  type: 'requirement'
  label: string
  position: { x: number; y: number }
  data?: any
}

/**
 * @deprecated 使用 GraphEdgeData 代替
 */
export interface GraphEdge {
  id: string
  source: string
  target: string
  type: string
  label?: string
}

// ========== 导出统一接口 ==========

/**
 * 统一导出所有类型，便于其他模块导入
 */
export type {
  ElementDTO as Element,
  RequirementDefinitionDTO as ReqDef,
  RequirementUsageDTO as ReqUsage,
  DependencyDTO as Dependency,
  QueryParams as Query,
  AdvancedQueryResponse as QueryResponse
}