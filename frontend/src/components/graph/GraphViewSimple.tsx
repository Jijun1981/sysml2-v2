/**
 * GraphView简化版 - 使用React Flow展示节点关系图
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
  addEdge,
  Connection,
  BackgroundVariant,
  MarkerType
} from 'reactflow'
import 'reactflow/dist/style.css'
import { Typography } from 'antd'

const { Title } = Typography

interface GraphViewProps {
  onNodeSelect?: (nodeId: string) => void
  showMinimap?: boolean
  showControls?: boolean
}

const GraphViewSimple: React.FC<GraphViewProps> = ({
  onNodeSelect,
  showMinimap = true,
  showControls = true
}) => {
  const [nodes, setNodes, onNodesChange] = useNodesState([])
  const [edges, setEdges, onEdgesChange] = useEdgesState([])
  const [demoData, setDemoData] = useState<any>(null)

  // 加载demo数据并转换为图节点
  useEffect(() => {
    fetch('http://localhost:8080/api/v1/demo/dataset/small')
      .then(res => res.json())
      .then(data => {
        console.log('GraphView - Demo数据加载成功', data)
        if (data?.content) {
          // 转换为React Flow节点
          const flowNodes: Node[] = data.content.map((item: any, index: number) => ({
            id: item.data?.elementId || `node-${index}`,
            position: { 
              x: 100 + (index % 4) * 200,  // 4列布局
              y: 100 + Math.floor(index / 4) * 150  // 自动换行
            },
            data: { 
              label: item.data?.declaredName || item.data?.reqId || `节点${index + 1}`,
              requirement: item.data
            },
            style: {
              background: '#fff',
              border: '1px solid #1890ff',
              borderRadius: '4px',
              padding: '10px',
              width: 180
            }
          }))
          
          // 创建一些示例边（模拟依赖关系）
          const flowEdges: Edge[] = []
          if (data.content.length > 1) {
            // 创建一些示例连接
            for (let i = 1; i < Math.min(5, data.content.length); i++) {
              flowEdges.push({
                id: `edge-${i}`,
                source: data.content[0].data?.elementId || 'node-0',
                target: data.content[i].data?.elementId || `node-${i}`,
                type: 'smoothstep',
                animated: true,
                style: { stroke: '#1890ff' },
                markerEnd: {
                  type: MarkerType.ArrowClosed,
                  color: '#1890ff'
                }
              })
            }
            
            // 添加一些横向连接
            if (data.content.length > 5) {
              flowEdges.push({
                id: 'edge-horizontal-1',
                source: data.content[1].data?.elementId || 'node-1',
                target: data.content[2].data?.elementId || 'node-2',
                type: 'straight',
                style: { stroke: '#52c41a' },
                markerEnd: {
                  type: MarkerType.ArrowClosed,
                  color: '#52c41a'
                }
              })
            }
          }
          
          setNodes(flowNodes)
          setEdges(flowEdges)
          setDemoData(data)
        }
      })
      .catch(err => console.error('GraphView数据加载失败:', err))
  }, [setNodes, setEdges])

  // 处理连接创建
  const onConnect = useCallback(
    (params: Connection) => setEdges((eds) => addEdge(params, eds)),
    [setEdges]
  )

  // 处理节点点击
  const onNodeClick = useCallback((event: React.MouseEvent, node: Node) => {
    console.log('节点被点击:', node)
    onNodeSelect?.(node.id)
  }, [onNodeSelect])

  // 节点颜色映射（用于小地图）
  const nodeColor = useCallback((node: Node) => {
    return '#1890ff'
  }, [])

  return (
    <div style={{ width: '100%', height: '100%', display: 'flex', flexDirection: 'column' }}>
      <div style={{ padding: '16px', borderBottom: '1px solid #f0f0f0', flexShrink: 0 }}>
        <Title level={4}>依赖关系图</Title>
        <p style={{ color: '#666', margin: 0 }}>
          节点数: {nodes.length} | 关系数: {edges.length}
        </p>
      </div>
      <div style={{ flex: 1, position: 'relative', minHeight: '400px' }}>
        <ReactFlow
          nodes={nodes}
          edges={edges}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          onConnect={onConnect}
          onNodeClick={onNodeClick}
          fitView
        >
          {showControls && <Controls />}
          <Background variant={BackgroundVariant.Dots} gap={12} size={1} />
          {showMinimap && (
            <MiniMap 
              nodeColor={nodeColor}
              nodeStrokeWidth={3}
              pannable
              zoomable
            />
          )}
        </ReactFlow>
      </div>
    </div>
  )
}

export default GraphViewSimple