/**
 * ModelViewer组件 - 三视图集成展示
 * 
 * 功能特性：
 * - REQ-A1-1: 三视图选中联动
 * - REQ-A1-3: 性能优化（500节点，响应<100ms）
 * - 视图切换
 * - 数据同步
 */

import React, { useState, useCallback, useEffect } from 'react'
import { Tabs, Layout, Space, Button, message } from 'antd'
import { 
  TreeOutlined,
  TableOutlined,
  PartitionOutlined,
  ReloadOutlined,
  FullscreenOutlined,
  FullscreenExitOutlined
} from '../utils/icons'
import { ModelProvider, useModelContext } from '../contexts/ModelContext'
import TreeView from './tree/TreeView'
import TableView from './table/TableView'
import GraphView from './graph/GraphView'

const { Content, Sider } = Layout

/**
 * 视图容器组件 - 处理视图间的选中联动
 */
const ViewContainer: React.FC = () => {
  const { 
    selectedIds, 
    selectElement, 
    clearSelection,
    loadAllElements,
    loading 
  } = useModelContext()
  
  const [activeView, setActiveView] = useState('split')
  const [siderCollapsed, setSiderCollapsed] = useState(false)
  const [fullscreen, setFullscreen] = useState(false)

  // 初始加载数据
  useEffect(() => {
    loadAllElements()
  }, [loadAllElements])

  // 处理选中事件（所有视图共用）
  const handleSelect = useCallback((elementId: string, isMultiSelect?: boolean) => {
    const startTime = performance.now()
    
    selectElement(elementId, !isMultiSelect)
    
    const endTime = performance.now()
    const responseTime = endTime - startTime
    
    // 性能监控
    if (responseTime > 100) {
      console.warn(`选中响应时间超过100ms: ${responseTime.toFixed(2)}ms`)
    }
  }, [selectElement])

  // 刷新数据
  const handleRefresh = useCallback(() => {
    clearSelection()
    loadAllElements()
    message.success('数据已刷新')
  }, [clearSelection, loadAllElements])

  // 切换全屏
  const toggleFullscreen = useCallback(() => {
    setFullscreen(prev => !prev)
  }, [])

  // 视图标签配置
  const tabItems = [
    {
      key: 'tree',
      label: (
        <span>
          <TreeOutlined />
          树视图
        </span>
      ),
      children: <TreeView onSelect={handleSelect} />
    },
    {
      key: 'table',
      label: (
        <span>
          <TableOutlined />
          表格视图
        </span>
      ),
      children: <TableView editable searchable />
    },
    {
      key: 'graph',
      label: (
        <span>
          <PartitionOutlined />
          图视图
        </span>
      ),
      children: <GraphView onNodeSelect={handleSelect} showMinimap showControls />
    },
    {
      key: 'split',
      label: '分屏视图',
      children: (
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', height: '100%' }}>
          <div style={{ border: '1px solid #d9d9d9', borderRadius: '4px', overflow: 'auto' }}>
            <TreeView onSelect={handleSelect} />
          </div>
          <div style={{ display: 'grid', gridTemplateRows: '1fr 1fr', gap: '16px' }}>
            <div style={{ border: '1px solid #d9d9d9', borderRadius: '4px', overflow: 'auto' }}>
              <TableView editable={false} searchable size="small" />
            </div>
            <div style={{ border: '1px solid #d9d9d9', borderRadius: '4px', overflow: 'auto' }}>
              <GraphView onNodeSelect={handleSelect} showMinimap={false} showControls={false} />
            </div>
          </div>
        </div>
      )
    }
  ]

  return (
    <Layout style={{ height: fullscreen ? '100vh' : 'calc(100vh - 64px)', position: fullscreen ? 'fixed' : 'relative', top: 0, left: 0, right: 0, zIndex: fullscreen ? 1000 : 'auto' }}>
      <Sider 
        collapsible 
        collapsed={siderCollapsed} 
        onCollapse={setSiderCollapsed}
        theme="light"
        width={240}
        style={{ borderRight: '1px solid #f0f0f0' }}
      >
        <div style={{ padding: '16px' }}>
          <h3 style={{ margin: 0, fontSize: '16px' }}>
            {siderCollapsed ? 'MVP' : 'SysML v2 MVP'}
          </h3>
        </div>
        <div style={{ padding: '0 16px' }}>
          <Space direction="vertical" style={{ width: '100%' }}>
            <Button
              icon={<ReloadOutlined />}
              onClick={handleRefresh}
              loading={loading}
              block
              size={siderCollapsed ? 'small' : 'middle'}
            >
              {!siderCollapsed && '刷新数据'}
            </Button>
            <Button
              icon={fullscreen ? <FullscreenExitOutlined /> : <FullscreenOutlined />}
              onClick={toggleFullscreen}
              block
              size={siderCollapsed ? 'small' : 'middle'}
            >
              {!siderCollapsed && (fullscreen ? '退出全屏' : '全屏')}
            </Button>
          </Space>
          
          {!siderCollapsed && selectedIds.size > 0 && (
            <div style={{ marginTop: '16px', padding: '8px', background: '#f0f0f0', borderRadius: '4px' }}>
              <div style={{ fontSize: '12px', color: '#666' }}>已选中</div>
              <div style={{ fontWeight: 'bold', color: '#1890ff' }}>{selectedIds.size} 项</div>
              <Button size="small" onClick={clearSelection} style={{ marginTop: '8px' }}>
                清除选中
              </Button>
            </div>
          )}
        </div>
      </Sider>
      
      <Content style={{ padding: '16px' }}>
        <Tabs 
          activeKey={activeView} 
          onChange={setActiveView}
          items={tabItems}
          style={{ height: '100%' }}
          tabBarStyle={{ marginBottom: '16px' }}
        />
      </Content>
    </Layout>
  )
}

/**
 * ModelViewer主组件 - 提供Context包装
 */
const ModelViewer: React.FC = () => {
  return (
    <ModelProvider>
      <ViewContainer />
    </ModelProvider>
  )
}

export default ModelViewer