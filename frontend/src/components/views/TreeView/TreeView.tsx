import React, { useMemo } from 'react'
import { Tree, Card, Empty, Spin } from 'antd'
import { FileOutlined, FolderOutlined } from '@ant-design/icons'
import { useModelContext } from '../../../contexts/ModelContext'
import type { DataNode } from 'antd/es/tree'
import './TreeView.css'

/**
 * 树视图组件 - 完全基于SSOT和通用接口
 * 需求实现：
 * - REQ-D1-1: 从通用接口数据构建树结构
 * - REQ-D1-2: 通过通用接口进行写回
 * - REQ-D1-3: 通过Context实现联动
 * - REQ-A1-1: 作为SSOT的投影视图
 */
const TreeView: React.FC = () => {
  const { 
    elements, 
    selectedIds, 
    loading,
    selectElement,
    loadAllElements
  } = useModelContext()
  
  // REQ-D1-1: 从SSOT构建树形数据结构
  const treeData = useMemo(() => {
    const nodes: DataNode[] = []
    
    // 先找出所有Definition作为父节点
    const definitions = Object.values(elements).filter(
      el => el.eClass === 'RequirementDefinition'
    )
    
    definitions.forEach(def => {
      // 找出关联的Usage作为子节点
      const usages = Object.values(elements).filter(
        el => el.eClass === 'RequirementUsage' && el.attributes?.of === def.id
      )
      
      const defNode: DataNode = {
        key: def.id,
        title: def.attributes?.declaredName || def.attributes?.declaredShortName || def.id,
        icon: <FolderOutlined />,
        className: selectedIds.has(def.id) ? 'tree-node selected' : 'tree-node',
        children: usages.map(usage => ({
          key: usage.id,
          title: usage.attributes?.declaredName || usage.attributes?.declaredShortName || usage.id,
          icon: <FileOutlined />,
          className: selectedIds.has(usage.id) ? 'tree-node selected' : 'tree-node'
        }))
      }
      
      nodes.push(defNode)
    })
    
    // 添加没有父节点的Usage（孤立的）
    const orphanUsages = Object.values(elements).filter(
      el => el.eClass === 'RequirementUsage' && !el.attributes?.of
    )
    
    orphanUsages.forEach(usage => {
      nodes.push({
        key: usage.id,
        title: usage.attributes?.declaredName || usage.attributes?.declaredShortName || usage.id,
        icon: <FileOutlined />,
        className: selectedIds.has(usage.id) ? 'tree-node selected' : 'tree-node'
      })
    })
    
    return nodes
  }, [elements, selectedIds])
  
  // REQ-D1-3: 处理选择事件 - 通过Context管理状态
  const handleSelect = (selectedKeys: React.Key[], info: any) => {
    if (selectedKeys.length > 0) {
      const id = selectedKeys[0] as string
      const multiSelect = info.nativeEvent?.ctrlKey || info.nativeEvent?.metaKey
      selectElement(id, multiSelect)
    }
  }
  
  // 初始加载数据（如果需要）
  React.useEffect(() => {
    if (Object.keys(elements).length === 0 && !loading) {
      loadAllElements()
    }
  }, [])
  
  return (
    <Card 
      className="tree-view-card" 
      title="需求结构"
      size="small"
    >
      <Spin spinning={loading}>
        {treeData.length > 0 ? (
          <Tree
            showIcon
            defaultExpandAll
            selectedKeys={Array.from(selectedIds)}
            onSelect={handleSelect}
            treeData={treeData}
          />
        ) : (
          <Empty 
            description={loading ? "加载中..." : "暂无需求数据"}
            image={Empty.PRESENTED_IMAGE_SIMPLE}
          />
        )}
      </Spin>
    </Card>
  )
}

export default TreeView