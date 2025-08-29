import React, { createContext, useContext, useState, useCallback, ReactNode, useEffect } from 'react'
import { 
  ElementData, 
  createUniversalElement, 
  updateUniversalElement, 
  deleteUniversalElement,
  setProjectId
} from '../services/universalApi'
import { 
  queryAdvanced, 
  queryByType, 
  searchRequirements as apiSearchRequirements, 
  getApprovedRequirements as apiGetApprovedRequirements,
  type QueryParams,
  type AdvancedQueryResponse 
} from '../services/advancedQueryApi'
import { 
  type GraphViewData, 
  type GraphNodeData, 
  type GraphEdgeData 
} from '../types/models'

/**
 * 模型上下文接口 - 基于通用接口的SSOT实现
 * REQ-A1-1: 数据源唯一 - 统一元素存储
 * REQ-A1-2: 视图为投影 - 所有视图共享同一状态
 */
// 分页信息类型
interface PaginationInfo {
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

interface ModelContextType {
  // SSOT数据存储 - 以元素ID为key的映射
  elements: Record<string, ElementData>
  selectedIds: Set<string>
  loading: boolean
  error: Error | null
  pagination: PaginationInfo
  
  // 元素操作（通用接口）
  createElement: (eClass: string, attributes: Record<string, any>) => Promise<ElementData>
  updateElement: (id: string, attributes: Record<string, any>) => Promise<ElementData>
  deleteElement: (id: string) => Promise<void>
  loadElementsByType: (eClass: string, params?: QueryParams) => Promise<void>
  loadAllElements: (params?: QueryParams) => Promise<void>
  
  // 高级查询便利方法
  searchRequirements: (searchTerm: string, params?: Omit<QueryParams, 'search'>) => Promise<void>
  getApprovedRequirements: (params?: Omit<QueryParams, 'filter'>) => Promise<void>
  
  // 选择操作
  selectElement: (id: string, multiSelect?: boolean) => void
  clearSelection: () => void
  
  // 状态管理
  setElements: (elements: Record<string, ElementData>) => void
  setLoading: (loading: boolean) => void
  setError: (error: Error | null) => void
  
  // 视图投影方法（基于SSOT数据生成不同视图所需的数据结构）
  getTreeViewData: () => TreeViewData
  getTableViewData: () => TableRowData[]
  getGraphViewData: () => GraphViewData
  
  // 项目管理
  setProjectId: (projectId: string) => void
  refreshProject: () => Promise<void>
}

// 视图数据接口
interface TreeViewData {
  definitions: TreeNodeData[]
}

interface TreeNodeData {
  id: string
  label: string
  type: string
  children?: TreeNodeData[]
  usages?: TreeNodeData[]
}

interface TableRowData {
  id: string
  eClass: string
  declaredShortName: string
  declaredName: string
  status?: string
  [key: string]: any
}

// GraphViewData等类型定义已移至 types/models.ts

const ModelContext = createContext<ModelContextType | undefined>(undefined)

export const useModelContext = () => {
  const context = useContext(ModelContext)
  if (!context) {
    throw new Error('useModelContext must be used within ModelProvider')
  }
  return context
}

// 向后兼容的hook
export const useModel = useModelContext

interface ModelProviderProps {
  children: ReactNode
  projectId?: string
  initialElements?: Record<string, ElementData>
}

/**
 * 模型状态提供者 - 通用接口版本
 * 实现REQ-A1-1和REQ-A1-2的SSOT原则
 */
export const ModelProvider: React.FC<ModelProviderProps> = ({ 
  children, 
  projectId = 'default',
  initialElements = {}
}) => {
  // SSOT状态：所有元素存储在单一映射中
  const [elements, setElementsState] = useState<Record<string, ElementData>>(initialElements)
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set())
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<Error | null>(null)
  const [currentProjectId, setCurrentProjectId] = useState(projectId)
  const [pagination, setPagination] = useState<PaginationInfo>({
    page: 0,
    size: 50,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true
  })

  // 设置项目ID并清理状态
  const handleSetProjectId = useCallback((newProjectId: string) => {
    setCurrentProjectId(newProjectId)
    setProjectId(newProjectId)
    setElementsState({})
    setSelectedIds(new Set())
    setError(null)
  }, [])

