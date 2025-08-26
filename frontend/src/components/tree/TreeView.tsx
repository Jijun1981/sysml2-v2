/**
 * TreeView组件 - TDD第六阶段
 * 
 * 功能特性：
 * - REQ-D1-1: 层级展示，包含/引用关系
 * - 展开/折叠交互
 * - 选中状态同步
 * - 搜索过滤
 * - 虚拟滚动优化
 * - 键盘导航支持
 */

import React, { useState, useCallback, useMemo, useEffect } from 'react'
import { Tree, Input, Typography, Space } from 'antd'
import { 
  FolderOutlined, 
  FileTextOutlined, 
  SearchOutlined,
  FolderOpenOutlined 
} from '../../utils/icons'
import { useModelContext } from '../../contexts/ModelContext'
import type { TreeViewData, TreeNodeData } from '../../types/models'
import type { TreeProps } from 'antd'

const { Title } = Typography
const { Search } = Input

// TreeView组件属性
interface TreeViewProps {
  /** 是否支持多选 */
  multiple?: boolean
  /** 是否支持搜索 */
  searchable?: boolean
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
  virtual = false,
  selectedKeys = [],
  onSelect,
  className = '',
  height = 400
}) => {
  const { getTreeViewData, selectElement, selectedIds, loading, error } = useModelContext()
  const [searchValue, setSearchValue] = useState('')
  const [expandedKeys, setExpandedKeys] = useState<string[]>([])
  const [autoExpandParent, setAutoExpandParent] = useState(true)

  // 获取树视图数据
  const treeViewData = useMemo(() => {
    try {
      return getTreeViewData()
    } catch (err) {
      console.error('获取树视图数据失败:', err)
      return { definitions: [] } as TreeViewData
    }
  }, [getTreeViewData])

  // 递归构建Ant Design树节点
  const buildTreeNodes = useCallback((nodes: TreeNodeData[], parentKey = '', searchValue = ''): AntTreeNode[] => {
    return nodes.map(node => {
      const nodeKey = node.id
      const isMatch = searchValue ? node.label.toLowerCase().includes(searchValue.toLowerCase()) : true
      
      // 高亮搜索文本
      const title = searchValue && isMatch ? (
        <span>
          {node.label.split(new RegExp(`(${searchValue})`, 'gi')).map((part, index) => 
            part.toLowerCase() === searchValue.toLowerCase() ? (
              <mark key={index} style={{ backgroundColor: '#ffc069', padding: 0 }}>
                {part}
              </mark>
            ) : part
          )}
        </span>
      ) : node.label

      // 构建子节点
      const children: AntTreeNode[] = []
      
      // 添加子Definition节点
      if (node.children && node.children.length > 0) {
        children.push(...buildTreeNodes(node.children, nodeKey, searchValue))
      }
      
      // 添加Usage节点
      if (node.usages && node.usages.length > 0) {
        const usageNodes = node.usages.map(usage => ({
          key: usage.id,
          title: searchValue && usage.label.toLowerCase().includes(searchValue.toLowerCase()) ? (
            <span>
              {usage.label.split(new RegExp(`(${searchValue})`, 'gi')).map((part, index) => 
                part.toLowerCase() === searchValue.toLowerCase() ? (
                  <mark key={index} style={{ backgroundColor: '#ffc069', padding: 0 }}>
                    {part}
                  </mark>
                ) : part
              )}
            </span>
          ) : usage.label,
          icon: <FileTextOutlined style={{ color: '#1890ff' }} />,
          isLeaf: true,
          selectable: true
        }))
        children.push(...usageNodes)
      }

      // 过滤搜索结果
      if (searchValue) {
        const hasMatchingChildren = children.some(child => 
          child.title && child.title.toString().toLowerCase().includes(searchValue.toLowerCase())
        )
        if (!isMatch && !hasMatchingChildren) {
          return null
        }
      }

      return {
        key: nodeKey,
        title,
        icon: node.type === 'definition' ? 
          <FolderOutlined style={{ color: '#faad14' }} /> : 
          <FileTextOutlined style={{ color: '#1890ff' }} />,
        children: children.length > 0 ? children : undefined,
        isLeaf: children.length === 0,
        selectable: true
      }
    }).filter((node): node is AntTreeNode => node !== null)
  }, [])

  // 构建树节点数据
  const treeNodes = useMemo(() => {
    return buildTreeNodes(treeViewData.definitions, '', searchValue)
  }, [treeViewData, buildTreeNodes, searchValue])

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
          <div style={{ textAlign: 'center', padding: '20px', color: '#999' }}>
            加载中...
          </div>
        )}

        {/* 错误状态 */}
        {error && (
          <div style={{ textAlign: 'center', padding: '20px', color: '#ff4d4f' }}>
            加载失败: {error.message}
          </div>
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
      </Space>
    </div>
  )
}

export default TreeView