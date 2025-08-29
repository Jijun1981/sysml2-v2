/**
 * TreeView组件 - REQ-F2-1: 树视图数据加载修复
 * 
 * 功能特性：
 * - 从requirementService加载真实数据
 * - 显示RequirementDefinition和RequirementUsage
 * - Usage节点显示关联的Definition（通过requirementDefinition字段）
 * - 按类型分组显示
 * - 展开/折叠交互
 * - 选中状态同步
 * - 搜索过滤
 * - 虚拟滚动优化
 */

import React, { useState, useCallback, useMemo, useEffect } from 'react'
import { Tree, Input, Typography, Space, Spin, Alert, Badge, Tooltip } from 'antd'
import { 
  FolderOutlined, 
  FileTextOutlined, 
  SearchOutlined,
  FolderOpenOutlined,
  LinkOutlined
} from '@ant-design/icons'
import { useModelContext } from '../../contexts/ModelContext'
import { requirementService } from '../../services/requirementService'
import type { TreeProps } from 'antd'

const { Title } = Typography
const { Search } = Input

// TreeView组件属性
interface TreeViewProps {
  /** 是否支持多选 */
  multiple?: boolean
  /** 是否支持搜索 */
  searchable?: boolean
  /** 是否显示过滤 */
  showFilter?: boolean
  /** 是否显示详情 */
  showDetails?: boolean
  /** 是否启用虚拟滚动 */
  virtual?: boolean
  /** 选中的节点keys */
  selectedKeys?: string[]
  /** 选中节点回调 */
  onSelect?: (selectedKeys: string[], info: any) => void
  /** 自定义样式类名 */
  className?: string
  /** 自定义高度（虚拟滚动时使用） */
  height?: number
}

// 需求节点数据接口
interface RequirementNode {
  elementId: string
  eClass: string
  declaredName: string
  declaredShortName?: string
  documentation?: string
  status?: string
  priority?: string
  verificationMethod?: string
  reqId?: string
  requirementDefinition?: string // 标准化字段
}

// Ant Design Tree节点数据格式
interface AntTreeNode {
  key: string
  title: React.ReactNode
  icon?: React.ReactNode
  children?: AntTreeNode[]
  isLeaf?: boolean
  selectable?: boolean
}

/**
 * TreeView组件 - 需求层次结构展示
 */