  // 元素创建
  const createElement = useCallback(async (eClass: string, attributes: Record<string, any>): Promise<ElementData> => {
    setLoading(true)
    setError(null)
    try {
      const response = await createUniversalElement(eClass, attributes)
      const id = response.elementId || response.id
      const newElement: ElementData = {
        ...response,
        id: id,
        eClass: response.eClass,
        attributes: {
          declaredName: response.declaredName,
          declaredShortName: response.declaredShortName,
          of: response.of,
          source: response.source,
          target: response.target,
          status: response.status || 'active',
          ...response
        }
      }
      setElementsState(prev => ({
        ...prev,
        [newElement.id]: newElement
      }))
      return newElement
    } catch (err) {
      const error = err instanceof Error ? err : new Error('创建元素失败')
      setError(error)
      throw error
    } finally {
      setLoading(false)
    }
  }, [])

  // 元素更新
  const updateElement = useCallback(async (id: string, attributes: Record<string, any>): Promise<ElementData> => {
    setLoading(true)
    setError(null)
    try {
      const response = await updateUniversalElement(id, attributes)
      const elementId = response.elementId || response.id || id
      const updatedElement: ElementData = {
        ...response,
        id: elementId,
        eClass: response.eClass,
        attributes: {
          declaredName: response.declaredName,
          declaredShortName: response.declaredShortName,
          of: response.of,
          source: response.source,
          target: response.target,
          status: response.status || 'active',
          ...response
        }
      }
      setElementsState(prev => ({
        ...prev,
        [elementId]: updatedElement
      }))
      return updatedElement
    } catch (err) {
      const error = err instanceof Error ? err : new Error('更新元素失败')
      setError(error)
      throw error
    } finally {
      setLoading(false)
    }
  }, [])

  // 元素删除
  const deleteElement = useCallback(async (id: string): Promise<void> => {
    setLoading(true)
    setError(null)
    try {
      await deleteUniversalElement(id)
      setElementsState(prev => {
        const newState = { ...prev }
        delete newState[id]
        return newState
      })
      setSelectedIds(prev => {
        const newSet = new Set(prev)
        newSet.delete(id)
        return newSet
      })
    } catch (err) {
      const error = err instanceof Error ? err : new Error('删除元素失败')
      setError(error)
      throw error
    } finally {
      setLoading(false)
    }
  }, [])

