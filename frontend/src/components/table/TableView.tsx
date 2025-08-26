/**
 * TableView组件 - TDD第六阶段
 * 
 * 功能特性：
 * - REQ-D2-1: 分页表格，内联编辑
 * - 排序和过滤
 * - 行选择和批量操作
 * - 搜索功能
 * - 响应式设计
 */

import React, { useState, useCallback, useMemo, useEffect } from 'react'
import { 
  Table, 
  Input, 
  Typography, 
  Space, 
  Tag, 
  Button, 
  Popconfirm,
  message,
  Form,
  Select,
  DatePicker
} from 'antd'
import {
  EditOutlined,
  DeleteOutlined,
  SaveOutlined,
  CloseOutlined,
  SearchOutlined,
  FilterOutlined
} from '../../utils/icons'
import { useModelContext } from '../../contexts/ModelContext'
import type { TableRowData, QueryParams, SortParam, FilterParam } from '../../types/models'
import type { ColumnsType, TableProps } from 'antd/es/table'
import type { SorterResult, TableCurrentDataSource } from 'antd/es/table/interface'

const { Title } = Typography
const { Search } = Input
const { Option } = Select

// TableView组件属性
interface TableViewProps {
  /** 是否支持编辑 */
  editable?: boolean
  /** 是否支持搜索 */
  searchable?: boolean
  /** 是否支持多选 */
  selectable?: boolean
  /** 自定义列配置 */
  columns?: ColumnsType<TableRowData>
  /** 自定义样式类名 */
  className?: string
  /** 表格大小 */
  size?: 'small' | 'middle' | 'large'
  /** 是否显示边框 */
  bordered?: boolean
}

// 编辑行状态
interface EditingRow {
  key: string
  record: TableRowData
}

/**
 * 状态标签颜色映射
 */
const getStatusColor = (status: string): string => {
  switch (status) {
    case 'approved': return 'success'
    case 'draft': return 'processing'  
    case 'rejected': return 'error'
    case 'implemented': return 'warning'
    case 'verified': return 'cyan'
    case 'deprecated': return 'default'
    default: return 'default'
  }
}

/**
 * 状态显示文本
 */
const getStatusText = (status: string): string => {
  switch (status) {
    case 'approved': return '已批准'
    case 'draft': return '草稿'
    case 'rejected': return '已拒绝'
    case 'implemented': return '已实现'
    case 'verified': return '已验证'
    case 'deprecated': return '已废弃'
    default: return status || '未知'
  }
}

/**
 * TableView组件 - 需求数据表格展示
 */
