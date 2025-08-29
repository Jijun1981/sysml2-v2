/**
 * 简化的图视图 - 确保容器有明确尺寸
 */

import React, { useState, useEffect } from 'react'
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
  selectedIds?: Set<string>
}

const SimpleGraph: React.FC<SimpleGraphProps> = ({ onNodeSelect, selectedIds = new Set() }) => {
  const [nodes, setNodes, onNodesChange] = useNodesState([])
  const [edges, setEdges, onEdgesChange] = useEdgesState([])

  useEffect(() => {
    // 使用简单的静态图结构
    const newNodes: Node[] = [
      {
        id: 'root',
        type: 'default',
        position: { x: 150, y: 50 },
        data: {
          label: (
            <div style={{ padding: '8px', minWidth: '150px', textAlign: 'center' }}>
              <div style={{ fontWeight: 'bold', marginBottom: '4px' }}>
                系统需求
              </div>
              <div style={{ fontSize: '12px', color: '#666' }}>
                主要需求
              </div>
            </div>
          )
        },
        style: {
          backgroundColor: selectedIds.has('root') ? '#e6f7ff' : '#ffffff',
          border: selectedIds.has('root') ? '2px solid #1890ff' : '1px solid #d9d9d9',
          borderRadius: '8px',
          width: 180
        }
      },
      {
        id: 'req1',
        type: 'default',
        position: { x: 30, y: 200 },
        data: {
          label: (
            <div style={{ padding: '8px', minWidth: '150px', textAlign: 'center' }}>
              <div style={{ fontWeight: 'bold', marginBottom: '4px' }}>
                功能需求1
              </div>
              <div style={{ fontSize: '12px', color: '#666' }}>
                系统功能
              </div>
            </div>
          )
        },
        style: {
          backgroundColor: selectedIds.has('req1') ? '#e6f7ff' : '#ffffff',
          border: selectedIds.has('req1') ? '2px solid #1890ff' : '1px solid #d9d9d9',
          borderRadius: '8px',
          width: 180
        }
      },
      {
        id: 'req2',
        type: 'default',
        position: { x: 270, y: 200 },
        data: {
          label: (
            <div style={{ padding: '8px', minWidth: '150px', textAlign: 'center' }}>
              <div style={{ fontWeight: 'bold', marginBottom: '4px' }}>
                性能需求1
              </div>
              <div style={{ fontSize: '12px', color: '#666' }}>
                性能指标
              </div>
            </div>
          )
        },
        style: {
          backgroundColor: selectedIds.has('req2') ? '#e6f7ff' : '#ffffff',
          border: selectedIds.has('req2') ? '2px solid #1890ff' : '1px solid #d9d9d9',
          borderRadius: '8px',
          width: 180
        }
      }
    ]

    const newEdges: Edge[] = [
      {
        id: 'e1-root-req1',
        source: 'root',
        target: 'req1',
        type: 'smoothstep',
        markerEnd: {
          type: MarkerType.ArrowClosed
        },
        style: {
          stroke: '#1890ff'
        }
      },
      {
        id: 'e2-root-req2',
        source: 'root',
        target: 'req2',
        type: 'smoothstep',
        markerEnd: {
          type: MarkerType.ArrowClosed
        },
        style: {
          stroke: '#1890ff'
        }
      }
    ]

    setNodes(newNodes)
    setEdges(newEdges)
  }, [selectedIds, setNodes, setEdges])

  const handleNodeClick = (event: React.MouseEvent, node: Node) => {
    console.log('节点点击:', node.id)
    onNodeSelect?.(node.id)
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