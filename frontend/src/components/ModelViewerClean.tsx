/**
 * ModelViewer组件 - 清理重构版
 * 三视图集成展示，不使用有问题的图标导入
 */

import React, { useState, useCallback, useEffect } from 'react'
import { Tabs, Layout, Space, Button, message, Typography } from 'antd'
import { ModelProvider, useModelContext } from '../contexts/ModelContext'
import TreeViewSimple from './tree/TreeViewSimple'
import SimpleGraph from './graph/SimpleGraph'

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
  const [demoData, setDemoData] = useState<any>(null)
  const [dataLoading, setDataLoading] = useState(true)
  const [dataSource, setDataSource] = useState<'small' | 'battery'>('small')

  // 根据数据源加载数据
  useEffect(() => {
    console.log(`开始加载${dataSource === 'battery' ? '电池系统' : '测试'}数据...`)
    setDataLoading(true)
    
    // 根据选择的数据源加载数据
    const loadDemoData = async () => {
      try {
        const url = dataSource === 'battery' 
          ? 'http://localhost:8080/api/v1/demo/battery-system'
          : 'http://localhost:8080/api/v1/demo/dataset/small'
        
        console.log('正在请求:', url)
        const response = await fetch(url)
        console.log('响应状态:', response.status, response.statusText)
        
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`)
        }
        
        const data = await response.json()
        console.log('数据加载成功:', data)
        setDemoData(data)
        
        // 解析数据中的content数组
        if (data && data.content && Array.isArray(data.content)) {
          console.log(`找到 ${data.content.length} 个元素`)
        }
      } catch (error) {
        console.error('数据加载失败:', error)
      } finally {
        setDataLoading(false)
      }
    }
    
    loadDemoData()
  }, [dataSource])

  // 处理选中事件
  const handleSelect = useCallback((elementId: string, isMultiSelect?: boolean) => {
    const startTime = performance.now()
    selectElement(elementId, !isMultiSelect)
    const endTime = performance.now()
    
    if (endTime - startTime > 100) {
      console.warn(`选中响应时间超过100ms: ${(endTime - startTime).toFixed(2)}ms`)
    }
  }, [selectElement])

  // 刷新数据
  const handleRefresh = useCallback(() => {
    clearSelection()
    loadAllElements()
    message.success('数据已刷新')
  }, [clearSelection, loadAllElements])

  // 简单的表格视图组件
  const SimpleTableView = () => {
    // 从demo数据中提取元素列表
    const elementsList = demoData?.content || []
    const elementCount = elementsList.length
    
    return (
      <div style={{ padding: '20px' }}>
        <Title level={4}>表格视图</Title>
        <p>元素总数: {elementCount}</p>
        <p>加载状态: {dataLoading ? '加载中...' : '已加载'}</p>
        {elementsList.length > 0 ? (
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ borderBottom: '2px solid #f0f0f0' }}>
                <th style={{ padding: '8px', textAlign: 'left' }}>ID</th>
                <th style={{ padding: '8px', textAlign: 'left' }}>名称</th>
                <th style={{ padding: '8px', textAlign: 'left' }}>类型</th>
                <th style={{ padding: '8px', textAlign: 'left' }}>文档</th>
              </tr>
            </thead>
            <tbody>
              {elementsList.slice(0, 10).map((item: any, index: number) => {
                const element = item.data || {}
                return (
                  <tr key={element.elementId || index} style={{ borderBottom: '1px solid #f0f0f0' }}>
                    <td style={{ padding: '8px' }}>{element.elementId || '未知'}</td>
                    <td style={{ padding: '8px' }}>{element.name || '未命名'}</td>
                    <td style={{ padding: '8px' }}>{item.eClass || '未知类型'}</td>
                    <td style={{ padding: '8px', maxWidth: '300px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {element.documentation || '无文档'}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        ) : (
          <p style={{ color: '#999' }}>{dataLoading ? '加载中...' : '暂无数据'}</p>
        )}
      </div>
    )
  }

  // 使用简化的图视图组件
  const GraphView = () => {
    return (
      <div style={{ padding: '20px' }}>
        <Title level={4}>依赖关系图</Title>
        <SimpleGraph onNodeSelect={handleSelect} dataSource={dataSource} />
      </div>
    )
  }

  // 标签配置 - 使用emoji代替图标
  const tabItems = [
    {
      key: 'tree',
      label: '🌳 树视图',
      children: <TreeViewSimple onSelect={handleSelect} dataSource={dataSource} />
    },
    {
      key: 'table',
      label: '📊 表格视图',  
      children: <SimpleTableView />
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
            <TreeViewSimple onSelect={handleSelect} showSearch={false} dataSource={dataSource} />
          </div>
          <div style={{ display: 'grid', gridTemplateRows: '1fr 1fr', gap: '16px' }}>
            <div style={{ border: '1px solid #d9d9d9', borderRadius: '4px', overflow: 'auto' }}>
              <SimpleTableView />
            </div>
            <div style={{ border: '1px solid #d9d9d9', borderRadius: '4px', overflow: 'hidden', padding: '10px' }}>
              <SimpleGraph onNodeSelect={handleSelect} dataSource={dataSource} />
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
            {siderCollapsed ? 'MVP' : 'SysML v2 MVP'}
          </Title>
        </div>
        
        {!siderCollapsed && (
          <div style={{ padding: '16px' }}>
            <Space direction="vertical" style={{ width: '100%' }}>
              <div style={{ marginBottom: '16px' }}>
                <div style={{ fontSize: '12px', color: '#666', marginBottom: '8px' }}>数据源</div>
                <Space.Compact style={{ width: '100%' }}>
                  <Button
                    type={dataSource === 'small' ? 'primary' : 'default'}
                    onClick={() => setDataSource('small')}
                    style={{ width: '50%' }}
                  >
                    测试数据
                  </Button>
                  <Button
                    type={dataSource === 'battery' ? 'primary' : 'default'}
                    onClick={() => setDataSource('battery')}
                    style={{ width: '50%' }}
                  >
                    🔋 电池系统
                  </Button>
                </Space.Compact>
              </div>
              
              <Button
                onClick={handleRefresh}
                loading={loading}
                block
                type="primary"
              >
                🔄 刷新数据
              </Button>
              
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