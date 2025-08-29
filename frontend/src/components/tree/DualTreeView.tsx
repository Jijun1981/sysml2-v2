/**
 * DualTreeView组件 - REQ-UI-4: 双树界面布局
 * 
 * 功能特性：
 * - 上下分割的双树视图
 * - 上部显示RequirementDefinition
 * - 下部显示RequirementUsage
 * - 支持拖动调节分割线
 * - 每部分独立搜索
 */

import React, { useState, useMemo, useCallback } from 'react'
import { Tree, Input, Typography, Space, Tag, Empty, Spin } from 'antd'
import { SearchOutlined } from '@ant-design/icons'
import { useModelContext } from '../../contexts/ModelContext'
import type { DataNode } from 'antd/es/tree'
import type { TreeNodeData } from '../../types/models'

const { Title } = Typography
const { Search } = Input

interface DualTreeViewProps {
  /** 选中节点回调 */
  onSelect?: (elementId: string, isMultiSelect?: boolean) => void
  /** 是否显示搜索框 */
  showSearch?: boolean
  /** 分割线初始位置（百分比） */
  splitPosition?: number
}

/**
 * 构建树节点数据
 */
const buildTreeData = (
  elements: TreeNodeData[], 
  filterType: 'definition' | 'usage'
): DataNode[] => {
  // 过滤数据
  const filtered = elements.filter(element => {
    if (filterType === 'definition') {
      return element.eClass === 'RequirementDefinition'
    } else {
      return element.eClass === 'RequirementUsage'
    }
  })

  // 构建树节点
  return filtered.map(element => ({
    key: element.id,
    title: (
      <Space>
        <Tag color={filterType === 'definition' ? 'blue' : 'purple'}>
          {filterType === 'definition' ? 'DEF' : 'USE'}
        </Tag>
        <span>{element.reqId || element.declaredName || element.id}</span>
      </Space>
    ),
    children: [],
    isLeaf: true
  }))
}

/**
 * 搜索过滤树节点
 */
const filterTreeData = (data: DataNode[], searchValue: string): DataNode[] => {
  if (!searchValue) return data

  const lowerSearchValue = searchValue.toLowerCase()
  
  return data.filter(node => {
    const title = node.title?.toString() || ''
    return title.toLowerCase().includes(lowerSearchValue)
  })
}

/**
 * 双树视图组件
 */
const DualTreeView: React.FC<DualTreeViewProps> = ({
  onSelect,
  showSearch = true,
  splitPosition = 50
}) => {
  const { 
    getTreeViewData, 
    selectedIds, 
    selectElement,
    loading 
  } = useModelContext()

  // 状态
  const [defSearchValue, setDefSearchValue] = useState('')
  const [usageSearchValue, setUsageSearchValue] = useState('')
  const [splitPos, setSplitPos] = useState(splitPosition)
  const [isDragging, setIsDragging] = useState(false)

  // 获取树数据
  const treeData = useMemo(() => {
    return getTreeViewData()
  }, [getTreeViewData])

  // 构建Definition树数据
  const definitionTreeData = useMemo(() => {
    const data = buildTreeData(treeData, 'definition')
    return filterTreeData(data, defSearchValue)
  }, [treeData, defSearchValue])

  // 构建Usage树数据
  const usageTreeData = useMemo(() => {
    const data = buildTreeData(treeData, 'usage')
    return filterTreeData(data, usageSearchValue)
  }, [treeData, usageSearchValue])

  // 处理选中事件
  const handleSelect = useCallback((selectedKeys: React.Key[], info: any) => {
    if (selectedKeys.length > 0) {
      const elementId = selectedKeys[0] as string
      if (onSelect) {
        onSelect(elementId, info.nativeEvent?.ctrlKey || info.nativeEvent?.metaKey)
      } else {
        selectElement(elementId, !(info.nativeEvent?.ctrlKey || info.nativeEvent?.metaKey))
      }
    }
  }, [onSelect, selectElement])

  // 处理拖动分割线
  const handleMouseDown = useCallback((e: React.MouseEvent) => {
    e.preventDefault()
    setIsDragging(true)

    const startY = e.clientY
    const startPos = splitPos

    const handleMouseMove = (e: MouseEvent) => {
      const container = document.querySelector('.dual-tree-container')
      if (!container) return

      const rect = container.getBoundingClientRect()
      const newPos = ((e.clientY - rect.top) / rect.height) * 100
      setSplitPos(Math.min(Math.max(20, newPos), 80)) // 限制在20%-80%之间
    }

    const handleMouseUp = () => {
      setIsDragging(false)
      document.removeEventListener('mousemove', handleMouseMove)
      document.removeEventListener('mouseup', handleMouseUp)
    }

    document.addEventListener('mousemove', handleMouseMove)
    document.addEventListener('mouseup', handleMouseUp)
  }, [splitPos])

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '50px' }}>
        <Spin size="large" tip="加载中..." />
      </div>
    )
  }

  return (
    <div 
      className="dual-tree-container"
      style={{ 
        height: '100%', 
        display: 'flex', 
        flexDirection: 'column',
        position: 'relative'
      }}
    >
      {/* 上部：Definition树 */}
      <div style={{ 
        height: `${splitPos}%`, 
        overflow: 'auto',
        borderBottom: '1px solid #d9d9d9',
        padding: '8px'
      }}>
        <Space direction="vertical" style={{ width: '100%' }}>
          <Title level={5} style={{ margin: 0 }}>
            需求定义 (RequirementDefinition)
          </Title>
          
          {showSearch && (
            <Search
              placeholder="搜索需求定义..."
              prefix={<SearchOutlined />}
              allowClear
              value={defSearchValue}
              onChange={(e) => setDefSearchValue(e.target.value)}
              style={{ marginBottom: 8 }}
            />
          )}

          {definitionTreeData.length > 0 ? (
            <Tree
              treeData={definitionTreeData}
              selectedKeys={Array.from(selectedIds)}
              onSelect={handleSelect}
              showLine
              defaultExpandAll
              style={{ background: '#fff' }}
            />
          ) : (
            <Empty 
              description={defSearchValue ? "未找到匹配的需求定义" : "暂无需求定义"} 
              style={{ padding: '20px' }}
            />
          )}
        </Space>
      </div>

      {/* 分割线 */}
      <div
        style={{
          height: '4px',
          background: isDragging ? '#1890ff' : '#d9d9d9',
          cursor: 'ns-resize',
          transition: isDragging ? 'none' : 'background 0.2s'
        }}
        onMouseDown={handleMouseDown}
      />

      {/* 下部：Usage树 */}
      <div style={{ 
        flex: 1, 
        overflow: 'auto',
        padding: '8px'
      }}>
        <Space direction="vertical" style={{ width: '100%' }}>
          <Title level={5} style={{ margin: 0 }}>
            需求使用 (RequirementUsage)
          </Title>
          
          {showSearch && (
            <Search
              placeholder="搜索需求使用..."
              prefix={<SearchOutlined />}
              allowClear
              value={usageSearchValue}
              onChange={(e) => setUsageSearchValue(e.target.value)}
              style={{ marginBottom: 8 }}
            />
          )}

          {usageTreeData.length > 0 ? (
            <Tree
              treeData={usageTreeData}
              selectedKeys={Array.from(selectedIds)}
              onSelect={handleSelect}
              showLine
              defaultExpandAll
              style={{ background: '#fff' }}
            />
          ) : (
            <Empty 
              description={usageSearchValue ? "未找到匹配的需求使用" : "暂无需求使用"} 
              style={{ padding: '20px' }}
            />
          )}
        </Space>
      </div>
    </div>
  )
}

export default DualTreeView