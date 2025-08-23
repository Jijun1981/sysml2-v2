import React, { useMemo } from 'react'
import { Tree, Card } from 'antd'
import { FileOutlined, FolderOutlined } from '@ant-design/icons'
import { useModel } from '../../../contexts/ModelContext'
import type { DataNode } from 'antd/es/tree'
import './TreeView.css'

/**
 * 树视图组件
 * REQ-D1-1: 树视图接口
 * REQ-D1-3: 联动 - 选中项高亮
 */
const TreeView: React.FC = () => {
  const { requirements, usages, selectedId, selectElement } = useModel()
  
  // 构建树形数据
  const treeData = useMemo(() => {
    const nodes: DataNode[] = requirements.map(req => {
      // 找到属于这个Definition的Usage
      const children = usages
        .filter(usage => usage.of === req.id)
        .map(usage => ({
          key: usage.id,
          title: usage.name,
          icon: <FileOutlined />,
          children: []
        }))
      
      return {
        key: req.id,
        title: `${req.reqId}: ${req.name}`,
        icon: <FolderOutlined />,
        children
      }
    })
    
    return nodes
  }, [requirements, usages])
  
  // 处理选择事件
  const handleSelect = (selectedKeys: React.Key[]) => {
    const id = selectedKeys[0] as string
    selectElement(id || null)
  }
  
  return (
    <Card className="tree-view-card">
      <Tree
        showIcon
        defaultExpandAll
        selectedKeys={selectedId ? [selectedId] : []}
        onSelect={handleSelect}
        treeData={treeData}
      />
    </Card>
  )
}

export default TreeView