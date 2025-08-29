/**
 * RequirementExplorer - 需求浏览器组件
 * 
 * 基于设计文档requirement-modeling-architecture-v2.md实现
 * 双树结构：上树显示Usage（实际需求实例），下树显示Definition（需求模板）
 */

import React, { useState, useCallback, useEffect } from 'react'
import { Layout, Tabs, Tree, List, Card, Button, message, Divider, Typography, Space, Empty, Spin } from 'antd'
import { 
  FolderOutlined, 
  FileTextOutlined, 
  PlusOutlined, 
  EditOutlined,
  DeleteOutlined,
  CopyOutlined
} from '@ant-design/icons'
import { useModelContext } from '../contexts/ModelContext'
import CreateRequirementDialog from './dialogs/CreateRequirementDialog'
import EditRequirementDialog from './dialogs/EditRequirementDialog'
import { requirementService } from '../services/requirementService'

const { Sider, Content } = Layout
const { TabPane } = Tabs
const { Title, Text } = Typography

interface RequirementExplorerProps {
  onUsageSelect?: (id: string) => void
  onDefinitionDragStart?: (definition: any) => void
}

/**
 * 需求浏览器主组件 - 实现双树结构
 */
const RequirementExplorer: React.FC<RequirementExplorerProps> = ({ 
  onUsageSelect, 
  onDefinitionDragStart 
}) => {
  // 状态管理
  const { loadAllElements, createElement, updateElement, deleteElement } = useModelContext()
  const [usages, setUsages] = useState<any[]>([])
  const [definitions, setDefinitions] = useState<any[]>([])
  const [loading, setLoading] = useState(false)
  const [selectedUsageKey, setSelectedUsageKey] = useState<string>('')
  const [selectedDefinitionKey, setSelectedDefinitionKey] = useState<string>('')
  
  // 对话框状态
  const [createDialogOpen, setCreateDialogOpen] = useState(false)
  const [createDialogType, setCreateDialogType] = useState<'definition' | 'usage'>('definition')
  const [editDialogOpen, setEditDialogOpen] = useState(false)
  const [editingId, setEditingId] = useState<string>('')

  // 加载需求数据
  const loadRequirements = useCallback(async () => {
    setLoading(true)
    try {
      // 加载所有RequirementDefinition
      const defsResponse = await fetch('http://localhost:8080/api/v1/requirements?page=0&size=100')
      const defsData = await defsResponse.json()
      
      // 加载所有RequirementUsage
      const usagesResponse = await fetch('http://localhost:8080/api/v1/requirements/usages?page=0&size=100')
      const usagesData = await usagesResponse.json()
      
      // 转换为树形数据格式
      const defTree = buildDefinitionTree(defsData)
      const usageTree = buildUsageTree(usagesData)
      
      setDefinitions(defTree)
      setUsages(usageTree)
    } catch (error) {
      console.error('加载需求失败:', error)
      message.error('加载需求数据失败')
    } finally {
      setLoading(false)
    }
  }, [])

  // 构建Definition树
  const buildDefinitionTree = (definitions: any[]): any[] => {
    // Definition是扁平列表，按分类组织
    const categories: Record<string, any[]> = {
      '功能需求': [],
      '性能需求': [],
      '安全需求': [],
      '其他': []
    }
    
    definitions.forEach(def => {
      const category = detectCategory(def.declaredName || def.reqId || '')
      const node = {
        key: def.elementId || def.id,
        title: def.declaredName || def.reqId || '未命名',
        icon: <FileTextOutlined />,
        data: def,
        isLeaf: true
      }
      categories[category].push(node)
    })
    
    return Object.entries(categories)
      .filter(([_, items]) => items.length > 0)
      .map(([category, items]) => ({
        key: `category-${category}`,
        title: `${category} (${items.length})`,
        icon: <FolderOutlined />,
        children: items
      }))
  }

  // 构建Usage树（基于derive关系）
  const buildUsageTree = (usages: any[]): any[] => {
    // TODO: 基于derive/refine关系构建真正的树形结构
    // 当前简化实现：扁平列表
    const rootNodes = usages.map(usage => ({
      key: usage.elementId || usage.id,
      title: usage.declaredName || '未命名需求',
      icon: <FileTextOutlined />,
      data: usage,
      isLeaf: true
    }))
    
    // 包装成技术规格书节点
    return [{
      key: 'tech-spec-root',
      title: '技术规格书：默认项目',
      icon: <FolderOutlined />,
      children: rootNodes
    }]
  }

  // 检测需求类别
  const detectCategory = (name: string): string => {
    const nameLower = name.toLowerCase()
    if (nameLower.includes('性能') || nameLower.includes('performance')) return '性能需求'
    if (nameLower.includes('安全') || nameLower.includes('security') || nameLower.includes('safety')) return '安全需求'
    if (nameLower.includes('功能') || nameLower.includes('function')) return '功能需求'
    return '其他'
  }

  // 初始化加载
  useEffect(() => {
    loadRequirements()
  }, [loadRequirements])

  // Usage树选择处理
  const handleUsageSelect = (selectedKeys: React.Key[]) => {
    if (selectedKeys.length > 0) {
      const key = selectedKeys[0].toString()
      setSelectedUsageKey(key)
      onUsageSelect?.(key)
    }
  }

  // Definition拖拽处理
  const handleDefinitionDragStart = (info: any) => {
    const nodeData = info.node.data
    if (nodeData && onDefinitionDragStart) {
      onDefinitionDragStart(nodeData)
    }
  }

  // 从Definition创建Usage
  const handleCreateUsageFromDefinition = (definitionId: string) => {
    setCreateDialogType('usage')
    setEditingId(definitionId)
    setCreateDialogOpen(true)
  }

  // 刷新数据
  const handleRefresh = () => {
    loadRequirements()
    message.success('数据已刷新')
  }

  return (
    <Layout style={{ height: '100%' }}>
      <Sider width={350} theme="light" style={{ borderRight: '1px solid #f0f0f0' }}>
        <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
          {/* 上部：Usage树 */}
          <div style={{ flex: 1, padding: '16px', borderBottom: '1px solid #f0f0f0', overflow: 'auto' }}>
            <div style={{ marginBottom: '8px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <Title level={5} style={{ margin: 0 }}>📋 需求实例 (Usage)</Title>
              <Space>
                <Button 
                  size="small" 
                  icon={<PlusOutlined />}
                  onClick={() => {
                    setCreateDialogType('usage')
                    setCreateDialogOpen(true)
                  }}
                >
                  新建
                </Button>
                <Button size="small" onClick={handleRefresh}>刷新</Button>
              </Space>
            </div>
            <Spin spinning={loading}>
              {usages.length > 0 ? (
                <Tree
                  showIcon
                  defaultExpandAll
                  selectedKeys={[selectedUsageKey]}
                  onSelect={handleUsageSelect}
                  treeData={usages}
                />
              ) : (
                <Empty description="暂无需求实例" />
              )}
            </Spin>
          </div>

          <Divider style={{ margin: 0 }} />

          {/* 下部：Definition库 */}
          <div style={{ flex: 1, padding: '16px', overflow: 'auto' }}>
            <div style={{ marginBottom: '8px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <Title level={5} style={{ margin: 0 }}>📚 需求模板 (Definition)</Title>
              <Space>
                <Button 
                  size="small" 
                  icon={<PlusOutlined />}
                  onClick={() => {
                    setCreateDialogType('definition')
                    setCreateDialogOpen(true)
                  }}
                >
                  新建
                </Button>
              </Space>
            </div>
            <Tabs defaultActiveKey="templates" size="small">
              <TabPane tab="需求模板" key="templates">
                <Spin spinning={loading}>
                  {definitions.length > 0 ? (
                    <Tree
                      showIcon
                      defaultExpandAll
                      draggable
                      selectedKeys={[selectedDefinitionKey]}
                      onSelect={(keys) => setSelectedDefinitionKey(keys[0]?.toString() || '')}
                      onDragStart={handleDefinitionDragStart}
                      treeData={definitions}
                      titleRender={(nodeData: any) => (
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <span>{nodeData.title}</span>
                          {nodeData.isLeaf && (
                            <Button 
                              size="small" 
                              type="link"
                              icon={<CopyOutlined />}
                              onClick={(e) => {
                                e.stopPropagation()
                                handleCreateUsageFromDefinition(nodeData.key)
                              }}
                              title="基于此模板创建Usage"
                            />
                          )}
                        </div>
                      )}
                    />
                  ) : (
                    <Empty description="暂无需求模板" />
                  )}
                </Spin>
              </TabPane>
              <TabPane tab="需求包" key="packages">
                <Empty description="需求包功能开发中..." />
              </TabPane>
            </Tabs>
          </div>
        </div>
      </Sider>

      {/* 创建需求对话框 */}
      <CreateRequirementDialog
        open={createDialogOpen}
        onClose={() => {
          setCreateDialogOpen(false)
          setEditingId('')
        }}
        onSuccess={() => {
          handleRefresh()
          message.success('创建成功')
        }}
        type={createDialogType}
        definitionId={createDialogType === 'usage' ? editingId : undefined}
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

export default RequirementExplorer