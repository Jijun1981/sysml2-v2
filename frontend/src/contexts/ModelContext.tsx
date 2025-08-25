import React, { createContext, useContext, useState, useCallback, ReactNode, useEffect } from 'react'
import { 
  ElementData, 
  createUniversalElement, 
  queryElementsByType, 
  queryAllElements,
  updateUniversalElement, 
  deleteUniversalElement,
  setProjectId
} from '../services/universalApi'

/**
 * 模型上下文接口 - 基于通用接口的SSOT实现
 * REQ-A1-1: 数据源唯一 - 统一元素存储
 * REQ-A1-2: 视图为投影 - 所有视图共享同一状态
 */
interface ModelContextType {
  // SSOT数据存储 - 以元素ID为key的映射
  elements: Record<string, ElementData>
  selectedIds: Set<string>
  loading: boolean
  error: Error | null
  
  // 元素操作（通用接口）
  createElement: (eClass: string, attributes: Record<string, any>) => Promise<ElementData>
  updateElement: (id: string, attributes: Record<string, any>) => Promise<ElementData>
  deleteElement: (id: string) => Promise<void>
  loadElementsByType: (eClass: string, params?: any) => Promise<void>
  loadAllElements: (params?: any) => Promise<void>
  
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

interface GraphViewData {
  nodes: GraphNodeData[]
  edges: GraphEdgeData[]
}

interface GraphNodeData {
  id: string
  label: string
  type: string
  x?: number
  y?: number
}

interface GraphEdgeData {
  id: string
  source: string
  target: string
  type: string
  label?: string
}

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

  // 按类型加载元素
  const loadElementsByType = useCallback(async (eClass: string, params?: any): Promise<void> => {
    setLoading(true)
    setError(null)
    try {
      const response = await queryElementsByType(eClass, params)
      const elementsToAdd = (response.data || []).reduce((acc, element) => {
        // 使用 elementId 作为 key，并将整个元素数据存储
        const id = element.elementId || element.id
        acc[id] = {
          ...element,
          id: id,  // 确保有 id 字段
          attributes: {
            declaredName: element.declaredName,
            declaredShortName: element.declaredShortName,
            of: element.of,
            source: element.source,
            target: element.target,
            status: element.status || 'active',
            ...element  // 保留所有其他属性
          }
        }
        return acc
      }, {} as Record<string, ElementData>)
      
      setElementsState(prev => ({
        ...prev,
        ...elementsToAdd
      }))
    } catch (err) {
      const error = err instanceof Error ? err : new Error(`加载${eClass}类型元素失败`)
      setError(error)
      throw error
    } finally {
      setLoading(false)
    }
  }, [])

  // 加载所有元素
  const loadAllElements = useCallback(async (params?: any): Promise<void> => {
    setLoading(true)
    setError(null)
    try {
      const response = await queryAllElements(params)
      const elementsToAdd = (response.data || []).reduce((acc, element) => {
        // 使用 elementId 作为 key，并将整个元素数据存储
        const id = element.elementId || element.id
        acc[id] = {
          ...element,
          id: id,  // 确保有 id 字段
          attributes: {
            declaredName: element.declaredName,
            declaredShortName: element.declaredShortName,
            of: element.of,
            source: element.source,
            target: element.target,
            status: element.status || 'active',
            ...element  // 保留所有其他属性
          }
        }
        return acc
      }, {} as Record<string, ElementData>)
      
      setElementsState(elementsToAdd)
    } catch (err) {
      const error = err instanceof Error ? err : new Error('加载所有元素失败')
      setError(error)
      throw error
    } finally {
      setLoading(false)
    }
  }, [])

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

  // 刷新项目
  const refreshProject = useCallback(async () => {
    await loadAllElements()
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
          .filter(element => element.eClass === 'RequirementUsage' && element.attributes.of === def.id)
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
    Object.values(elements).forEach(element => {
      nodes.push({
        id: element.id,
        label: element.attributes.declaredName || element.attributes.declaredShortName || element.id,
        type: element.eClass.toLowerCase()
      })
    })

    // 创建边
    Object.values(elements).forEach(element => {
      // RequirementUsage的of关系
      if (element.eClass === 'RequirementUsage' && element.attributes.of) {
        edges.push({
          id: `of-${element.id}`,
          source: element.attributes.of,
          target: element.id,
          type: 'of',
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
    createElement,
    updateElement,
    deleteElement,
    loadElementsByType,
    loadAllElements,
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
  GraphEdgeData 
}