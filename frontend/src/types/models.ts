/**
 * SysML v2 MVP 模型类型定义
 */

/**
 * 需求定义
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
 * 需求使用
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
 * 追溯关系
 */
export interface Trace {
  id: string
  fromId: string
  toId: string
  type: 'derive' | 'satisfy' | 'refine' | 'trace'
  createdAt: string
}

/**
 * 树节点
 */
export interface TreeNode {
  id: string
  label: string
  type: 'definition' | 'usage'
  children?: TreeNode[]
}

/**
 * 图节点
 */
export interface GraphNode {
  id: string
  type: 'requirement'
  label: string
  position: { x: number; y: number }
  data?: any
}

/**
 * 图边
 */
export interface GraphEdge {
  id: string
  source: string
  target: string
  type: string
  label?: string
}

/**
 * 校验违规
 */
export interface Violation {
  ruleCode: 'DUP_REQID' | 'CYCLE_DERIVE_REFINE' | 'BROKEN_REF'
  targetId: string
  message: string
  details?: Record<string, any>
}