const TableView: React.FC<TableViewProps> = ({
  editable = false,
  searchable = true,
  selectable = true,
  columns: customColumns,
  className = '',
  size = 'middle',
  bordered = true
}) => {
  const {
    getTableViewData,
    loadAllElements,
    updateElement,
    deleteElement,
    selectElement,
    selectedIds,
    pagination,
    loading,
    error
  } = useModelContext()

  // 组件状态
  const [editingRow, setEditingRow] = useState<EditingRow | null>(null)
  const [searchValue, setSearchValue] = useState('')
  const [currentSort, setCurrentSort] = useState<SortParam[]>([])
  const [currentFilters, setCurrentFilters] = useState<FilterParam[]>([])
  const [form] = Form.useForm()

  // 获取表格数据
  const tableData = useMemo(() => {
    try {
      return getTableViewData()
    } catch (err) {
      console.error('获取表格数据失败:', err)
      return []
    }
  }, [getTableViewData])

  // 加载数据
  const loadData = useCallback((params: QueryParams = {}) => {
    const queryParams: QueryParams = {
      page: pagination.page,
      size: pagination.size,
      ...params
    }

    if (currentSort.length > 0) {
      queryParams.sort = currentSort
    }

    if (currentFilters.length > 0) {
      queryParams.filter = currentFilters
    }

    if (searchValue.trim()) {
      queryParams.search = searchValue
    }

    loadAllElements(queryParams)
  }, [loadAllElements, pagination, currentSort, currentFilters, searchValue])

  // 处理表格变化（分页、排序、过滤）
  const handleTableChange: TableProps<TableRowData>['onChange'] = useCallback((
    paginationParams,
    filters,
    sorter,
    extra
  ) => {
    // 处理分页
    const page = (paginationParams?.current || 1) - 1
    const size = paginationParams?.pageSize || 10

    // 处理排序
    let newSort: SortParam[] = []
    if (Array.isArray(sorter)) {
      newSort = sorter.map(s => ({
        field: s.field as string,
        direction: s.order === 'ascend' ? 'asc' : 'desc'
      })).filter(s => s.direction)
    } else if (sorter && sorter.order) {
      newSort = [{
        field: sorter.field as string,
        direction: sorter.order === 'ascend' ? 'asc' : 'desc'
      }]
    }

    // 处理过滤
    let newFilters: FilterParam[] = []
    Object.entries(filters || {}).forEach(([field, values]) => {
      if (values && values.length > 0) {
        values.forEach(value => {
          if (value) {
            newFilters.push({ field, value: value.toString() })
          }
        })
      }
    })

    setCurrentSort(newSort)
    setCurrentFilters(newFilters)

    loadData({
      page,
      size,
      sort: newSort.length > 0 ? newSort : undefined,
      filter: newFilters.length > 0 ? newFilters : undefined
    })
  }, [loadData])

  // 处理搜索
  const handleSearch = useCallback((value: string) => {
    setSearchValue(value)
    loadData({
      page: 0,
      search: value || undefined
    })
  }, [loadData])

  // 处理搜索防抖
  useEffect(() => {
    const timer = setTimeout(() => {
      if (searchValue !== '') {
        handleSearch(searchValue)
      }
    }, 300)

    return () => clearTimeout(timer)
  }, [searchValue, handleSearch])

  // 编辑行
  const handleEdit = useCallback((record: TableRowData) => {
    setEditingRow({ key: record.id, record })
    form.setFieldsValue(record)
  }, [form])

  // 保存编辑
  const handleSave = useCallback(async () => {
    if (!editingRow) return

    try {
      const values = await form.validateFields()
      await updateElement(editingRow.key, values)
      setEditingRow(null)
      message.success('保存成功')
      loadData()
    } catch (error) {
      message.error('保存失败')
      console.error('保存编辑失败:', error)
    }
  }, [editingRow, form, updateElement, loadData])

  // 取消编辑
  const handleCancel = useCallback(() => {
    setEditingRow(null)
    form.resetFields()
  }, [form])

  // 删除行
  const handleDelete = useCallback(async (id: string) => {
    try {
      await deleteElement(id)
      message.success('删除成功')
      loadData()
    } catch (error) {
      message.error('删除失败')
      console.error('删除失败:', error)
    }
  }, [deleteElement, loadData])

  // 行选择
  const rowSelection = useMemo(() => {
    if (!selectable) return undefined

    return {
      selectedRowKeys: Array.from(selectedIds),
      onChange: (selectedRowKeys: React.Key[]) => {
        selectedRowKeys.forEach(key => {
          selectElement(key as string, true)
        })
      },
      onSelect: (record: TableRowData, selected: boolean) => {
        selectElement(record.id, !selected)
      },
      onSelectAll: (selected: boolean, selectedRows: TableRowData[], changeRows: TableRowData[]) => {
        changeRows.forEach(row => {
          selectElement(row.id, !selected)
        })
      }
    }
  }, [selectable, selectedIds, selectElement])

  // 默认列配置
  const defaultColumns: ColumnsType<TableRowData> = useMemo(() => [
    {
      title: '类型',
      dataIndex: 'eClass',
      key: 'eClass',
      width: 150,
      sorter: true,
      filters: [
        { text: 'RequirementDefinition', value: 'RequirementDefinition' },
        { text: 'RequirementUsage', value: 'RequirementUsage' },
        { text: 'Dependency', value: 'Dependency' }
      ],
      filterIcon: <FilterOutlined />,
      render: (eClass: string) => (
        <Tag color={eClass === 'RequirementDefinition' ? 'blue' : 'green'}>
          {eClass}
        </Tag>
      )
    },
    {
      title: '短名称',
      dataIndex: 'declaredShortName',
      key: 'declaredShortName',
      width: 120,
      sorter: true,
      render: (text: string, record: TableRowData) => {
        if (editingRow && editingRow.key === record.id) {
          return (
            <Form.Item name="declaredShortName" style={{ margin: 0 }}>
              <Input size="small" />
            </Form.Item>
          )
        }
        return text || '-'
      }
    },
    {
      title: '名称',
      dataIndex: 'declaredName',
      key: 'declaredName',
      sorter: true,
      ellipsis: true,
      render: (text: string, record: TableRowData) => {
        if (editingRow && editingRow.key === record.id) {
          return (
            <Form.Item 
              name="declaredName" 
              rules={[{ required: true, message: '请输入名称' }]}
              style={{ margin: 0 }}
            >
              <Input size="small" />
            </Form.Item>
          )
        }
        return text || '-'
      }
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      sorter: true,
      filters: [
        { text: '已批准', value: 'approved' },
        { text: '草稿', value: 'draft' },
        { text: '已拒绝', value: 'rejected' },
        { text: '已实现', value: 'implemented' },
        { text: '已验证', value: 'verified' },
        { text: '已废弃', value: 'deprecated' }
      ],
      filterIcon: <FilterOutlined />,
      render: (status: string, record: TableRowData) => {
        if (editingRow && editingRow.key === record.id) {
          return (
            <Form.Item name="status" style={{ margin: 0 }}>
              <Select size="small" style={{ width: '100%' }}>
                <Option value="approved">已批准</Option>
                <Option value="draft">草稿</Option>
                <Option value="rejected">已拒绝</Option>
                <Option value="implemented">已实现</Option>
                <Option value="verified">已验证</Option>
                <Option value="deprecated">已废弃</Option>
              </Select>
            </Form.Item>
          )
        }
        return (
          <Tag color={getStatusColor(status)}>
            {getStatusText(status)}
          </Tag>
        )
      }
    },
    {
      title: '操作',
      key: 'action',
      width: 120,
      fixed: 'right',
      render: (_, record: TableRowData) => {
        if (editingRow && editingRow.key === record.id) {
          return (
            <Space size="small">
              <Button
                type="link"
                size="small"
                icon={<SaveOutlined />}
                onClick={handleSave}
              >
                保存
              </Button>
              <Button
                type="link"
                size="small"
                icon={<CloseOutlined />}
                onClick={handleCancel}
              >
                取消
              </Button>
            </Space>
          )
        }

        return (
          <Space size="small">
            {editable && (
              <Button
                type="link"
                size="small"
                icon={<EditOutlined />}
                onClick={() => handleEdit(record)}
              >
                编辑
              </Button>
            )}
            <Popconfirm
              title="确定要删除这条记录吗？"
              onConfirm={() => handleDelete(record.id)}
              okText="确定"
              cancelText="取消"
            >
              <Button
                type="link"
                size="small"
                danger
                icon={<DeleteOutlined />}
              >
                删除
              </Button>
            </Popconfirm>
          </Space>
        )
      }
    }
  ], [editingRow, editable, handleEdit, handleSave, handleCancel, handleDelete])

  // 最终列配置
  const finalColumns = customColumns || defaultColumns

  return (
    <div className={`table-view-container ${className}`} style={{ padding: '16px' }}>
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
        {/* 标题 */}
        <Title level={4} style={{ margin: 0 }}>
          需求数据表格
        </Title>

        {/* 工具栏 */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          {/* 搜索框 */}
          {searchable && (
            <Search
              placeholder="搜索需求..."
              allowClear
              prefix={<SearchOutlined />}
              style={{ width: 300 }}
              onChange={(e) => setSearchValue(e.target.value)}
              onSearch={handleSearch}
            />
          )}

          {/* 统计信息 */}
          <Space>
            <span style={{ color: '#666' }}>
              共 {pagination.totalElements} 条记录
            </span>
            {selectedIds.size > 0 && (
              <span style={{ color: '#1890ff' }}>
                已选择 {selectedIds.size} 项
              </span>
            )}
          </Space>
        </div>

        {/* 表格 */}
        <Form form={form} component={false}>
          <Table<TableRowData>
            columns={finalColumns}
            dataSource={tableData}
            rowKey="id"
            size={size}
            bordered={bordered}
            loading={loading}
            rowSelection={rowSelection}
            pagination={{
              current: pagination.page + 1,
              pageSize: pagination.size,
              total: pagination.totalElements,
              showSizeChanger: true,
              showQuickJumper: true,
              showTotal: (total, range) => 
                `第 ${range[0]}-${range[1]} 条，共 ${total} 条`,
              pageSizeOptions: ['10', '20', '50', '100'],
              size: 'default'
            }}
            onChange={handleTableChange}
            scroll={{ x: 1000 }}
            locale={{
              emptyText: searchValue ? '未找到匹配数据' : '暂无数据'
            }}
          />
        </Form>

        {/* 错误状态 */}
        {error && (
          <div style={{ textAlign: 'center', padding: '20px', color: '#ff4d4f' }}>
            加载失败: {error.message}
          </div>
        )}
      </Space>
    </div>
  )
}

export default TableView