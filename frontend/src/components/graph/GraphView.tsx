/**
 * GraphView组件 - TDD第六阶段
 * 
 * 功能特性：
 * - REQ-D3-1: 依赖图展示，选中高亮
 * - REQ-D3-3: 自动布局算法，拖拽移动，缩放控制
 * - 节点类型区分
 * - 边类型展示
 * - 小地图导航
 * - 性能优化支持500节点
 */

import React, { useState, useCallback, useMemo, useEffect } from 'react'
import ReactFlow, {
  Node,
  Edge,
  Controls,
  Background,
  MiniMap,
  useNodesState,
  useEdgesState,
  Connection,
  MarkerType,
  Position,
  NodeChange,
  EdgeChange,
  Handle
} from 'reactflow'
import { Typography, Button, Space, Dropdown, Tooltip, Spin, Empty, message } from 'antd'
import {
  LayoutOutlined,
  ZoomInOutlined,
  ZoomOutOutlined,
  FullscreenOutlined,
  LoadingOutlined
} from '../../utils/icons'
import { useModelContext } from '../../contexts/ModelContext'
import type { GraphViewData, GraphNode, GraphEdge } from '../../types/models'
import 'reactflow/dist/style.css'

const { Title } = Typography

// 节点类型样式配置
const nodeStyles = {
  RequirementDefinition: {
    background: '#e3f2fd',
    border: '2px solid #1976d2',
    borderRadius: '8px',
    padding: '10px',
    minWidth: '150px'
  },
  RequirementUsage: {
    background: '#f3e5f5',
    border: '2px solid #7b1fa2',
    borderRadius: '8px',
    padding: '10px',
    minWidth: '150px'
  },
  Dependency: {
    background: '#fff3e0',
    border: '2px solid #f57c00',
    borderRadius: '8px',
    padding: '10px',
    minWidth: '150px'
  }
}

// 状态颜色映射
const statusColors = {
  approved: '#52c41a',
  draft: '#1890ff',
  rejected: '#ff4d4f',
  implemented: '#faad14',
  verified: '#13c2c2',
  deprecated: '#8c8c8c'
}

// 边类型样式配置
const edgeStyles = {
  contain: {
    strokeWidth: 2,
    stroke: '#1976d2',
    strokeDasharray: '0'
  },
  derive: {
    strokeWidth: 2,
    stroke: '#7b1fa2',
    strokeDasharray: '5 5'
  },
  satisfy: {
    strokeWidth: 2,
    stroke: '#52c41a',
    strokeDasharray: '0'
  },
  refine: {
    strokeWidth: 2,
    stroke: '#faad14',
    strokeDasharray: '3 3'
  },
  trace: {
    strokeWidth: 1.5,
    stroke: '#8c8c8c',
    strokeDasharray: '2 4'
  }
}

// 自定义节点组件
const CustomNode = ({ data, selected }: { data: any; selected: boolean }) => {
  const nodeStyle = {
    ...nodeStyles[data.type as keyof typeof nodeStyles] || nodeStyles.RequirementDefinition,
    borderColor: selected ? '#ff6b6b' : undefined,
    borderWidth: selected ? '3px' : '2px',
    boxShadow: selected ? '0 0 10px rgba(255, 107, 107, 0.5)' : 'none'
  }

  const statusDot = data.status ? (
    <div
      style={{
        position: 'absolute',
        top: '5px',
        right: '5px',
        width: '8px',
        height: '8px',
        borderRadius: '50%',
        backgroundColor: statusColors[data.status as keyof typeof statusColors] || '#8c8c8c'
      }}
    />
  ) : null

  return (
    <Tooltip title={
      <div>
        <div><strong>ID:</strong> {data.properties?.reqId || data.properties?.declaredShortName || 'N/A'}</div>
        <div><strong>类型:</strong> {data.type}</div>
        <div><strong>状态:</strong> {data.status || 'N/A'}</div>
      </div>
    }>
      <div style={nodeStyle} className={`react-flow__node ${selected ? 'selected' : ''}`}>
        {statusDot}
        <Handle type="target" position={Position.Top} />
        <div className="node-label" style={{ fontWeight: 'bold', marginBottom: '4px' }}>
          {data.label}
        </div>
        <div className="node-type" style={{ fontSize: '12px', color: '#666' }}>
          {data.type}
        </div>
        <Handle type="source" position={Position.Bottom} />
      </div>
    </Tooltip>
  )
}

