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
  dataSource?: 'small' | 'battery'
}

const TreeViewSimple: React.FC<TreeViewProps> = ({
  multiSelect = false,
  showSearch = true,
  onSelect,
  dataSource = 'small'
}) => {
  const { 
    selectedIds, 
    selectElement,
    loading 
  } = useModelContext()
  
  const [expandedKeys, setExpandedKeys] = useState<React.Key[]>([])
  const [searchValue, setSearchValue] = useState('')
  const [demoData, setDemoData] = useState<any[]>([])

  // 加载demo数据
  useEffect(() => {
    const url = dataSource === 'battery' 
      ? 'http://localhost:8080/api/v1/demo/battery-system'
      : 'http://localhost:8080/api/v1/demo/dataset/small'
    
    fetch(url)
      .then(res => res.json())
      .then(data => {
        console.log('TreeView - Demo数据加载成功', data)
        if (data?.content) {
          // 转换demo数据为树结构
          const treeNodes = data.content.map((item: any) => ({
            key: item.data?.elementId || Math.random().toString(),
            title: item.data?.declaredName || item.data?.reqId || item.data?.documentation || '未命名元素',
            children: []
          }))
          console.log('TreeView - 转换后的树节点:', treeNodes)
          setDemoData(treeNodes)
          // 默认展开所有节点
          setExpandedKeys(treeNodes.map((n: any) => n.key))
        }
      })
      .catch(err => console.error('TreeView数据加载失败:', err))
  }, [dataSource])

  const treeData = useMemo(() => {
    return demoData
  }, [demoData])

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
        treeData={treeData as any}
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