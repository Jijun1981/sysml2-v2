import React, { useMemo, useCallback } from 'react'
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
import { Card, Empty, Spin } from 'antd'
import { useModelContext } from '../../../contexts/ModelContext'
import 'reactflow/dist/style.css'
import './GraphView.css'

/**
 * 图视图组件 - 完全基于SSOT和通用接口
 * 需求实现：
 * - REQ-D3-1: 从通用接口数据构建图结构
 * - REQ-D3-2: 通过通用接口创建/删除关系
 * - REQ-D3-3: 前端自动布局
 * - REQ-A1-1: 作为SSOT的投影视图
 */
const GraphView: React.FC = () => {
  const { 
    elements,
    selectedIds,
    loading,
    selectElement,
    createElement,
    deleteElement,
    loadAllElements
  } = useModelContext()
  
  // REQ-D3-1: 从SSOT构建图数据结构
  const { nodes: initialNodes, edges: initialEdges } = useMemo(() => {
    const nodes: Node[] = []
    const edges: Edge[] = []
    
    // 布局参数
    const nodeWidth = 150
    const nodeHeight = 50
    const horizontalSpacing = 200
    const verticalSpacing = 100
    
    // 按类型分组
    const definitions = Object.values(elements).filter(el => el.eClass === 'RequirementDefinition')
    const usages = Object.values(elements).filter(el => el.eClass === 'RequirementUsage')
    const dependencies = Object.values(elements).filter(el => 
      ['Satisfy', 'DeriveRequirement', 'Refine', 'Dependency'].includes(el.eClass)
    )
    
    // 创建Definition节点（第一行）
    definitions.forEach((def, index) => {
      nodes.push({
        id: def.id,
        type: 'default',
        position: { 
          x: index * horizontalSpacing, 
          y: 0 
        },
        data: { 
          label: def.attributes?.declaredName || def.attributes?.declaredShortName || def.id 
        },
        style: {
          background: selectedIds.has(def.id) ? '#b3d9ff' : '#fff',
          border: '2px solid #1890ff',
          borderRadius: '5px',
          padding: '10px',
          width: nodeWidth,
        }
      })
    })
    
    // 创建Usage节点（第二行）
    usages.forEach((usage, index) => {
      nodes.push({
        id: usage.id,
        type: 'default',
        position: { 
          x: index * horizontalSpacing, 
          y: verticalSpacing 
        },
        data: { 
          label: usage.attributes?.declaredName || usage.attributes?.declaredShortName || usage.id 
        },
        style: {
          background: selectedIds.has(usage.id) ? '#d4f1d4' : '#fff',
          border: '2px solid #52c41a',
          borderRadius: '5px',
          padding: '10px',
          width: nodeWidth,
        }
      })
      
      // 创建of关系边
      if (usage.attributes?.of) {
        edges.push({
          id: `of-${usage.id}`,
          source: usage.attributes.of,
          target: usage.id,
          type: 'smoothstep',
          animated: true,
          label: 'of',
          markerEnd: {
            type: MarkerType.ArrowClosed,
          },
          style: { stroke: '#1890ff' }
        })
      }
    })
    
    // 创建依赖关系边
    dependencies.forEach(dep => {
      if (dep.attributes?.source && dep.attributes?.target) {
        edges.push({
          id: dep.id,
          source: dep.attributes.source,
          target: dep.attributes.target,
          type: 'smoothstep',
          label: dep.eClass.toLowerCase(),
          markerEnd: {
            type: MarkerType.ArrowClosed,
          },
          style: { 
            stroke: dep.eClass === 'Satisfy' ? '#52c41a' : 
                   dep.eClass === 'DeriveRequirement' ? '#fa8c16' : 
                   '#722ed1'
          }
        })
      }
    })
    
    // 添加其他类型的节点（第三行）
    const otherElements = Object.values(elements).filter(el => 
      !['RequirementDefinition', 'RequirementUsage', 'Satisfy', 'DeriveRequirement', 'Refine', 'Dependency'].includes(el.eClass)
    )
    
    otherElements.forEach((el, index) => {
      nodes.push({
        id: el.id,
        type: 'default',
        position: { 
          x: index * horizontalSpacing, 
          y: verticalSpacing * 2 
        },
        data: { 
          label: el.attributes?.declaredName || el.attributes?.declaredShortName || el.id 
        },
        style: {
          background: selectedIds.has(el.id) ? '#ffe7ba' : '#fff',
          border: '2px solid #fa8c16',
          borderRadius: '5px',
          padding: '10px',
          width: nodeWidth,
        }
      })
    })
    
    return { nodes, edges }
  }, [elements, selectedIds])
  
  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes)
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges)
  
  // 更新节点和边
  React.useEffect(() => {
    setNodes(initialNodes)
    setEdges(initialEdges)
  }, [initialNodes, initialEdges, setNodes, setEdges])
  
  // REQ-D3-2: 处理连线 - 创建依赖关系
  const onConnect = useCallback(async (params: Connection) => {
    if (params.source && params.target) {
      try {
        // 创建Satisfy关系（可以根据需要选择其他类型）
        const newDependency = await createElement('Satisfy', {
          source: params.source,
          target: params.target
        })
        
        // 添加新边到图中
        setEdges((eds) => addEdge({
          ...params,
          id: newDependency.id,
          label: 'satisfy',
          markerEnd: {
            type: MarkerType.ArrowClosed,
          },
          style: { stroke: '#52c41a' }
        }, eds))
      } catch (error) {
        console.error('创建关系失败:', error)
        // REQ-D3-2: 失败时回滚UI
      }
    }
  }, [createElement, setEdges])
  
  // 处理节点点击 - 选中联动
  const onNodeClick = useCallback((event: React.MouseEvent, node: Node) => {
    const multiSelect = event.ctrlKey || event.metaKey
    selectElement(node.id, multiSelect)
  }, [selectElement])
  
  // REQ-D3-2: 处理边删除 - 删除依赖关系
  const onEdgesDelete = useCallback(async (edgesToDelete: Edge[]) => {
    for (const edge of edgesToDelete) {
      // 只删除依赖关系，不删除of关系
      if (!edge.id.startsWith('of-')) {
        try {
          await deleteElement(edge.id)
        } catch (error) {
          console.error('删除关系失败:', error)
          // REQ-D3-2: 失败时回滚UI
          setEdges((eds) => [...eds, edge])
        }
      }
    }
  }, [deleteElement, setEdges])
  
  // 初始加载数据（如果需要）
  React.useEffect(() => {
    if (Object.keys(elements).length === 0 && !loading) {
      loadAllElements()
    }
  }, [])
  
  return (
    <Card 
      className="graph-view-card"
      title="关系图"
      size="small"
      style={{ height: '100%' }}
    >
      <Spin spinning={loading}>
        {nodes.length > 0 ? (
          <div style={{ height: '500px' }}>
            <ReactFlow
              nodes={nodes}
              edges={edges}
              onNodesChange={onNodesChange}
              onEdgesChange={onEdgesChange}
              onConnect={onConnect}
              onNodeClick={onNodeClick}
              onEdgesDelete={onEdgesDelete}
              fitView
            >
              <Background variant={BackgroundVariant.Dots} />
              <Controls />
              <MiniMap />
            </ReactFlow>
          </div>
        ) : (
          <Empty 
            description={loading ? "加载中..." : "暂无数据"}
            image={Empty.PRESENTED_IMAGE_SIMPLE}
          />
        )}
      </Spin>
    </Card>
  )
}

export default GraphView