  // ElementDTO转换为SSOT格式的通用方法
  const convertElementDTOToSSOT = useCallback((elementDTO: any) => {
    // 确保使用唯一ID
    const id = elementDTO.elementId || elementDTO.id || `element-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
    return {
      ...elementDTO,
      id: id,
      eClass: elementDTO.eClass || elementDTO.eclass, // 兼容小写的eclass
      attributes: {
        // 标准化字段映射
        declaredName: elementDTO.properties?.declaredName || elementDTO.declaredName,
        declaredShortName: elementDTO.properties?.declaredShortName || elementDTO.declaredShortName,
        documentation: elementDTO.properties?.documentation || elementDTO.documentation || elementDTO.properties?.text || elementDTO.text,
        requirementDefinition: elementDTO.properties?.requirementDefinition || elementDTO.requirementDefinition || elementDTO.properties?.of || elementDTO.of,
        source: elementDTO.properties?.source || elementDTO.source,
        target: elementDTO.properties?.target || elementDTO.target,
        status: elementDTO.properties?.status || elementDTO.status || 'active',
        priority: elementDTO.properties?.priority || elementDTO.priority,
        verificationMethod: elementDTO.properties?.verificationMethod || elementDTO.verificationMethod,
        reqId: elementDTO.properties?.reqId || elementDTO.reqId,
        // 保持向后兼容性
        text: elementDTO.properties?.documentation || elementDTO.documentation || elementDTO.properties?.text || elementDTO.text,
        of: elementDTO.properties?.requirementDefinition || elementDTO.requirementDefinition || elementDTO.properties?.of || elementDTO.of,
        ...elementDTO.properties,
        ...elementDTO
      }
    }
  }, [])

  // 按类型加载元素（使用高级查询API）
  const loadElementsByType = useCallback(async (eClass: string, params: QueryParams = {}): Promise<void> => {
    setLoading(true)
    setError(null)
    try {
      const response = await queryByType(eClass, params)
      
      // 更新分页信息
      setPagination({
        page: response.page,
        size: response.size,
        totalElements: response.totalElements,
        totalPages: response.totalPages,
        first: response.first,
        last: response.last
      })
      
      const elementsToAdd = response.content.reduce((acc, elementDTO) => {
        const element = convertElementDTOToSSOT(elementDTO)
        acc[element.id] = element
        return acc
      }, {} as Record<string, ElementData>)
      
      setElementsState(prev => ({
        ...prev,
        ...elementsToAdd
      }))
    } catch (err) {
      setError(err as Error)
      throw err
    } finally {
      setLoading(false)
    }
  }, [convertElementDTOToSSOT])

  // 加载所有元素（使用高级查询API）
  const loadAllElements = useCallback(async (params: QueryParams = {}): Promise<void> => {
    setLoading(true)
    setError(null)
    try {
      // 设置默认分页参数
      const queryParams: QueryParams = {
        page: 0,
        size: 50,
        ...params
      }
      
      const response = await queryAdvanced(queryParams)
      
      // 更新分页信息
      setPagination({
        page: response.page,
        size: response.size,
        totalElements: response.totalElements,
        totalPages: response.totalPages,
        first: response.first,
        last: response.last
      })
      
      const elementsToAdd = response.content.reduce((acc, elementDTO) => {
        const element = convertElementDTOToSSOT(elementDTO)
        acc[element.id] = element
        return acc
      }, {} as Record<string, ElementData>)
      
      // 如果是第一页，替换所有元素；否则合并
      if (queryParams.page === 0) {
        setElementsState(elementsToAdd)
      } else {
        setElementsState(prev => ({
          ...prev,
          ...elementsToAdd
        }))
      }
    } catch (err) {
      setError(err as Error)
      throw err
    } finally {
      setLoading(false)
    }
  }, [convertElementDTOToSSOT])

  // 元素选择
  const selectElement = useCallback((id: string, multiSelect = false) => {
    setSelectedIds(prev => {
      if (multiSelect) {
        const newSet = new Set(prev)
        if (newSet.has(id)) {
          newSet.delete(id)
        } else {
          newSet.add(id)
        }
        return newSet
      } else {
        return new Set([id])
      }
    })
  }, [])

  // 清除选择
  const clearSelection = useCallback(() => {
    setSelectedIds(new Set())
  }, [])

  // 直接设置元素（用于测试和初始化）
  const setElements = useCallback((newElements: Record<string, ElementData>) => {
    setElementsState(newElements)
  }, [])

  // 搜索需求的便利方法
  const searchRequirements = useCallback(async (searchTerm: string, params: Omit<QueryParams, 'search'> = {}): Promise<void> => {
    setLoading(true)
    setError(null)
    try {
      const response = await apiSearchRequirements(searchTerm, params)
      
      setPagination({
        page: response.page,
        size: response.size,
        totalElements: response.totalElements,
        totalPages: response.totalPages,
        first: response.first,
        last: response.last
      })
      
      const elementsToAdd = response.content.reduce((acc, elementDTO) => {
        const element = convertElementDTOToSSOT(elementDTO)
        acc[element.id] = element
        return acc
      }, {} as Record<string, ElementData>)
      
      setElementsState(elementsToAdd)
    } catch (err) {
      setError(err as Error)
      throw err
    } finally {
      setLoading(false)
    }
  }, [convertElementDTOToSSOT])

  // 获取已批准需求的便利方法
  const getApprovedRequirements = useCallback(async (params: Omit<QueryParams, 'filter'> = {}): Promise<void> => {
    setLoading(true)
    setError(null)
    try {
      const response = await apiGetApprovedRequirements(params)
      
      setPagination({
        page: response.page,
        size: response.size,
        totalElements: response.totalElements,
        totalPages: response.totalPages,
        first: response.first,
        last: response.last
      })
      
      const elementsToAdd = response.content.reduce((acc, elementDTO) => {
        const element = convertElementDTOToSSOT(elementDTO)
        acc[element.id] = element
        return acc
      }, {} as Record<string, ElementData>)
      
      setElementsState(elementsToAdd)
    } catch (err) {
      setError(err as Error)
      throw err
    } finally {
      setLoading(false)
    }
  }, [convertElementDTOToSSOT])

  // 刷新项目
  const refreshProject = useCallback(async () => {
    await loadAllElements()
  }, [loadAllElements])

  // 初始化时加载数据
  useEffect(() => {
    loadAllElements()
  }, [loadAllElements])

  // 视图投影：树视图数据
  const getTreeViewData = useCallback((): TreeViewData => {
    const definitions = Object.values(elements)
      .filter(element => element.eClass === 'RequirementDefinition')
      .map(def => ({
        id: def.id,
        label: def.attributes.declaredName || def.attributes.declaredShortName || def.id,
        type: 'definition',
        usages: Object.values(elements)
          .filter(element => element.eClass === 'RequirementUsage' && (element.attributes.requirementDefinition === def.id || element.attributes.of === def.id))
          .map(usage => ({
            id: usage.id,
            label: usage.attributes.declaredName || usage.attributes.declaredShortName || usage.id,
            type: 'usage'
          }))
      }))
    
    return { definitions }
  }, [elements])

  // 视图投影：表视图数据
  const getTableViewData = useCallback((): TableRowData[] => {
    return Object.values(elements).map(element => ({
      id: element.id,
      eClass: element.eClass,
      declaredShortName: element.attributes.declaredShortName || '',
      declaredName: element.attributes.declaredName || '',
      status: element.attributes.status || 'active',
      ...element.attributes
    }))
  }, [elements])

  // 视图投影：图视图数据
  const getGraphViewData = useCallback((): GraphViewData => {
    const nodes: GraphNodeData[] = []
    const edges: GraphEdgeData[] = []

    // 创建节点
    let nodeIndex = 0
    Object.values(elements).forEach(element => {
      const isDefinition = element.eClass === 'RequirementDefinition'
      const isUsage = element.eClass === 'RequirementUsage'
      
      // 计算节点位置（简单网格布局）
      const gridSize = Math.ceil(Math.sqrt(Object.keys(elements).length))
      const x = (nodeIndex % gridSize) * 200 + 100
      const y = Math.floor(nodeIndex / gridSize) * 150 + 100
      
      nodes.push({
        id: element.id,
        type: isDefinition ? 'definition' : (isUsage ? 'usage' : 'default'),
        position: { x, y },
        data: {
          label: element.attributes.declaredName || element.attributes.declaredShortName || element.id,
          type: element.eClass,
          status: element.attributes.status,
          reqId: element.attributes.reqId,
          properties: element.attributes
        },
        style: {
          backgroundColor: isDefinition ? '#1976d2' : (isUsage ? '#4caf50' : '#757575'),
          color: '#ffffff',
          border: '2px solid',
          borderColor: isDefinition ? '#0d47a1' : (isUsage ? '#2e7d32' : '#424242'),
          borderRadius: isDefinition ? '8px' : (isUsage ? '50%' : '8px'),
          padding: '10px',
          minWidth: '120px',
          textAlign: 'center'
        }
      })
      nodeIndex++
    })

    // 创建边
    Object.values(elements).forEach(element => {
      // RequirementUsage的requirementDefinition关系（支持新旧字段）
      const requirementDef = element.attributes.requirementDefinition || element.attributes.of
      if (element.eClass === 'RequirementUsage' && requirementDef) {
        edges.push({
          id: `usage-${element.id}`,
          source: requirementDef,
          target: element.id,
          type: 'usage',
          label: 'defines'
        })
      }

      // Satisfy关系
      if (element.eClass === 'Satisfy' && element.attributes.source && element.attributes.target) {
        edges.push({
          id: element.id,
          source: element.attributes.source,
          target: element.attributes.target,
          type: 'satisfy',
          label: 'satisfies'
        })
      }

      // DeriveRequirement关系
      if (element.eClass === 'DeriveRequirement' && element.attributes.source && element.attributes.target) {
        edges.push({
          id: element.id,
          source: element.attributes.source,
          target: element.attributes.target,
          type: 'derive',
          label: 'derives'
        })
      }

      // Refine关系
      if (element.eClass === 'Refine' && element.attributes.source && element.attributes.target) {
        edges.push({
          id: element.id,
          source: element.attributes.source,
          target: element.attributes.target,
          type: 'refine',
          label: 'refines'
        })
      }
    })

    return { nodes, edges }
  }, [elements])

  // 初始化时设置项目ID
  useEffect(() => {
    setProjectId(currentProjectId)
  }, [currentProjectId])

  const value: ModelContextType = {
    elements,
    selectedIds,
    loading,
    error,
    pagination,
    createElement,
    updateElement,
    deleteElement,
    loadElementsByType,
    loadAllElements,
    searchRequirements,
    getApprovedRequirements,
    selectElement,
    clearSelection,
    setElements,
    setLoading,
    setError,
    getTreeViewData,
    getTableViewData,
    getGraphViewData,
    setProjectId: handleSetProjectId,
    refreshProject
  }

  return (
    <ModelContext.Provider value={value}>
      {children}
    </ModelContext.Provider>
  )
}

// 导出类型定义供组件使用
export type { 
  ModelContextType, 
  ElementData, 
  TreeViewData, 
  TreeNodeData, 
  TableRowData, 
  GraphViewData, 
  GraphNodeData, 
  GraphEdgeData,
  PaginationInfo
}