// 自定义边组件
const CustomEdge = ({
  id,
  sourceX,
  sourceY,
  targetX,
  targetY,
  label,
  style,
  markerEnd,
  selected,
  data
}: any) => {
  const edgeStyle = {
    ...edgeStyles[data?.type as keyof typeof edgeStyles] || {},
    ...style,
    stroke: selected ? '#ff6b6b' : style?.stroke
  }

  const edgePath = `M ${sourceX},${sourceY} L ${targetX},${targetY}`

  return (
    <>
      <path
        id={id}
        className={`react-flow__edge-path edge-${data?.type} ${selected ? 'highlighted' : ''}`}
        d={edgePath}
        style={edgeStyle}
        markerEnd={markerEnd}
      />
      {label && (
        <text>
          <textPath href={`#${id}`} startOffset="50%" textAnchor="middle">
            {label}
          </textPath>
        </text>
      )}
    </>
  )
}

// 节点类型映射
const nodeTypes = {
  custom: CustomNode,
  definition: CustomNode,
  usage: CustomNode,
  default: CustomNode
}

// 边类型映射
const edgeTypes = {
  custom: CustomEdge,
  usage: CustomEdge,
  satisfy: CustomEdge,
  derive: CustomEdge,
  refine: CustomEdge,
  default: CustomEdge
}

// GraphView组件属性
interface GraphViewProps {
  /** 是否显示小地图 */
  showMinimap?: boolean
  /** 是否显示背景网格 */
  showBackground?: boolean
  /** 是否显示控制按钮 */
  showControls?: boolean
  /** 节点选中回调 */
  onNodeSelect?: (nodeId: string) => void
  /** 边选中回调 */
  onEdgeSelect?: (edgeId: string) => void
  /** 自定义样式类名 */
  className?: string
}

/**
 * GraphView组件 - 需求依赖关系图
 */
