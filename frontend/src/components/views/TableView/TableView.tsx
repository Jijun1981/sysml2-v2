import React, { useState, useMemo } from 'react'
import { Table, Card, Tag, Input, Select, Space } from 'antd'
import { SearchOutlined } from '@ant-design/icons'
import { useModelContext } from '../../../contexts/ModelContext'
import type { ColumnsType } from 'antd/es/table'

const { Search } = Input
const { Option } = Select

/**
 * 表视图组件 - 完全基于SSOT和通用接口
 * 需求实现：
 * - REQ-D2-1: 从通用接口获取数据，客户端渲染表格
 * - REQ-D2-2: 通过Context实现联动
 * - REQ-A1-1: 作为SSOT的投影视图
 */
const TableView: React.FC = () => {
  const { 
    elements,
    selectedIds,
    loading,
    selectElement,
    loadAllElements
  } = useModelContext()
  
  const [filterText, setFilterText] = useState('')
  const [filterType, setFilterType] = useState<string>('all')
  
  // REQ-D2-1: 从SSOT构建表格数据
  const tableData = useMemo(() => {
    return Object.values(elements).map(element => ({
      key: element.id,
      id: element.id,
      eClass: element.eClass,
      declaredShortName: element.attributes?.declaredShortName || '',
      declaredName: element.attributes?.declaredName || '',
      status: element.attributes?.status || 'active',
      of: element.attributes?.of,
      source: element.attributes?.source,
      target: element.attributes?.target
    }))
  }, [elements])
  
  // 客户端筛选和排序
  const filteredData = useMemo(() => {
    let data = tableData
    
    // 类型筛选
    if (filterType !== 'all') {
      data = data.filter(item => item.eClass === filterType)
    }
    
    // 文本筛选
    if (filterText) {
      const searchText = filterText.toLowerCase()
      data = data.filter(item => 
        item.declaredName.toLowerCase().includes(searchText) ||
        item.declaredShortName.toLowerCase().includes(searchText) ||
        item.id.toLowerCase().includes(searchText)
      )
    }
    
    return data
  }, [tableData, filterText, filterType])
  
  // 获取所有的元素类型
  const elementTypes = useMemo(() => {
    const types = new Set(tableData.map(item => item.eClass))
    return Array.from(types).sort()
  }, [tableData])
  
  // 表格列定义
  const columns: ColumnsType<any> = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 200,
      ellipsis: true,
    },
    {
      title: '类型',
      dataIndex: 'eClass',
      key: 'eClass',
      width: 180,
      render: (eClass: string) => (
        <Tag color="blue">{eClass}</Tag>
      ),
    },
    {
      title: '短名称',
      dataIndex: 'declaredShortName',
      key: 'declaredShortName',
      width: 150,
    },
    {
      title: '名称',
      dataIndex: 'declaredName',
      key: 'declaredName',
      width: 250,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => {
        const color = status === 'active' ? 'green' : 'orange'
        return <Tag color={color}>{status}</Tag>
      },
    },
    {
      title: '关系',
      key: 'relations',
      width: 150,
      render: (_: any, record: any) => {
        const relations = []
        if (record.of) relations.push(`of:${record.of.slice(0, 8)}...`)
        if (record.source) relations.push(`src:${record.source.slice(0, 8)}...`)
        if (record.target) relations.push(`tgt:${record.target.slice(0, 8)}...`)
        
        return relations.length > 0 ? (
          <span style={{ fontSize: '12px' }}>{relations.join(', ')}</span>
        ) : '-'
      },
    },
  ]
  
  // REQ-D2-2: 处理行点击 - 通过Context管理联动
  const handleRowClick = (record: any, event: React.MouseEvent) => {
    const multiSelect = event.ctrlKey || event.metaKey
    selectElement(record.id, multiSelect)
  }
  
  // 初始加载数据（如果需要）
  React.useEffect(() => {
    if (Object.keys(elements).length === 0 && !loading) {
      loadAllElements()
    }
  }, [])
  
  return (
    <Card 
      className="table-view-card"
      title="元素列表"
      size="small"
    >
      {/* 筛选器 */}
      <Space style={{ marginBottom: 16 }}>
        <Search
          placeholder="搜索ID、名称或短名称"
          allowClear
          style={{ width: 250 }}
          value={filterText}
          onChange={(e) => setFilterText(e.target.value)}
          prefix={<SearchOutlined />}
        />
        <Select
          value={filterType}
          onChange={setFilterType}
          style={{ width: 180 }}
        >
          <Option value="all">所有类型</Option>
          {elementTypes.map(type => (
            <Option key={type} value={type}>{type}</Option>
          ))}
        </Select>
      </Space>
      
      {/* 表格 */}
      <Table
        columns={columns}
        dataSource={filteredData}
        loading={loading}
        size="small"
        pagination={{
          pageSize: 20,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total) => `共 ${total} 条`,
        }}
        rowClassName={(record) => 
          selectedIds.has(record.id) ? 'table-row-selected' : ''
        }
        onRow={(record) => ({
          onClick: (event) => handleRowClick(record, event),
          className: selectedIds.has(record.id) ? 'selected' : '',
        })}
        scroll={{ x: 1000 }}
      />
    </Card>
  )
}

export default TableView