/**
 * ModelViewer组件 - 清理重构版
 * 三视图集成展示，不使用有问题的图标导入
 */

import React, { useState, useCallback, useEffect } from 'react'
import { Tabs, Layout, Space, Button, message, Typography, Empty } from 'antd'
import { ModelProvider, useModelContext } from '../contexts/ModelContext'
import TreeViewSimple from './tree/TreeViewSimple'
import TableView from './table/TableView'
import SimpleGraph from './graph/SimpleGraph'
import CreateRequirementDialog from './dialogs/CreateRequirementDialog'
import EditRequirementDialog from './dialogs/EditRequirementDialog'

const { Content, Sider } = Layout
const { Title } = Typography

/**
 * 视图容器组件
 */
const ViewContainer: React.FC = () => {
  const { 
    selectedIds, 
    selectElement, 
    clearSelection,
    loadAllElements,
    loading,
    elements 
  } = useModelContext()
  
  const [activeView, setActiveView] = useState('tree')
  const [siderCollapsed, setSiderCollapsed] = useState(false)
  const [createDialogOpen, setCreateDialogOpen] = useState(false)
  const [createDialogType, setCreateDialogType] = useState<'definition' | 'usage'>('definition')
  const [editDialogOpen, setEditDialogOpen] = useState(false)
  const [editingId, setEditingId] = useState<string>('')

  // 初始加载数据
  useEffect(() => {
    console.log('初始化加载后端数据...')
    loadAllElements()
  }, [loadAllElements])

  // 处理选中事件
  const handleSelect = useCallback((elementId: string, isMultiSelect?: boolean) => {
    const startTime = performance.now()
    selectElement(elementId, !isMultiSelect)
    const endTime = performance.now()
    
    if (endTime - startTime > 100) {
      console.warn(`选中响应时间超过100ms: ${(endTime - startTime).toFixed(2)}ms`)
    }
  }, [selectElement])

  // 刷新数据 - 从后端重新加载
  const handleRefresh = useCallback(async () => {
    clearSelection()
    try {
      await loadAllElements()
      message.success('数据已刷新')
    } catch (error) {
      console.error('刷新失败:', error)
      message.error('刷新失败，请重试')
    }
  }, [clearSelection, loadAllElements])


  // 使用简化的图视图组件
  const GraphView = () => {
    return (
      <div style={{ padding: '20px' }}>
        <Title level={4}>依赖关系图</Title>
        <SimpleGraph onNodeSelect={handleSelect} selectedIds={selectedIds} />
      </div>
    )
  }

  // 标签配置 - 使用emoji代替图标
  const tabItems = [
    {
      key: 'tree',
      label: '🌳 树视图',
      children: (
        <div style={{ 
          display: 'flex', 
          height: '100%', 
          gap: '1px',
          background: '#f0f0f0'
        }}>
          {/* 左侧：双树布局 */}
          <div style={{ 
            width: '280px', 
            background: '#fff',
            display: 'flex', 
            flexDirection: 'column',
            borderRight: '1px solid #d9d9d9'
          }}>
            {/* 上部：Usage树 */}
            <div style={{ 
              flex: 1, 
              borderBottom: '1px solid #d9d9d9',
              overflow: 'auto',
              background: '#fff'
            }}>
              <div style={{ 
                padding: '8px 12px', 
                background: '#fafafa',
                borderBottom: '1px solid #d9d9d9',
                fontWeight: 500
              }}>
                📄 需求使用列表
              </div>
              <TreeViewSimple 
                onSelect={handleSelect} 
                filterType="usage"
                placeholder="搜索需求使用..."
                showSearch={false}
              />
            </div>
            {/* 下部：Definition树 */}
            <div style={{ 
              flex: 1, 
              overflow: 'auto',
              background: '#fff'
            }}>
              <div style={{ 
                padding: '8px 12px', 
                background: '#fafafa',
                borderBottom: '1px solid #d9d9d9',
                fontWeight: 500
              }}>
                📦 需求定义库
              </div>
              <TreeViewSimple 
                onSelect={handleSelect} 
                filterType="definition"
                placeholder="搜索需求定义..."
                showSearch={false}
              />
            </div>
          </div>
          {/* 右侧：表格视图 */}
          <div style={{ 
            flex: 1, 
            background: '#fff',
            overflow: 'hidden',
            display: 'flex',
            flexDirection: 'column'
          }}>
            <TableView 
              editable={true} 
              selectable={true} 
              pageable={true}
              pageSize={20}
              searchable={false}
              sortable={true}
              filterable={false}
              showRelation={true}
              usageOnly={true}
              showToolbar={true}
              size="small"
              bordered={true}
            />
          </div>
        </div>
      )
    },
    {
      key: 'table',
      label: '📊 表格视图',  
      children: <TableView 
        editable={true} 
        selectable={true} 
        pageable={true}
        pageSize={20}
        searchable={true}
        sortable={true}
        filterable={true}
        showRelation={true}
        usageOnly={true}  // REQ-UI-2: 只显示Usage
        showToolbar={true} // REQ-UI-3: 显示工具栏
      />
    },
    {
      key: 'graph',
      label: '🔗 图视图',
      children: <GraphView />
    },
    {
      key: 'split',
      label: '⊞ 分屏视图',
      children: (
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', height: '100%' }}>
          <div style={{ border: '1px solid #d9d9d9', borderRadius: '4px', overflow: 'auto' }}>
            <TreeViewSimple onSelect={handleSelect} showSearch={false} />
          </div>
          <div style={{ display: 'grid', gridTemplateRows: '1fr 1fr', gap: '16px' }}>
            <div style={{ border: '1px solid #d9d9d9', borderRadius: '4px', overflow: 'auto' }}>
              <TableView 
                editable={true} 
                selectable={true} 
                size="small" 
                pageable={true}
                pageSize={10}
                usageOnly={true}
                showRelation={true}
              />
            </div>
            <div style={{ border: '1px solid #d9d9d9', borderRadius: '4px', overflow: 'hidden', padding: '10px' }}>
              <SimpleGraph onNodeSelect={handleSelect} selectedIds={selectedIds} />
            </div>
          </div>
        </div>
      )
    }
  ]

  return (
    <Layout style={{ height: '100vh' }}>
      <Sider 
        collapsible 
        collapsed={siderCollapsed} 
        onCollapse={setSiderCollapsed}
        theme="light"
        width={240}
        style={{ borderRight: '1px solid #f0f0f0' }}
      >
        <div style={{ padding: '16px', borderBottom: '1px solid #f0f0f0' }}>
          <Title level={4} style={{ margin: 0 }}>
            {siderCollapsed ? 'SysML' : 'SysML v2 建模平台'}
          </Title>
        </div>
        
        {!siderCollapsed && (
          <div style={{ padding: '16px' }}>
            <Space direction="vertical" style={{ width: '100%' }}>
              <Button
                onClick={handleRefresh}
                loading={loading}
                block
                type="primary"
              >
                🔄 刷新数据
              </Button>
              
              <Space.Compact style={{ width: '100%' }}>
                <Button
                  onClick={() => {
                    setCreateDialogType('definition')
                    setCreateDialogOpen(true)
                  }}
                  block
                  style={{ width: '50%' }}
                >
                  创建需求定义
                </Button>
                <Button
                  onClick={() => {
                    setCreateDialogType('usage')
                    setCreateDialogOpen(true)
                  }}
                  block
                  style={{ width: '50%' }}
                >
                  创建需求使用
                </Button>
              </Space.Compact>
              
              {selectedIds.size > 0 && (
                <div style={{ 
                  padding: '12px', 
                  background: '#e6f7ff', 
                  borderRadius: '4px',
                  border: '1px solid #91d5ff'
                }}>
                  <div style={{ fontWeight: 'bold', marginBottom: '8px' }}>
                    已选中: {selectedIds.size} 项
                  </div>
                  <Button size="small" onClick={clearSelection} block>
                    清除选中
                  </Button>
                </div>
              )}
              
              <div style={{ 
                padding: '12px', 
                background: '#f6ffed', 
                borderRadius: '4px',
                border: '1px solid #b7eb8f'
              }}>
                <div style={{ fontSize: '12px', color: '#52c41a', marginBottom: '4px' }}>
                  数据状态
                </div>
                <div>元素总数: {elements instanceof Map ? elements.size : 0}</div>
                <div>当前视图: {activeView}</div>
              </div>
            </Space>
          </div>
        )}
      </Sider>
      
      <Content style={{ background: '#fff', overflow: 'hidden' }}>
        <div style={{ padding: '16px', borderBottom: '1px solid #f0f0f0' }}>
          <Title level={3} style={{ margin: 0 }}>模型浏览器</Title>
        </div>
        <div style={{ padding: '16px', height: 'calc(100% - 64px)' }}>
          <Tabs 
            activeKey={activeView} 
            onChange={setActiveView}
            items={tabItems}
            size="large"
            style={{ height: '100%' }}
          />
        </div>
      </Content>
      
      {/* 创建需求对话框 */}
      <CreateRequirementDialog
        open={createDialogOpen}
        onClose={() => setCreateDialogOpen(false)}
        onSuccess={async () => {
          await handleRefresh()
          message.success('创建成功')
        }}
        type={createDialogType}
      />
      
      {/* 编辑需求对话框 */}
      <EditRequirementDialog
        open={editDialogOpen}
        onClose={() => {
          setEditDialogOpen(false)
          setEditingId('')
        }}
        onSuccess={() => {
          handleRefresh()
          message.success('更新成功')
        }}
        requirementId={editingId}
      />
    </Layout>
  )
}

/**
 * ModelViewer主组件 - 清理重构版
 */
const ModelViewerClean: React.FC = () => {
  return (
    <ModelProvider>
      <ViewContainer />
    </ModelProvider>
  )
}

export default ModelViewerClean