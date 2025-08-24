import React, { useEffect, useState, useMemo } from 'react';
import { Table, Tag, Space, Button, Tooltip, Input, Select, Card } from 'antd';
import { EditOutlined, DeleteOutlined, SearchOutlined } from '@ant-design/icons';
import { useModelContext, TableRowData } from '../../../contexts/ModelContext';

const { Search } = Input;
const { Option } = Select;

/**
 * 表视图组件 - 基于通用接口和SSOT实现
 * REQ-D2-1: 表视图接口
 * REQ-D2-2: 联动 - 点击表行高亮
 * REQ-A1-1: 数据源唯一 - 从SSOT获取数据
 */
const TableView: React.FC = () => {
  const { 
    selectedIds, 
    selectElement, 
    deleteElement, 
    getTableViewData,
    loadAllElements,
    loading,
    elements  // 添加elements依赖
  } = useModelContext();

  const [filteredData, setFilteredData] = useState<TableRowData[]>([]);
  const [filterText, setFilterText] = useState('');
  const [filterType, setFilterType] = useState<string>('all');

  // 获取表格数据 - 使用useMemo避免无限重渲染
  const tableData = useMemo(() => getTableViewData(), [elements]);

  // 初始加载数据
  useEffect(() => {
    const loadData = async () => {
      try {
        await loadAllElements()
      } catch (error) {
        console.error('TableView: 加载数据失败', error)
      }
    }
    
    if (tableData.length === 0) {
      loadData()
    }
  }, []); // 移除循环依赖，只在挂载时执行一次

  // 过滤数据
  useEffect(() => {
    let data = tableData;

    // 类型过滤
    if (filterType !== 'all') {
      data = data.filter(item => item.eClass === filterType);
    }

    // 文本过滤
    if (filterText) {
      data = data.filter(item => 
        item.declaredName?.toLowerCase().includes(filterText.toLowerCase()) ||
        item.declaredShortName?.toLowerCase().includes(filterText.toLowerCase()) ||
        item.eClass.toLowerCase().includes(filterText.toLowerCase())
      );
    }

    setFilteredData(data);
  }, [tableData, filterText, filterType]);

  const columns = [
    {
      title: '类型',
      dataIndex: 'eClass',
      key: 'eClass',
      width: 150,
      render: (eClass: string) => (
        <Tag color="geekblue">{eClass}</Tag>
      ),
    },
    {
      title: 'Short Name',
      dataIndex: 'declaredShortName',
      key: 'declaredShortName',
      width: 150,
    },
    {
      title: '名称',
      dataIndex: 'declaredName',
      key: 'declaredName',
      width: 250,
      render: (text: string, record: TableRowData) => (
        <span data-testid={`table-row-name-${record.id}`}>
          {text || record.declaredShortName || record.id}
        </span>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => {
        const color = status === 'active' ? 'green' : status === 'draft' ? 'orange' : 'default';
        return <Tag color={color}>{status || 'active'}</Tag>;
      },
    },
    {
      title: '引用关系',
      key: 'references',
      width: 150,
      render: (_: any, record: TableRowData) => {
        const refs = [];
        if (record.of) refs.push(`of: ${record.of}`);
        if (record.subject) refs.push(`subject: ${record.subject}`);
        if (record.source) refs.push(`source: ${record.source}`);
        if (record.target) refs.push(`target: ${record.target}`);
        
        return refs.length > 0 ? (
          <Tooltip title={refs.join(', ')}>
            <Tag>{refs.length} 个引用</Tag>
          </Tooltip>
        ) : '-';
      },
    },
    {
      title: '操作',
      key: 'action',
      width: 120,
      render: (_: any, record: TableRowData) => (
        <Space size="small">
          <Button 
            type="link" 
            icon={<EditOutlined />} 
            size="small"
            data-testid={`edit-button-${record.id}`}
            onClick={(e) => {
              e.stopPropagation();
              selectElement(record.id);
            }}
          />
          <Button 
            type="link" 
            icon={<DeleteOutlined />} 
            size="small"
            danger
            onClick={async (e) => {
              e.stopPropagation();
              try {
                await deleteElement(record.id);
              } catch (error) {
                console.error('删除失败:', error);
              }
            }}
          />
        </Space>
      ),
    },
  ];

  // 获取选中状态的行样式
  const getRowClassName = (record: TableRowData) => {
    return selectedIds.has(record.id) ? 'table-row-selected selected' : '';
  };

  // 处理行点击
  const handleRowClick = (record: TableRowData, index: number, event: React.MouseEvent) => {
    const multiSelect = event.ctrlKey || event.metaKey;
    selectElement(record.id, multiSelect);
    
    // 模拟滚动到视图（为了测试）
    const element = event.currentTarget as HTMLElement;
    if (element.scrollIntoView) {
      element.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }
  };

  // 获取唯一的类型列表用于过滤
  const elementTypes = Array.from(new Set(tableData.map(item => item.eClass))).sort();

  return (
    <Card 
      className="table-view-card" 
      data-testid="table-view"
      title="元素表格"
    >
      {/* 过滤器 */}
      <Space style={{ marginBottom: 16 }}>
        <Search
          placeholder="搜索名称或类型"
          allowClear
          style={{ width: 250 }}
          value={filterText}
          onChange={(e) => setFilterText(e.target.value)}
          data-testid="table-filter-input"
          prefix={<SearchOutlined />}
        />
        <Select
          value={filterType}
          onChange={setFilterType}
          style={{ width: 180 }}
          placeholder="选择类型"
        >
          <Option value="all">所有类型</Option>
          {elementTypes.map(type => (
            <Option key={type} value={type}>{type}</Option>
          ))}
        </Select>
      </Space>

      <Table 
        columns={columns}
        dataSource={filteredData}
        rowKey="id"
        size="small"
        loading={loading}
        locale={{
          emptyText: filteredData.length === 0 && tableData.length > 0 ? '无匹配数据' : '暂无数据'
        }}
        pagination={{
          pageSize: 20,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total, range) => `第 ${range[0]}-${range[1]} 条，共 ${total} 条`,
        }}
        rowClassName={getRowClassName}
        onRow={(record, index) => ({
          onClick: (event) => handleRowClick(record, index!, event),
          'data-testid': `table-row-${record.id}`,
          className: selectedIds.has(record.id) ? 'selected' : '',
        })}
        scroll={{ x: 800 }}
      />
    </Card>
  );
};

export default TableView;