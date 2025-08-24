import React, { useMemo, useCallback } from 'react'
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
import { useModel } from '../../../contexts/ModelContext'
import 'reactflow/dist/style.css'
import './GraphView.css'

/**
 * 图视图组件
 * REQ-D3-1: 图视图接口 - 返回nodes/edges
 * REQ-D3-2: 连线/删线 - 调用Trace创建/删除
 */
const GraphView: React.FC = () => {
  const { 
    requirements, 
    usages, 
    traces, 
    selectedId, 
    selectElement,
    createTrace,
    deleteTrace 
  } = useModel()

  // 构建节点数据
  const initialNodes = useMemo(() => {
    const nodes: Node[] = []
    
    // 添加需求定义节点
    requirements.forEach((req, index) => {
      nodes.push({
        id: req.id,
        type: 'default',
        position: { x: 100 + (index % 3) * 200, y: 100 + Math.floor(index / 3) * 150 },
        data: { 
          label: `${req.reqId}: ${req.name}` 
        },
        style: {
          background: selectedId === req.id ? '#bae7ff' : '#fff',
          border: selectedId === req.id ? '2px solid #1890ff' : '1px solid #d9d9d9',
          borderRadius: '8px',
          padding: '10px'
        }
      })
    })
    
    // 添加需求用法节点
    usages.forEach((usage, index) => {
      nodes.push({
        id: usage.id,
        type: 'default',
        position: { x: 100 + (index % 3) * 200, y: 400 + Math.floor(index / 3) * 150 },
        data: { 
          label: usage.name 
        },
        style: {
          background: selectedId === usage.id ? '#f6ffed' : '#fff',
          border: selectedId === usage.id ? '2px solid #52c41a' : '1px solid #d9d9d9',
          borderRadius: '8px',
          padding: '10px'
        }
      })
    })
    
    return nodes
  }, [requirements, usages, selectedId])

  // 构建边数据
  const initialEdges = useMemo(() => {
    const edges: Edge[] = []
    
    // 添加追溯关系边
    traces.forEach(trace => {
      edges.push({
        id: trace.id,
        source: trace.fromId,
        target: trace.toId,
        type: 'default',
        animated: trace.type === 'derive',
        label: trace.type,
        style: {
          stroke: getEdgeColor(trace.type)
        },
        markerEnd: {
          type: MarkerType.ArrowClosed,
          color: getEdgeColor(trace.type)
        }
      })
    })
    
    // 添加Usage到Definition的关系边
    usages.forEach(usage => {
      if (usage.of) {
        edges.push({
          id: `usage-${usage.id}`,
          source: usage.of,
          target: usage.id,
          type: 'default',
          animated: false,
          label: 'usage of',
          style: {
            stroke: '#999',
            strokeDasharray: '5,5'
          },
          markerEnd: {
            type: MarkerType.ArrowClosed,
            color: '#999'
          }
        })
      }
    })
    
    return edges
  }, [traces, usages])

  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes)
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges)

  // 更新节点和边
  React.useEffect(() => {
    setNodes(initialNodes)
  }, [initialNodes, setNodes])

  React.useEffect(() => {
    setEdges(initialEdges)
  }, [initialEdges, setEdges])

  // 处理节点点击
  const onNodeClick = useCallback((event: React.MouseEvent, node: Node) => {
    selectElement(node.id)
  }, [selectElement])

  // 处理连线
  const onConnect = useCallback(
    async (params: Connection) => {
      if (params.source && params.target) {
        try {
          await createTrace(params.source, params.target, 'trace')
          // 成功后会通过Context更新traces，自动触发重新渲染
        } catch (error) {
          console.error('创建追溯失败:', error)
        }
      }
    },
    [createTrace]
  )

  // 处理边删除
  const onEdgeClick = useCallback(
    async (event: React.MouseEvent, edge: Edge) => {
      // 只删除追溯关系，不删除usage关系
      if (!edge.id.startsWith('usage-')) {
        if (window.confirm(`确定删除追溯关系 ${edge.label}?`)) {
          try {
            await deleteTrace(edge.id)
          } catch (error) {
            console.error('删除追溯失败:', error)
          }
        }
      }
    },
    [deleteTrace]
  )

  return (
    <Card className="graph-view-card" style={{ height: '100%' }}>
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onConnect={onConnect}
        onNodeClick={onNodeClick}
        onEdgeClick={onEdgeClick}
        fitView
      >
        <Controls />
        <MiniMap />
        <Background variant={BackgroundVariant.Dots} gap={12} size={1} />
      </ReactFlow>
    </Card>
  )
}

// 根据追溯类型获取边的颜色
function getEdgeColor(type: string): string {
  switch (type) {
    case 'derive':
      return '#1890ff' // 蓝色
    case 'satisfy':
      return '#52c41a' // 绿色
    case 'refine':
      return '#fa8c16' // 橙色
    case 'trace':
    default:
      return '#8c8c8c' // 灰色
  }
}

export default GraphView