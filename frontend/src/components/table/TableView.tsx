/**
 * TableView组件 - REQ-F2-2: 表视图数据集成
 * 
 * 功能特性：
 * - 显示reqId、declaredName、documentation等核心字段
 * - 显示status、priority、verificationMethod等元数据字段
 * - Usage行显示requirementDefinition关联
 * - 支持字段排序和过滤
 * - 分页表格，内联编辑
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
import { requirementService } from '../../services/requirementService'
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
  /** 是否支持排序 */
  sortable?: boolean
  /** 是否支持过滤 */
  filterable?: boolean
  /** 是否显示关联字段 */
  showRelation?: boolean
  /** 是否支持分页 */
  pageable?: boolean
  /** 页面大小 */
  pageSize?: number
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
  sortable = true,
  filterable = true,
  showRelation = false,
  pageable = true,
  pageSize = 50,
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
  const [definitionMap, setDefinitionMap] = useState<Map<string, string>>(new Map())

  // 从ModelContext获取表格数据
  const tableData = useMemo(() => {
    const data = getTableViewData()
    
    // 创建Definition映射
    const defMap = new Map<string, string>()
    data
      .filter((r: any) => r.eClass === 'RequirementDefinition')
      .forEach((def: any) => {
        defMap.set(def.id, def.reqId || def.declaredName || def.id)
      })
    setDefinitionMap(defMap)
    
    return data
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
      // 不需要loadData，updateElement已经更新了本地状态
    } catch (error) {
      message.error('保存失败')
      console.error('保存编辑失败:', error)
    }
  }, [editingRow, form, updateElement])

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
      // 不需要loadData，deleteElement已经更新了本地状态
    } catch (error) {
      message.error('删除失败')
      console.error('删除失败:', error)
    }
  }, [deleteElement])

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

  // 默认列配置 - 使用标准化字段
  const defaultColumns: ColumnsType<TableRowData> = useMemo(() => {
    const cols: ColumnsType<TableRowData> = [
      {
        title: '类型',
        dataIndex: 'eClass',
        key: 'eClass',
        width: 100,
        sorter: sortable,
        filters: filterable ? [
          { text: 'RequirementDefinition', value: 'RequirementDefinition' },
          { text: 'RequirementUsage', value: 'RequirementUsage' }
        ] : undefined,
        filterIcon: <FilterOutlined />,
        render: (eClass: string) => (
          <Tag color={eClass === 'RequirementDefinition' ? 'blue' : 'green'}>
            {eClass === 'RequirementDefinition' ? '定义' : '使用'}
          </Tag>
        )
      },
      {
        title: '需求ID',
        dataIndex: 'reqId',
        key: 'reqId',
        width: 120,
        sorter: sortable,
        render: (reqId?: string, record: any) => {
          if (record.eClass === 'RequirementDefinition') {
            return <strong>{reqId || '-'}</strong>
          }
          // Usage显示关联的Definition
          if (record.requirementDefinition) {
            const defName = definitionMap.get(record.requirementDefinition)
            return (
              <span style={{ color: '#1890ff' }}>
                → {defName || record.requirementDefinition}
              </span>
            )
          }
          return '-'
        }
      },
      {
        title: '名称',
        dataIndex: 'declaredName',
        key: 'declaredName',
        width: 200,
        sorter: sortable,
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
          return <strong>{text || '-'}</strong>
        }
      },
      {
        title: '文档',
        dataIndex: 'documentation',
        key: 'documentation',
        width: 300,
        ellipsis: true,
        render: (doc?: string) => doc || '-'
      },
      {
        title: '状态',
        dataIndex: 'status',
        key: 'status',
        width: 100,
        sorter: sortable,
        filters: filterable ? [
          { text: 'approved', value: 'approved' },
          { text: 'draft', value: 'draft' },
          { text: 'implemented', value: 'implemented' },
          { text: 'verified', value: 'verified' },
          { text: 'deprecated', value: 'deprecated' }
        ] : undefined,
        filterIcon: <FilterOutlined />,
        render: (status: string) => {
          return status ? (
            <Tag color={getStatusColor(status)}>
              {status}
            </Tag>
          ) : '-'
        }
      },
      {
        title: '优先级',
        dataIndex: 'priority',
        key: 'priority',
        width: 100,
        sorter: sortable,
        render: (priority?: string) => {
          const colorMap: Record<string, string> = {
            'P0': 'red',
            'P1': 'orange',
            'P2': 'blue',
            'P3': 'default'
          }
          return priority ? (
            <Tag color={colorMap[priority] || 'default'}>
              {priority}
            </Tag>
          ) : '-'
        }
      },
      {
        title: '验证方法',
        dataIndex: 'verificationMethod',
        key: 'verificationMethod',
        width: 120,
        filters: filterable ? [
          { text: 'test', value: 'test' },
          { text: 'analysis', value: 'analysis' },
          { text: 'inspection', value: 'inspection' },
          { text: 'demonstration', value: 'demonstration' }
        ] : undefined,
        render: (method?: string) => method || '-'
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
    ]

    // 如果showRelation为true，添加关联定义列
    if (showRelation) {
      cols.splice(2, 0, {
        title: '关联定义',
        dataIndex: 'requirementDefinition',
        key: 'requirementDefinition',
        width: 150,
        render: (defId?: string) => defId || '-'
      })
    }

    return cols
  }, [editingRow, editable, sortable, filterable, showRelation, definitionMap, handleEdit, handleSave, handleCancel, handleDelete])

  // 最终列配置
  const finalColumns = customColumns || defaultColumns

  return (
    <div className={`table-view-container ${className}`} style={{ padding: '16px' }}>
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
        {/* 标题 */}
        <Title level={4} style={{ margin: 0 }}>
          需求表格视图
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
            pagination={pageable ? {
              current: pagination.page + 1,
              pageSize: pagination.size,
              total: pagination.totalElements,
              showSizeChanger: true,
              showQuickJumper: true,
              showTotal: (total, range) => 
                `第 ${range[0]}-${range[1]} 条，共 ${total} 条`,
              pageSizeOptions: ['10', '20', '50', '100'],
              size: 'default'
            } : false}
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