import React, { useMemo, useEffect } from 'react'
import { Tree, Card, Spin } from 'antd'
import { FileOutlined, FolderOutlined } from '@ant-design/icons'
import { useModelContext } from '../../../contexts/ModelContext'
import type { DataNode } from 'antd/es/tree'
import './TreeView.css'

/**
 * 树视图组件 - 基于通用接口和SSOT实现
 * REQ-D1-1: 树视图接口
 * REQ-D1-3: 联动 - 选中项高亮
 * REQ-A1-1: 数据源唯一 - 从SSOT获取数据
 */
const TreeView: React.FC = () => {
  const { 
    elements, 
    selectedIds, 
    loading,
    selectElement, 
    getTreeViewData,
    loadElementsByType 
  } = useModelContext()
  
  // 初始加载数据
  useEffect(() => {
    const loadData = async () => {
      try {
        await loadElementsByType('RequirementDefinition')
        await loadElementsByType('RequirementUsage')
      } catch (error) {
        console.error('TreeView: 加载数据失败', error)
      }
    }
    
    // 如果没有任何元素数据，则加载
    const hasRequirementData = Object.values(elements).some(
      element => element.eClass === 'RequirementDefinition' || element.eClass === 'RequirementUsage'
    )
    
    if (!hasRequirementData) {
      loadData()
    }
  }, []) // 移除循环依赖，只在挂载时执行一次
  
  // 构建树形数据 - 使用ModelContext的视图投影方法
  const treeData = useMemo(() => {
    const treeViewData = getTreeViewData()
    
    const nodes: DataNode[] = treeViewData.definitions.map(def => {
      // 构建Usage子节点
      const usageChildren = (def.usages || []).map(usage => ({
        key: usage.id,
        title: (
          <span data-testid={`tree-node-text-${usage.id}`}>
            {usage.label}
          </span>
        ),
        icon: <FileOutlined />,
        className: selectedIds.has(usage.id) ? 'tree-node-selected' : ''
      }))
      
      return {
        key: def.id,
        title: (
          <span data-testid={`tree-node-text-${def.id}`}>
            {def.label}
          </span>
        ),
        icon: <FolderOutlined />,
        children: usageChildren,
        className: selectedIds.has(def.id) ? 'tree-node-selected expanded' : 'expanded'
      }
    })
    
    return nodes
  }, [elements, selectedIds])  // 直接依赖elements而不是函数
  
  // 处理选择事件 - 支持多选
  const handleSelect = (selectedKeys: React.Key[], info: any) => {
    const id = selectedKeys[0] as string
    const multiSelect = info.nativeEvent?.ctrlKey || info.nativeEvent?.metaKey
    
    if (id) {
      selectElement(id, multiSelect)
    }
  }
  
  // 获取当前选中的keys
  const selectedKeys = Array.from(selectedIds)
  
  return (
    <Card 
      className="tree-view-card" 
      data-testid="tree-view"
      title="需求结构"
    >
      <Spin spinning={loading} tip="加载中...">
        {treeData.length > 0 ? (
          <Tree
            showIcon
            defaultExpandAll
            multiple
            selectedKeys={selectedKeys}
            onSelect={handleSelect}
            treeData={treeData.map(node => ({
              ...node,
              key: node.key,
              // 添加测试标识
              className: `${node.className || ''} tree-node`,
              title: (
                <div data-testid={`tree-node-${node.key}`} className={selectedIds.has(node.key as string) ? 'selected' : ''}>
                  {node.title}
                </div>
              ),
              children: node.children?.map(child => ({
                ...child,
                title: (
                  <div data-testid={`tree-node-${child.key}`} className={selectedIds.has(child.key as string) ? 'selected' : ''}>
                    {child.title}
                  </div>
                )
              }))
            }))}
          />
        ) : (
          <div className="tree-empty-state">
            {loading ? '加载中...' : '暂无需求数据'}
          </div>
        )}
      </Spin>
    </Card>
  )
}

export default TreeView