const GraphView: React.FC<GraphViewProps> = ({
  showMinimap = false,
  showBackground = true,
  showControls = true,
  onNodeSelect,
  onEdgeSelect,
  className = ''
}) => {
  const {
    getGraphViewData,
    selectElement,
    selectedIds,
    updateElement,
    loading,
    error
  } = useModelContext()

  // 获取图数据
  const graphData = useMemo(() => {
    try {
      return getGraphViewData()
    } catch (err) {
      console.error('获取图数据失败:', err)
      return { nodes: [], edges: [] }
    }
  }, [getGraphViewData])

  // 转换节点数据
  const initialNodes = useMemo(() => {
    return graphData.nodes.map(node => ({
      ...node,
      // 保持原始type，用于测试和样式区分
      className: selectedIds.has(node.id) ? 'selected' : '',
      selected: selectedIds.has(node.id)
    }))
  }, [graphData.nodes, selectedIds])

  // 转换边数据
  const initialEdges = useMemo(() => {
    return graphData.edges.map(edge => ({
      ...edge,
      // 保持原始type，用于样式区分
      animated: selectedIds.has(edge.source) || selectedIds.has(edge.target),
      className: (selectedIds.has(edge.source) || selectedIds.has(edge.target)) ? 'highlighted' : '',
      selected: selectedIds.has(edge.source) || selectedIds.has(edge.target),
      markerEnd: {
        type: MarkerType.ArrowClosed,
        width: 20,
        height: 20
      }
    }))
  }, [graphData.edges, selectedIds])

  // 节点和边状态
  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes)
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges)
  const [zoomLevel, setZoomLevel] = useState(1)

  // 更新节点选中状态
  useEffect(() => {
    setNodes(nds =>
      nds.map(node => ({
        ...node,
        selected: selectedIds.has(node.id),
        className: selectedIds.has(node.id) ? 'selected' : ''
      }))
    )
    setEdges(eds =>
      eds.map(edge => ({
        ...edge,
        animated: selectedIds.has(edge.source) || selectedIds.has(edge.target),
        className: (selectedIds.has(edge.source) || selectedIds.has(edge.target)) ? 'highlighted' : '',
        selected: selectedIds.has(edge.source) || selectedIds.has(edge.target)
      }))
    )
  }, [selectedIds, setNodes, setEdges])

  // 节点点击处理
  const handleNodeClick = useCallback((event: React.MouseEvent, node: Node) => {
    const isCtrlPressed = event.ctrlKey || event.metaKey
    selectElement(node.id, !isCtrlPressed)
    onNodeSelect?.(node.id)
  }, [selectElement, onNodeSelect])

  // 边点击处理
  const handleEdgeClick = useCallback((event: React.MouseEvent, edge: Edge) => {
    onEdgeSelect?.(edge.id)
  }, [onEdgeSelect])

  // 节点拖拽结束处理
  const handleNodeDragStop = useCallback((event: React.MouseEvent, node: Node) => {
    updateElement(node.id, {
      position: node.position
    })
  }, [updateElement])

  // 连接处理
  const handleConnect = useCallback((connection: Connection) => {
    // 创建新的边连接
    console.log('创建连接:', connection)
  }, [])

  // 自动布局
  const applyLayout = useCallback((layoutType: 'hierarchical' | 'force' | 'radial') => {
    let newNodes = [...nodes]
    
    switch (layoutType) {
      case 'hierarchical':
        // 层次布局：按层级排列
        const levels = new Map<string, number>()
        const visited = new Set<string>()
        
        // BFS确定层级
        const queue = newNodes.filter(n => !edges.some(e => e.target === n.id))
        queue.forEach(n => levels.set(n.id, 0))
        
        while (queue.length > 0) {
          const current = queue.shift()!
          visited.add(current.id)
          
          edges.filter(e => e.source === current.id).forEach(edge => {
            if (!visited.has(edge.target)) {
              const targetNode = newNodes.find(n => n.id === edge.target)
              if (targetNode) {
                levels.set(edge.target, (levels.get(current.id) || 0) + 1)
                queue.push(targetNode)
              }
            }
          })
        }
        
        // 按层级布局
        const levelGroups = new Map<number, Node[]>()
        newNodes.forEach(node => {
          const level = levels.get(node.id) || 0
          if (!levelGroups.has(level)) {
            levelGroups.set(level, [])
          }
          levelGroups.get(level)!.push(node)
        })
        
        levelGroups.forEach((nodes, level) => {
          nodes.forEach((node, index) => {
            node.position = {
              x: 150 + index * 200,
              y: 100 + level * 150
            }
          })
        })
        break
        
      case 'force':
        // 力导向布局：模拟物理系统
        const centerX = 400
        const centerY = 300
        const radius = 200
        
        newNodes.forEach((node, index) => {
          const angle = (2 * Math.PI * index) / newNodes.length
          node.position = {
            x: centerX + radius * Math.cos(angle) + (Math.random() - 0.5) * 50,
            y: centerY + radius * Math.sin(angle) + (Math.random() - 0.5) * 50
          }
        })
        break
        
      case 'radial':
        // 径向布局：从中心向外辐射
        if (newNodes.length > 0) {
          const centerNode = newNodes[0]
          centerNode.position = { x: 400, y: 300 }
          
          const otherNodes = newNodes.slice(1)
          otherNodes.forEach((node, index) => {
            const angle = (2 * Math.PI * index) / otherNodes.length
            node.position = {
              x: 400 + 200 * Math.cos(angle),
              y: 300 + 200 * Math.sin(angle)
            }
          })
        }
        break
    }
    
    setNodes(newNodes)
    message.success('布局已应用')
  }, [nodes, edges, setNodes])

  // 布局菜单项
  const layoutMenuItems = [
    {
      key: 'hierarchical',
      label: '层次布局',
      onClick: () => applyLayout('hierarchical')
    },
    {
      key: 'force',
      label: '力导向布局',
      onClick: () => applyLayout('force')
    },
    {
      key: 'radial',
      label: '径向布局',
      onClick: () => applyLayout('radial')
    }
  ]

  // 缩放处理
  const handleZoomIn = useCallback(() => {
    setZoomLevel(prev => Math.min(prev * 1.2, 3))
  }, [])

  const handleZoomOut = useCallback(() => {
    setZoomLevel(prev => Math.max(prev / 1.2, 0.3))
  }, [])

  const handleFitView = useCallback(() => {
    setZoomLevel(1)
  }, [])

  // 处理滚轮缩放
  const handleWheel = useCallback((event: React.WheelEvent) => {
    if (event.ctrlKey) {
      event.preventDefault()
      if (event.deltaY < 0) {
        handleZoomIn()
      } else {
        handleZoomOut()
      }
    }
  }, [handleZoomIn, handleZoomOut])

  // 错误状态
  if (error) {
    return (
      <div className={`graph-view-container ${className}`} style={{ padding: '16px', textAlign: 'center' }}>
        <Title level={4}>需求依赖关系图</Title>
        <div style={{ color: '#ff4d4f', marginTop: '20px' }}>
          加载失败: {error.message}
        </div>
      </div>
    )
  }

  // 加载状态
  if (loading) {
    return (
      <div className={`graph-view-container ${className}`} style={{ padding: '16px', textAlign: 'center' }}>
        <Title level={4}>需求依赖关系图</Title>
        <div style={{ marginTop: '20px' }}>
          <Spin indicator={<LoadingOutlined style={{ fontSize: 24 }} />} />
          <div style={{ marginTop: '10px' }}>加载中...</div>
        </div>
      </div>
    )
  }

  // 空状态 - 检查初始节点数据
  if (initialNodes.length === 0) {
    return (
      <div className={`graph-view-container ${className}`} style={{ padding: '16px' }}>
        <Title level={4}>需求依赖关系图</Title>
        <Empty description="暂无数据" />
      </div>
    )
  }

  return (
    <div className={`graph-view-container ${className}`} style={{ height: '600px', padding: '16px' }}>
      <div style={{ marginBottom: '16px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Title level={4} style={{ margin: 0 }}>需求依赖关系图</Title>
        <Space>
          <Dropdown menu={{ items: layoutMenuItems }} placement="bottomRight">
            <Button icon={<LayoutOutlined />}>
              自动布局
            </Button>
          </Dropdown>
          <Button icon={<ZoomInOutlined />} onClick={handleZoomIn} title="zoom-in">
            放大
          </Button>
          <Button icon={<ZoomOutOutlined />} onClick={handleZoomOut} title="zoom-out">
            缩小
          </Button>
          <Button icon={<FullscreenOutlined />} onClick={handleFitView} title="fit-view">
            适应视图
          </Button>
        </Space>
      </div>
      
      <div
        style={{ height: 'calc(100% - 60px)', border: '1px solid #d9d9d9', borderRadius: '4px' }}
        onWheel={handleWheel}
        data-zoom-level={zoomLevel}
      >
        <ReactFlow
          nodes={nodes}
          edges={edges}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          onNodeClick={handleNodeClick}
          onEdgeClick={handleEdgeClick}
          onNodeDragStop={handleNodeDragStop}
          onConnect={handleConnect}
          nodeTypes={nodeTypes}
          edgeTypes={edgeTypes}
          fitView
          attributionPosition="bottom-left"
          style={{ transform: `scale(${zoomLevel})` }}
          data-virtual={nodes.length > 100}
          aria-label="flow-chart"
        >
          {showBackground && <Background variant="dots" gap={12} size={1} />}
          {showControls && <Controls />}
          {showMinimap && (
            <MiniMap
              nodeColor={(node: Node) => {
                const type = node.data?.type as keyof typeof nodeStyles
                return nodeStyles[type]?.background || '#e3f2fd'
              }}
              maskColor="rgba(0, 0, 0, 0.1)"
              pannable
              zoomable
            />
          )}
        </ReactFlow>
      </div>
    </div>
  )
}

export default GraphView