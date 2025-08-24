import React, { useMemo, useCallback, useEffect } from 'react'
import ReactFlow, {
  Controls,
  MiniMap,
  Background,
  BackgroundVariant,
  Node,
  Edge,
  MarkerType,
  useNodesState,
  useEdgesState,
  Connection,
  addEdge
} from 'reactflow'
import { Card } from 'antd'
import { useModelContext } from '../../../contexts/ModelContext'
import 'reactflow/dist/style.css'
import './GraphView.css'

/**
 * 图视图组件 - 基于通用接口和SSOT实现
 * REQ-D3-1: 图视图接口 - 返回nodes/edges
 * REQ-D3-2: 连线/删线 - 调用Trace创建/删除
 * REQ-A1-1: 数据源唯一 - 从SSOT获取数据
 */
const GraphView: React.FC = () => {
  const { 
    selectedIds, 
    selectElement,
    createElement,
    deleteElement,
    getGraphViewData,
    loadAllElements,
    loading
  } = useModelContext()

  // 初始加载数据
  useEffect(() => {
    const loadData = async () => {
      try {
        await loadAllElements()
      } catch (error) {
        console.error('GraphView: 加载数据失败', error)
      }
    }
    
    loadData()
  }, [loadAllElements])

  // 构建节点和边数据 - 使用ModelContext的视图投影方法
  const { nodes: graphNodes, edges: graphEdges } = getGraphViewData()

  // 构建ReactFlow节点数据
  const initialNodes = useMemo(() => {
    return graphNodes.map((node, index) => {
      const isSelected = selectedIds.has(node.id)
      const isRelated = Array.from(selectedIds).some(selectedId => 
        graphEdges.some(edge => 
          (edge.source === selectedId && edge.target === node.id) ||
          (edge.target === selectedId && edge.source === node.id)
        )
      )
      
      // 根据节点类型设置不同的样式
      let nodeStyle = {
        background: '#fff',
        border: '1px solid #d9d9d9',
        borderRadius: '8px',
        padding: '10px',
        fontSize: '12px'
      }

      if (isSelected) {
        nodeStyle = {
          ...nodeStyle,
          background: '#bae7ff',
          border: '2px solid #1890ff'
        }
      } else if (isRelated) {
        nodeStyle = {
          ...nodeStyle,
          background: '#f6ffed',
          border: '1px solid #52c41a'
        }
      }

      // 根据类型设置颜色
      switch (node.type) {
        case 'requirementdefinition':
          nodeStyle.background = isSelected ? '#bae7ff' : '#e6f7ff'
          break
        case 'requirementusage':
          nodeStyle.background = isSelected ? '#f6ffed' : '#f6ffed'
          break
        case 'partusage':
          nodeStyle.background = isSelected ? '#fff7e6' : '#fff7e6'
          break
        default:
          break
      }
      
      return {
        id: node.id,
        type: 'default',
        position: { 
          x: node.x || 100 + (index % 4) * 250, 
          y: node.y || 100 + Math.floor(index / 4) * 150 
        },
        data: { 
          label: (
            <div data-testid={`graph-node-label-${node.id}`}>
              <div style={{ fontWeight: 'bold', marginBottom: '4px' }}>
                {node.type}
              </div>
              <div>{node.label}</div>
            </div>
          )
        },
        style: nodeStyle,
        className: `graph-node ${isSelected ? 'selected' : ''} ${isRelated ? 'related' : ''}`,
      }
    })
  }, [graphNodes, selectedIds, graphEdges])

  // 构建ReactFlow边数据
  const initialEdges = useMemo(() => {
    return graphEdges.map(edge => {
      const isHighlighted = Array.from(selectedIds).some(selectedId => 
        edge.source === selectedId || edge.target === selectedId
      )
      
      const edgeColor = getEdgeColor(edge.type)
      
      return {
        id: edge.id,
        source: edge.source,
        target: edge.target,
        type: 'default',
        animated: edge.type === 'derive',
        label: edge.label || edge.type,
        style: {
          stroke: edgeColor,
          strokeWidth: isHighlighted ? 2 : 1,
          strokeDasharray: edge.type === 'of' ? '5,5' : 'none'
        },
        markerEnd: {
          type: MarkerType.ArrowClosed,
          color: edgeColor
        },
        className: `graph-edge ${isHighlighted ? 'highlighted' : ''}`,
        data: {
          testId: `graph-edge-${edge.id}`
        }
      }
    })
  }, [graphEdges, selectedIds])

  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes)
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges)

  // 更新节点和边
  React.useEffect(() => {
    setNodes(initialNodes)
  }, [initialNodes, setNodes])

  React.useEffect(() => {
    setEdges(initialEdges)
  }, [initialEdges, setEdges])

  // 处理节点点击 - 支持多选
  const onNodeClick = useCallback((event: React.MouseEvent, node: Node) => {
    const multiSelect = event.ctrlKey || event.metaKey
    selectElement(node.id, multiSelect)
  }, [selectElement])

  // 处理连线 - 创建Satisfy关系
  const onConnect = useCallback(
    async (params: Connection) => {
      if (params.source && params.target) {
        try {
          // 默认创建Satisfy关系，实际应用中可以让用户选择关系类型
          await createElement('Satisfy', {
            source: params.source,
            target: params.target
          })
        } catch (error) {
          console.error('创建关系失败:', error)
        }
      }
    },
    [createElement]
  )

  // 处理边删除
  const onEdgeClick = useCallback(
    async (event: React.MouseEvent, edge: Edge) => {
      // 只删除依赖关系，不删除of关系
      if (!edge.id.startsWith('of-')) {
        if (window.confirm(`确定删除关系 ${edge.label}?`)) {
          try {
            await deleteElement(edge.id)
          } catch (error) {
            console.error('删除关系失败:', error)
          }
        }
      }
    },
    [deleteElement]
  )

  return (
    <Card 
      className="graph-view-card" 
      style={{ height: '100%' }}
      data-testid="graph-view"
      title="依赖关系图"
      loading={loading}
    >
      <ReactFlow
        nodes={nodes.map(node => ({
          ...node,
          data: {
            ...node.data,
            testId: `graph-node-${node.id}`
          }
        }))}
        edges={edges.map(edge => ({
          ...edge,
          data: {
            ...edge.data,
            testId: `graph-edge-${edge.id}`
          }
        }))}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onConnect={onConnect}
        onNodeClick={onNodeClick}
        onEdgeClick={onEdgeClick}
        fitView
        connectOnClick={false}
        nodesDraggable={true}
        nodesConnectable={true}
        elementsSelectable={true}
      >
        <Controls />
        <MiniMap 
          nodeColor={(node) => {
            if (selectedIds.has(node.id)) return '#1890ff'
            switch (node.data?.type) {
              case 'requirementdefinition': return '#e6f7ff'
              case 'requirementusage': return '#f6ffed'
              case 'partusage': return '#fff7e6'
              default: return '#f0f0f0'
            }
          }}
        />
        <Background variant={BackgroundVariant.Dots} gap={12} size={1} />
      </ReactFlow>
    </Card>
  )
}

// 根据关系类型获取边的颜色
function getEdgeColor(type: string): string {
  switch (type) {
    case 'derive':
      return '#1890ff' // 蓝色
    case 'satisfy':
      return '#52c41a' // 绿色
    case 'refine':
      return '#fa8c16' // 橙色
    case 'of':
      return '#999999' // 灰色，虚线
    case 'trace':
    default:
      return '#8c8c8c' // 灰色
  }
}

export default GraphView