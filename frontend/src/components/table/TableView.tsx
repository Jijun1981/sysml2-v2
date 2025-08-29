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
import './TableView.css'
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
  /** 只显示Usage（REQ-UI-2） */
  usageOnly?: boolean
  /** 显示工具栏（REQ-UI-3） */
  showToolbar?: boolean
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
  usageOnly = false,
  showToolbar = false,
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
  const [definitionMap, setDefinitionMap] = useState<Map<string, any>>(new Map())

  // 从ModelContext获取表格数据
  const tableData = useMemo(() => {
    let data = getTableViewData()
    
    // 创建Definition映射，存储完整的definition对象
    const defMap = new Map<string, any>()
    data
      .filter((r: any) => r.eClass === 'RequirementDefinition')
      .forEach((def: any) => {
        defMap.set(def.id, def)
      })
    setDefinitionMap(defMap)
    
    // REQ-UI-2: 如果设置了usageOnly，只显示RequirementUsage
    if (usageOnly) {
      data = data.filter((r: any) => r.eClass === 'RequirementUsage')
    }
    
    return data
  }, [getTableViewData, usageOnly])

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

  // 行点击选中（不使用复选框）
  const handleRowClick = useCallback((record: TableRowData) => {
    if (selectable) {
      selectElement(record.id, false) // 单选模式
    }
  }, [selectable, selectElement])

  // 行样式（高亮选中行）
  const rowClassName = useCallback((record: TableRowData) => {
    if (selectedIds.has(record.id)) {
      return 'ant-table-row-selected'
    }
    return ''
  }, [selectedIds])

  // 默认列配置 - 使用实际存在的字段
  const defaultColumns: ColumnsType<TableRowData> = useMemo(() => {
    const cols: ColumnsType<TableRowData> = [
      {
        title: 'ID',
        dataIndex: 'id',
        key: 'id',
        width: 80,
        ellipsis: true,
        render: (id: string) => (
          <span style={{ fontSize: '12px', color: '#666' }}>
            {id?.substring(0, 8)}
          </span>
        )
      },
      {
        title: '类型',
        dataIndex: 'eClass',
        key: 'eClass',
        width: 100,
        render: (eClass: string) => {
          if (usageOnly) {
            return <Tag color="purple">使用</Tag>
          }
          return (
            <Tag color={eClass === 'RequirementDefinition' ? 'blue' : 'purple'}>
              {eClass === 'RequirementDefinition' ? '定义' : '使用'}
            </Tag>
          )
        }
      },
      {
        title: '短名称',
        dataIndex: 'declaredShortName',
        key: 'declaredShortName',
        width: 150,
        sorter: sortable,
        ellipsis: true,
        render: (text: string, record: TableRowData) => {
          if (editingRow && editingRow.key === record.id) {
            return (
              <Form.Item 
                name="declaredShortName" 
                rules={[{ required: false }]}
                style={{ margin: 0 }}
              >
                <Input size="small" placeholder="短名称" />
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
          return <strong>{text || record.id}</strong>
        }
      },
      {
        title: '文本描述',
        dataIndex: 'text',
        key: 'text',
        ellipsis: true,
        render: (text?: string, record: any) => {
          if (editingRow && editingRow.key === record.id) {
            return (
              <Form.Item 
                name="text" 
                style={{ margin: 0 }}
              >
                <Input.TextArea size="small" rows={2} />
              </Form.Item>
            )
          }
          const desc = text || record.documentation || '-'
          return (
            <span style={{ fontSize: '13px' }}>
              {desc.length > 100 ? `${desc.substring(0, 100)}...` : desc}
            </span>
          )
        }
      },
      {
        title: '状态',
        dataIndex: 'status',
        key: 'status',
        width: 100,
        sorter: sortable,
        filters: filterable ? [
          { text: '已批准', value: 'approved' },
          { text: '草稿', value: 'draft' },
          { text: '已实现', value: 'implemented' },
          { text: '已验证', value: 'verified' },
          { text: '已废弃', value: 'deprecated' }
        ] : undefined,
        filterIcon: <FilterOutlined />,
        render: (status: string) => {
          return status ? (
            <Tag color={getStatusColor(status)}>
              {getStatusText(status)}
            </Tag>
          ) : <Tag>草稿</Tag>
        }
      },
      {
        title: '创建时间',
        dataIndex: 'createdAt',
        key: 'createdAt',
        width: 150,
        sorter: sortable,
        render: (createdAt?: string) => {
          if (!createdAt) return '-'
          const date = new Date(createdAt)
          return date.toLocaleDateString('zh-CN') + ' ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
        }
      },
      {
        title: '更新时间',
        dataIndex: 'updatedAt',
        key: 'updatedAt',
        width: 150,
        sorter: sortable,
        render: (updatedAt?: string) => {
          if (!updatedAt) return '-'
          const date = new Date(updatedAt)
          return date.toLocaleDateString('zh-CN') + ' ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
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
    ]

    // 如果showRelation为true，添加关联定义列（仅对Usage有效）
    if (showRelation && usageOnly) {
      cols.splice(3, 0, {
        title: '关联定义',
        dataIndex: 'requirementDefinition',
        key: 'requirementDefinition',
        width: 150,
        ellipsis: true,
        render: (defId?: string, record: any) => {
          // 尝试从多个可能的位置获取requirementDefinition
          const definitionId = defId || 
                              record.requirementDefinition || 
                              record.properties?.requirementDefinition || 
                              record.of || 
                              record.properties?.of
          if (!definitionId) return '-'
          
          // 如果有definitionMap，显示定义名称
          if (definitionMap && definitionMap.has(definitionId)) {
            const def = definitionMap.get(definitionId)
            return def?.declaredShortName || def?.declaredName || definitionId.substring(0, 8)
          }
          return definitionId.substring(0, 8)
        }
      })
    }

    return cols
  }, [editingRow, editable, sortable, filterable, showRelation, usageOnly, definitionMap, handleEdit, handleSave, handleCancel, handleDelete])

  // 最终列配置
  const finalColumns = customColumns || defaultColumns

  return (
    <div className={`table-view-container ${className}`} style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
        {/* 标题栏 */}
        <div className="table-header">
          {usageOnly ? '📋 需求条目列表' : '📊 需求表格视图'}
        </div>

        {/* 工具栏 */}
        {showToolbar && (
          <div className="table-toolbar">
            <Space>
              <Button
                type="primary"
                size="small"
                icon={<EditOutlined />}
                disabled={selectedIds.size !== 1}
                onClick={() => {
                  if (selectedIds.size === 1) {
                    const selectedId = Array.from(selectedIds)[0]
                    handleEdit(tableData.find(r => r.id === selectedId)!)
                  }
                }}
              >
                编辑
              </Button>
              <Button
                danger
                size="small"
                icon={<DeleteOutlined />}
                disabled={selectedIds.size === 0}
                onClick={() => {
                  if (selectedIds.size > 0) {
                    const count = selectedIds.size
                    const confirmMsg = count === 1 
                      ? '确定要删除选中的需求吗？' 
                      : `确定要删除选中的 ${count} 个需求吗？`
                    if (window.confirm(confirmMsg)) {
                      selectedIds.forEach(id => handleDelete(id))
                    }
                  }
                }}
              >
                删除
              </Button>
              <Button
                size="small"
                onClick={() => loadData()}
              >
                刷新
              </Button>
              <div style={{ marginLeft: 'auto', marginRight: '16px' }}>
                {selectedIds.size > 0 && (
                  <span style={{ color: '#1890ff' }}>
                    已选择 {selectedIds.size} 项
                  </span>
                )}
                <span style={{ marginLeft: '16px', color: '#666' }}>
                  共 {usageOnly ? tableData.length : pagination.totalElements} 条记录
                </span>
              </div>
            </Space>
          </div>
        )}

        {/* 表格容器 */}
        <div style={{ flex: 1, overflow: 'hidden', padding: '0' }}>
          <Form form={form} component={false}>
            <Table<TableRowData>
              columns={finalColumns}
              dataSource={tableData}
              rowKey="id"
              size={size}
              bordered={bordered}
              loading={loading}
              onRow={(record) => ({
                onClick: () => handleRowClick(record),
                style: { cursor: 'pointer' }
              })}
              rowClassName={rowClassName}
              pagination={pageable ? {
                current: pagination.page + 1,
                pageSize: pagination.size,
                total: pagination.totalElements,
                showSizeChanger: true,
                showQuickJumper: false,
                showTotal: (total) => `共 ${total} 条`,
                pageSizeOptions: ['10', '20', '30', '50'],
                size: 'small'
              } : false}
              onChange={handleTableChange}
              scroll={{ y: 'calc(100vh - 280px)' }}
              locale={{
                emptyText: '暂无数据'
              }}
            />
          </Form>
        </div>
        
        {/* 错误状态 */}
        {error && (
          <div style={{ textAlign: 'center', padding: '20px', color: '#ff4d4f' }}>
            加载失败: {error.message}
          </div>
        )}
    </div>
  )
}

export default TableView