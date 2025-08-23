import React, { createContext, useContext, useState, useCallback, ReactNode } from 'react'
import { RequirementDefinition, RequirementUsage, Trace } from '../types/models'
import * as api from '../services/api'

/**
 * 模型上下文接口
 * REQ-A1-1: 数据源唯一 - 单一状态管理
 */
interface ModelContextType {
  // 数据
  requirements: RequirementDefinition[]
  usages: RequirementUsage[]
  traces: Trace[]
  selectedId: string | null
  loading: boolean
  error: string | null
  
  // 操作
  loadProject: (projectId: string) => Promise<void>
  createRequirement: (data: Partial<RequirementDefinition>) => Promise<void>
  updateRequirement: (id: string, data: Partial<RequirementDefinition>) => Promise<void>
  deleteRequirement: (id: string) => Promise<void>
  createTrace: (fromId: string, toId: string, type: string) => Promise<void>
  deleteTrace: (traceId: string) => Promise<void>
  selectElement: (id: string | null) => void
  refresh: () => Promise<void>
}

const ModelContext = createContext<ModelContextType | undefined>(undefined)

export const useModel = () => {
  const context = useContext(ModelContext)
  if (!context) {
    throw new Error('useModel must be used within ModelProvider')
  }
  return context
}

interface ModelProviderProps {
  children: ReactNode
}

/**
 * 模型状态提供者
 * REQ-A1-2: 视图为投影 - 所有视图共享同一状态
 */
export const ModelProvider: React.FC<ModelProviderProps> = ({ children }) => {
  const [requirements, setRequirements] = useState<RequirementDefinition[]>([])
  const [usages, setUsages] = useState<RequirementUsage[]>([])
  const [traces, setTraces] = useState<Trace[]>([])
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [currentProjectId, setCurrentProjectId] = useState<string>('default')

  // 加载项目
  const loadProject = useCallback(async (projectId: string) => {
    setLoading(true)
    setError(null)
    try {
      const [reqData, traceData] = await Promise.all([
        api.getRequirements(projectId),
        api.getTraces(projectId)
      ])
      
      // 分离Definition和Usage
      const defs = reqData.filter(r => r.eClass === 'RequirementDefinition') as RequirementDefinition[]
      const uses = reqData.filter(r => r.eClass === 'RequirementUsage') as RequirementUsage[]
      
      setRequirements(defs)
      setUsages(uses)
      setTraces(traceData)
      setCurrentProjectId(projectId)
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载失败')
    } finally {
      setLoading(false)
    }
  }, [])

  // 创建需求
  const createRequirement = useCallback(async (data: Partial<RequirementDefinition>) => {
    setLoading(true)
    setError(null)
    try {
      const newReq = await api.createRequirement(currentProjectId, data)
      setRequirements(prev => [...prev, newReq])
    } catch (err) {
      setError(err instanceof Error ? err.message : '创建失败')
      throw err
    } finally {
      setLoading(false)
    }
  }, [currentProjectId])

  // 更新需求
  const updateRequirement = useCallback(async (id: string, data: Partial<RequirementDefinition>) => {
    setLoading(true)
    setError(null)
    try {
      const updated = await api.updateRequirement(currentProjectId, id, data)
      setRequirements(prev => prev.map(r => r.id === id ? updated : r))
    } catch (err) {
      setError(err instanceof Error ? err.message : '更新失败')
      throw err
    } finally {
      setLoading(false)
    }
  }, [currentProjectId])

  // 删除需求
  const deleteRequirement = useCallback(async (id: string) => {
    setLoading(true)
    setError(null)
    try {
      await api.deleteRequirement(currentProjectId, id)
      setRequirements(prev => prev.filter(r => r.id !== id))
      setUsages(prev => prev.filter(u => u.of !== id))
    } catch (err) {
      setError(err instanceof Error ? err.message : '删除失败')
      throw err
    } finally {
      setLoading(false)
    }
  }, [currentProjectId])

  // 创建追溯
  const createTrace = useCallback(async (fromId: string, toId: string, type: string) => {
    setLoading(true)
    setError(null)
    try {
      const newTrace = await api.createTrace(currentProjectId, fromId, { toId, type })
      setTraces(prev => [...prev, newTrace])
    } catch (err) {
      setError(err instanceof Error ? err.message : '创建追溯失败')
      throw err
    } finally {
      setLoading(false)
    }
  }, [currentProjectId])

  // 删除追溯
  const deleteTrace = useCallback(async (traceId: string) => {
    setLoading(true)
    setError(null)
    try {
      await api.deleteTrace(currentProjectId, traceId)
      setTraces(prev => prev.filter(t => t.id !== traceId))
    } catch (err) {
      setError(err instanceof Error ? err.message : '删除追溯失败')
      throw err
    } finally {
      setLoading(false)
    }
  }, [currentProjectId])

  // 选择元素
  const selectElement = useCallback((id: string | null) => {
    setSelectedId(id)
  }, [])

  // 刷新
  const refresh = useCallback(async () => {
    await loadProject(currentProjectId)
  }, [currentProjectId, loadProject])

  const value: ModelContextType = {
    requirements,
    usages,
    traces,
    selectedId,
    loading,
    error,
    loadProject,
    createRequirement,
    updateRequirement,
    deleteRequirement,
    createTrace,
    deleteTrace,
    selectElement,
    refresh
  }

  return (
    <ModelContext.Provider value={value}>
      {children}
    </ModelContext.Provider>
  )
}