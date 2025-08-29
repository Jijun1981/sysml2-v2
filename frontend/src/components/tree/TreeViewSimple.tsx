/**
 * TreeView简化版 - 不使用图标
 */

import React, { useState, useCallback, useMemo, useEffect } from 'react'
import { Tree, Input, Typography, Space } from 'antd'
import { useModelContext } from '../../contexts/ModelContext'
import type { TreeProps } from 'antd'

const { Title } = Typography
const { Search } = Input

interface TreeViewProps {
  multiSelect?: boolean
  showSearch?: boolean
  onSelect?: (selectedId: string) => void
}

const TreeViewSimple: React.FC<TreeViewProps> = ({
  multiSelect = false,
  showSearch = true,
  onSelect
}) => {
  const { 
    selectedIds, 
    selectElement,
    loading,
    elements 
  } = useModelContext()
  
  const [expandedKeys, setExpandedKeys] = useState<React.Key[]>([])
  const [searchValue, setSearchValue] = useState('')

  // 使用ModelContext中的elements数据
  useEffect(() => {
    if (elements && Object.keys(elements).length > 0) {
      console.log('TreeView - 使用ModelContext数据', Object.keys(elements).length, '个元素')
      // 展开所有节点
      const allKeys = Object.keys(elements)
      setExpandedKeys(allKeys)
    }
  }, [elements])

  // 转换为树结构
  const treeData = useMemo(() => {
    if (!elements || Object.keys(elements).length === 0) return []
    
    const treeNodes = Object.values(elements).map((item: any) => ({
      key: item.id || item.elementId,
      title: item.attributes?.declaredName || 
             item.attributes?.reqId || 
             item.declaredName || 
             item.reqId || 
             item.attributes?.documentation || 
             item.documentation || 
             '未命名元素',
      children: []
    }))
    
    console.log('TreeView - 转换后的树节点:', treeNodes)
    return treeNodes
  }, [elements])

  const handleSelect: TreeProps['onSelect'] = useCallback((selectedKeys, info) => {
    const key = info.node.key as string
    selectElement(key, multiSelect)
    onSelect?.(key)
  }, [selectElement, multiSelect, onSelect])

  const handleExpand: TreeProps['onExpand'] = useCallback((expandedKeysValue) => {
    setExpandedKeys(expandedKeysValue)
  }, [])

  if (loading) {
    return <div style={{ padding: '16px', textAlign: 'center' }}>加载中...</div>
  }

  return (
    <div style={{ padding: '16px' }}>
      <Title level={4}>需求树视图</Title>
      {showSearch && (
        <Search
          style={{ marginBottom: 8 }}
          placeholder="搜索需求"
          onChange={e => setSearchValue(e.target.value)}
        />
      )}
      <Tree
        treeData={treeData}
        selectedKeys={Array.from(selectedIds)}
        expandedKeys={expandedKeys}
        onSelect={handleSelect}
        onExpand={handleExpand}
        multiple={multiSelect}
      />
    </div>
  )
}

export default TreeViewSimple