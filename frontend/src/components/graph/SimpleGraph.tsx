/**
 * 简化的图视图 - 确保容器有明确尺寸
 */

import React, { useState, useEffect, useMemo } from 'react'
import ReactFlow, {
  Node,
  Edge,
  Controls,
  Background,
  useNodesState,
  useEdgesState,
  BackgroundVariant,
  MarkerType
} from 'reactflow'
import 'reactflow/dist/style.css'

interface SimpleGraphProps {
  onNodeSelect?: (nodeId: string) => void
  dataSource?: 'small' | 'battery'
}

const SimpleGraph: React.FC<SimpleGraphProps> = ({ onNodeSelect, dataSource = 'small' }) => {
  const [nodes, setNodes, onNodesChange] = useNodesState([])
  const [edges, setEdges, onEdgesChange] = useEdgesState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    // 根据数据源加载数据
    const url = dataSource === 'battery' 
      ? 'http://localhost:8080/api/v1/demo/battery-system'
      : 'http://localhost:8080/api/v1/demo/dataset/small'
    
    fetch(url)
      .then(res => res.json())
      .then(data => {
        console.log('SimpleGraph - 数据加载成功', data)
        
        if (data?.content && data.content.length > 0) {
          // 创建节点
          const newNodes: Node[] = data.content.map((item: any, index: number) => ({
            id: item.data?.elementId || `node-${index}`,
            type: 'default',
            position: { 
              x: 150 + (index % 3) * 250,
              y: 100 + Math.floor(index / 3) * 120
            },
            data: { 
              label: (
                <div style={{ padding: '8px', minWidth: '150px' }}>
                  <div style={{ fontWeight: 'bold', marginBottom: '4px' }}>
                    {item.data?.reqId || `REQ-${index + 1}`}
                  </div>
                  <div style={{ fontSize: '12px', color: '#666' }}>
                    {item.data?.declaredName || '需求名称'}
                  </div>
                </div>
              )
            },
            style: {
              background: '#ffffff',
              border: '2px solid #1890ff',
              borderRadius: '8px',
              fontSize: '12px',
              width: 200
            }
          }))

          // 创建示例边
          const newEdges: Edge[] = []
          for (let i = 1; i < Math.min(4, data.content.length); i++) {
            newEdges.push({
              id: `e${i}`,
              source: newNodes[0].id,
              target: newNodes[i].id,
              type: 'smoothstep',
              animated: true,
              style: { 
                stroke: '#1890ff',
                strokeWidth: 2
              },
              markerEnd: {
                type: MarkerType.ArrowClosed,
                color: '#1890ff',
                width: 20,
                height: 20
              }
            })
          }

          console.log('SimpleGraph - 创建了', newNodes.length, '个节点,', newEdges.length, '条边')
          setNodes(newNodes)
          setEdges(newEdges)
        }
        setLoading(false)
      })
      .catch(err => {
        console.error('SimpleGraph - 数据加载失败:', err)
        setLoading(false)
      })
  }, [setNodes, setEdges, dataSource])

  const handleNodeClick = (event: React.MouseEvent, node: Node) => {
    console.log('节点点击:', node.id)
    onNodeSelect?.(node.id)
  }

  if (loading) {
    return (
      <div style={{ 
        width: '100%', 
        height: '500px', 
        display: 'flex', 
        alignItems: 'center', 
        justifyContent: 'center',
        background: '#fafafa',
        border: '1px solid #d9d9d9',
        borderRadius: '4px'
      }}>
        加载中...
      </div>
    )
  }

  return (
    <div style={{ 
      width: '100%', 
      height: '500px',
      border: '1px solid #d9d9d9',
      borderRadius: '4px',
      background: '#fafafa'
    }}>
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onNodeClick={handleNodeClick}
        fitView
        fitViewOptions={{ padding: 0.2 }}
        defaultViewport={{ x: 0, y: 0, zoom: 0.8 }}
      >
        <Background variant={BackgroundVariant.Dots} gap={20} size={1} color="#e0e0e0" />
        <Controls />
      </ReactFlow>
    </div>
  )
}

export default SimpleGraph