const TreeView: React.FC<TreeViewProps> = ({
  multiple = false,
  searchable = true,
  showFilter = false,
  showDetails = false,
  virtual = false,
  selectedKeys = [],
  onSelect,
  className = '',
  height = 400
}) => {
  const { selectElement, selectedIds } = useModelContext()
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [definitions, setDefinitions] = useState<RequirementNode[]>([])
  const [usages, setUsages] = useState<RequirementNode[]>([])
  const [searchValue, setSearchValue] = useState('')
  const [statusFilter, setStatusFilter] = useState<string>('all')
  const [expandedKeys, setExpandedKeys] = useState<string[]>(['definitions', 'usages'])
  const [autoExpandParent, setAutoExpandParent] = useState(true)
  const [selectedNode, setSelectedNode] = useState<RequirementNode | null>(null)

  // 加载数据
  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    setLoading(true)
    setError(null)
    
    try {
      // 并行加载Definition和Usage数据
      const [defResponse, usageResponse] = await Promise.all([
        requirementService.getRequirementDefinitions(0, 100),
        requirementService.getRequirementUsages(0, 100)
      ])

      // 设置数据 - 确保使用标准化字段
      setDefinitions(defResponse.content || [])
      setUsages(usageResponse.content || [])
    } catch (err) {
      console.error('加载数据失败:', err)
      setError('加载失败：' + (err as Error).message)
    } finally {
      setLoading(false)
    }
  }

  // 创建Definition ID到名称的映射
  const definitionMap = useMemo(() => {
    const map = new Map<string, string>()
    definitions.forEach(def => {
      map.set(def.elementId, def.declaredName || def.reqId || def.elementId)
    })
    return map
  }, [definitions])

  // 获取状态徽章样式
  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'approved': return 'success'
      case 'implemented': return 'processing'
      case 'verified': return 'success'
      case 'draft': return 'default'
      case 'deprecated': return 'error'
      default: return 'default'
    }
  }

  // 过滤数据
  const filteredData = useMemo(() => {
    let filteredDefs = [...definitions]
    let filteredUsages = [...usages]

    // 搜索过滤
    if (searchValue) {
      const searchLower = searchValue.toLowerCase()
      filteredDefs = filteredDefs.filter(d => 
        d.declaredName?.toLowerCase().includes(searchLower) ||
        d.reqId?.toLowerCase().includes(searchLower) ||
        d.documentation?.toLowerCase().includes(searchLower)
      )
      filteredUsages = filteredUsages.filter(u => 
        u.declaredName?.toLowerCase().includes(searchLower) ||
        u.documentation?.toLowerCase().includes(searchLower)
      )
    }

    // 状态过滤
    if (statusFilter !== 'all') {
      filteredDefs = filteredDefs.filter(d => d.status === statusFilter)
      filteredUsages = filteredUsages.filter(u => u.status === statusFilter)
    }

    return { definitions: filteredDefs, usages: filteredUsages }
  }, [definitions, usages, searchValue, statusFilter])

  // 构建树形数据
  const treeNodes = useMemo(() => {
    const { definitions: filteredDefs, usages: filteredUsages } = filteredData

    // 构建Definition节点
    const defNodes: AntTreeNode[] = filteredDefs.map(def => ({
      key: def.elementId,
      title: (
        <span className="tree-node-title">
          <FileTextOutlined style={{ marginRight: 4 }} />
          <span className="node-name">{def.declaredName || def.reqId}</span>
          {def.status && (
            <Badge 
              status={getStatusBadge(def.status)} 
              text={def.status} 
              style={{ marginLeft: 8 }}
            />
          )}
          {def.priority && (
            <span className="priority-tag" style={{ marginLeft: 4 }}>
              {def.priority}
            </span>
          )}
        </span>
      ),
      isLeaf: true,
      className: 'requirement-definition-node',
      data: def
    }))

    // 构建Usage节点 - 显示关联的Definition
    const usageNodes: AntTreeNode[] = filteredUsages.map(usage => {
      const definitionName = usage.requirementDefinition 
        ? definitionMap.get(usage.requirementDefinition)
        : null

      const children: AntTreeNode[] = []
      if (usage.requirementDefinition && definitionName) {
        children.push({
          key: `${usage.elementId}-def-ref`,
          title: (
            <span style={{ color: '#8c8c8c', fontSize: '12px' }}>
              <LinkOutlined /> 基于: {definitionName}
            </span>
          ),
          isLeaf: true,
          selectable: false
        })
      }

      return {
        key: usage.elementId,
        title: (
          <span className="tree-node-title">
            <FileTextOutlined style={{ marginRight: 4 }} />
            <span className="node-name">{usage.declaredName}</span>
            {definitionName && (
              <Tooltip title={`基于: ${definitionName}`}>
                <span style={{ color: '#1890ff', marginLeft: 8 }}>
                  → {definitionName}
                </span>
              </Tooltip>
            )}
            {!usage.requirementDefinition && (
              <span style={{ color: '#ff4d4f', marginLeft: 8 }}>
                未关联
              </span>
            )}
            {usage.status && (
              <Badge 
                status={getStatusBadge(usage.status)} 
                text={usage.status} 
                style={{ marginLeft: 8 }}
              />
            )}
          </span>
        ),
        children: children.length > 0 ? children : undefined,
        className: 'requirement-usage-node',
        data: usage
      }
    })

    // 返回分组的树形结构
    return [
      {
        key: 'definitions',
        title: (
          <span style={{ fontWeight: 'bold' }}>
            <FolderOutlined style={{ marginRight: 4 }} />
            需求定义 ({defNodes.length})
          </span>
        ),
        children: defNodes,
        className: 'tree-group'
      },
      {
        key: 'usages',
        title: (
          <span style={{ fontWeight: 'bold' }}>
            <FolderOutlined style={{ marginRight: 4 }} />
            需求使用 ({usageNodes.length})
          </span>
        ),
        children: usageNodes,
        className: 'tree-group'
      }
    ]
  }, [filteredData, definitionMap, getStatusBadge])

  // 获取所有节点的key（用于搜索时展开）
  const getAllKeys = useCallback((nodes: AntTreeNode[]): string[] => {
    let keys: string[] = []
    nodes.forEach(node => {
      keys.push(node.key)
      if (node.children) {
        keys = keys.concat(getAllKeys(node.children))
      }
    })
    return keys
  }, [])

  // 处理搜索
  const handleSearch = useCallback((value: string) => {
    setSearchValue(value)
    if (value) {
      // 搜索时展开所有节点
      const allKeys = getAllKeys(treeNodes)
      setExpandedKeys(allKeys)
      setAutoExpandParent(true)
    } else {
      setAutoExpandParent(false)
    }
  }, [treeNodes, getAllKeys])

  // 处理节点展开
  const handleExpand = useCallback((expandedKeysValue: React.Key[]) => {
    setExpandedKeys(expandedKeysValue as string[])
    setAutoExpandParent(false)
  }, [])

  // 处理节点选中
  const handleSelect = useCallback((selectedKeysValue: React.Key[], info: any) => {
    const keys = selectedKeysValue as string[]
    
    // 获取节点数据
    const nodeData = info.node.data as RequirementNode | undefined
    if (nodeData) {
      setSelectedNode(nodeData)
    }
    
    // 调用ModelContext的选择逻辑
    if (keys.length > 0) {
      const lastSelectedKey = keys[keys.length - 1]
      selectElement(lastSelectedKey, multiple)
    }
    
    // 调用外部回调
    if (onSelect) {
      onSelect(keys, info)
    }
  }, [selectElement, multiple, onSelect])

  // 同步ModelContext的选中状态
  const syncSelectedKeys = useMemo(() => {
    return Array.from(selectedIds)
  }, [selectedIds])

  // Tree组件的props
  const treeProps: TreeProps = {
    treeData: treeNodes,
    selectedKeys: selectedKeys.length > 0 ? selectedKeys : syncSelectedKeys,
    expandedKeys,
    autoExpandParent,
    multiple,
    onExpand: handleExpand,
    onSelect: handleSelect,
    showIcon: true,
    showLine: false,
    blockNode: true,
    'aria-label': '需求树视图'
  }

  // 虚拟滚动配置
  if (virtual) {
    treeProps.virtual = true
    treeProps.height = height
    treeProps['data-virtual'] = 'true'
  }

  return (
    <div className={`tree-view-container ${className}`} style={{ padding: '16px' }}>
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
        {/* 标题 */}
        <Title level={4} style={{ margin: 0 }}>
          需求层次结构
        </Title>

        {/* 搜索框 */}
        {searchable && (
          <Search
            placeholder="搜索需求..."
            allowClear
            prefix={<SearchOutlined />}
            onChange={(e) => handleSearch(e.target.value)}
            style={{ marginBottom: 8 }}
            size="middle"
          />
        )}

        {/* 加载状态 */}
        {loading && (
          <div style={{ textAlign: 'center', padding: '20px' }}>
            <Spin tip="加载中..." />
          </div>
        )}

        {/* 错误状态 */}
        {error && (
          <Alert message="加载失败" description={error} type="error" showIcon />
        )}

        {/* 树组件 */}
        {!loading && !error && (
          <Tree
            {...treeProps}
            className="requirement-tree"
            style={{
              background: '#fff',
              border: '1px solid #d9d9d9',
              borderRadius: '6px',
              padding: '8px'
            }}
          />
        )}

        {/* 空状态 */}
        {!loading && !error && treeNodes.length === 0 && (
          <div style={{ textAlign: 'center', padding: '40px', color: '#999' }}>
            {searchValue ? '未找到匹配的需求' : '暂无数据'}
          </div>
        )}
        
        {/* 节点详情 */}
        {showDetails && selectedNode && (
          <div 
            data-testid="node-details"
            style={{ 
              padding: '12px 16px', 
              borderTop: '1px solid #f0f0f0',
              background: '#fafafa'
            }}
          >
            <h4>{selectedNode.declaredName}</h4>
            <div style={{ fontSize: '12px', color: '#666' }}>
              {selectedNode.status && <div>状态: {selectedNode.status}</div>}
              {selectedNode.priority && <div>优先级: {selectedNode.priority}</div>}
              {selectedNode.verificationMethod && <div>验证方法: {selectedNode.verificationMethod}</div>}
              {selectedNode.documentation && (
                <div style={{ marginTop: 8 }}>
                  文档: {selectedNode.documentation}
                </div>
              )}
            </div>
          </div>
        )}
      </Space>
    </div>
  )
}

export default TreeView