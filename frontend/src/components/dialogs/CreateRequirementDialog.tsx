import React, { useState, useEffect } from 'react'
import {
  Modal,
  Form,
  Input,
  Select,
  Switch,
  Tag,
  Space,
  message
} from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { requirementService } from '../../services/requirementService'
import { errorHandler } from '../../utils/errorHandler'

const { TextArea } = Input
const { Option } = Select

interface CreateRequirementDialogProps {
  open: boolean
  onClose: () => void
  onSuccess: (newRequirement?: any) => void
  type?: 'definition' | 'usage'
  templateId?: string // 如果是从模板创建
  definitionId?: string // 如果是创建Usage，基于哪个Definition
}

const CreateRequirementDialog: React.FC<CreateRequirementDialogProps> = ({
  open,
  onClose,
  onSuccess,
  type = 'definition',
  templateId,
  definitionId
}) => {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [tags, setTags] = useState<string[]>([])
  const [inputTag, setInputTag] = useState('')
  const [definitions, setDefinitions] = useState<any[]>([])
  const [loadingDefinitions, setLoadingDefinitions] = useState(false)

  // 加载可用的Definition列表
  useEffect(() => {
    if (type === 'usage' && open && !definitionId) {
      loadDefinitions()
    }
  }, [type, open, definitionId])

  const loadDefinitions = async () => {
    try {
      setLoadingDefinitions(true)
      const response = await fetch('http://localhost:8080/api/v1/requirements?page=0&size=100')
      const data = await response.json()
      const definitionList = data.content || data || []
      setDefinitions(definitionList)
    } catch (error) {
      console.error('加载Definition失败:', error)
      errorHandler.handleApiError(error as any, '加载需求定义列表失败')
    } finally {
      setLoadingDefinitions(false)
    }
  }

  const handleSubmit = async () => {
    console.log('handleSubmit called')
    try {
      const values = await form.validateFields()
      console.log('Form validated, values:', values)
      setLoading(true)

      const data = {
        ...values,
        elementId: values.reqId || `REQ-${Date.now()}`, // 确保有elementId
        tags: tags.length > 0 ? tags : undefined,
        isAbstract: values.isAbstract || false
      }

      let newRequirement
      if (type === 'definition') {
        newRequirement = await requirementService.createRequirementDefinition(data)
        message.success('需求定义创建成功')
      } else {
        console.log('Creating RequirementUsage')
        // RequirementUsage必须基于Definition创建
        const selectedDefId = values.definitionId || definitionId
        console.log('Selected Definition ID:', selectedDefId)
        if (!selectedDefId) {
          throw new Error('必须选择一个需求定义')
        }
        
        const usageData = {
          ...data,
          requirementDefinition: selectedDefId // 使用标准化的requirementDefinition字段
        }
        delete usageData.definitionId // 移除表单字段，使用'requirementDefinition'代替
        console.log('Calling createRequirementUsage with:', usageData)
        newRequirement = await requirementService.createRequirementUsage(usageData)
        console.log('createRequirementUsage completed')
        message.success('需求使用创建成功')
      }

      form.resetFields()
      setTags([])
      onSuccess(newRequirement)
      onClose()
    } catch (error: any) {
      errorHandler.handleApiError(error)
    } finally {
      setLoading(false)
    }
  }

  const handleAddTag = () => {
    if (inputTag && !tags.includes(inputTag)) {
      setTags([...tags, inputTag])
      setInputTag('')
    }
  }

  const handleRemoveTag = (removedTag: string) => {
    setTags(tags.filter(tag => tag !== removedTag))
  }

  return (
    <Modal
      title={type === 'definition' ? '创建需求定义' : '创建需求使用'}
      open={open}
      onOk={handleSubmit}
      onCancel={onClose}
      confirmLoading={loading}
      width={600}
      okText={loading ? "创建中..." : "创建"}
      cancelText="取消"
    >
      <Form
        form={form}
        layout="vertical"
        autoComplete="off"
      >
        {type === 'definition' && (
          <Form.Item
            label="需求ID *"
            name="reqId"
            rules={[
              { required: true, message: '请输入需求ID' },
              { pattern: /^REQ-[\w-]+$/, message: '需求ID格式不正确，应以REQ-开头' }
            ]}
            extra="唯一标识符，格式：REQ-XXX"
          >
            <Input placeholder="例如：REQ-001" />
          </Form.Item>
        )}

        <Form.Item
          label="简称"
          name="declaredShortName"
          rules={[{ max: 50, message: '简称最多50个字符' }]}
        >
          <Input placeholder="需求的简短名称" />
        </Form.Item>

        <Form.Item
          label="需求名称 *"
          name="declaredName"
          rules={[
            { required: true, message: '请输入需求名称' },
            { max: 200, message: '名称最多200个字符' }
          ]}
        >
          <Input placeholder="需求的完整名称" />
        </Form.Item>

        <Form.Item
          label="需求文档 *"
          name="documentation"
          rules={[
            { required: true, message: '请输入需求文档' },
            { min: 10, message: '需求文档至少需要10个字符' },
            { max: 2000, message: '需求文档最多2000个字符' }
          ]}
        >
          <TextArea
            rows={4}
            placeholder={
              type === 'definition' 
                ? "需求的详细描述，可使用模板变量如：${responseTime}" 
                : "需求使用的具体描述"
            }
          />
        </Form.Item>

        <Form.Item
          label="状态"
          name="status"
          initialValue="draft"
        >
          <Select>
            <Option value="draft">草稿</Option>
            <Option value="approved">已批准</Option>
            <Option value="implemented">已实现</Option>
            <Option value="verified">已验证</Option>
            <Option value="deprecated">已废弃</Option>
          </Select>
        </Form.Item>

        <Form.Item
          label="优先级"
          name="priority"
          initialValue="P2"
        >
          <Select>
            <Option value="P0">P0 - 紧急</Option>
            <Option value="P1">P1 - 高</Option>
            <Option value="P2">P2 - 中</Option>
            <Option value="P3">P3 - 低</Option>
          </Select>
        </Form.Item>

        <Form.Item
          label="验证方法"
          name="verificationMethod"
          initialValue="test"
        >
          <Select>
            <Option value="test">测试</Option>
            <Option value="analysis">分析</Option>
            <Option value="inspection">检查</Option>
            <Option value="demonstration">演示</Option>
          </Select>
        </Form.Item>

        {type === 'definition' && (
          <Form.Item
            label="是否为模板"
            name="isAbstract"
            valuePropName="checked"
            extra="模板可用于创建多个需求实例"
          >
            <Switch />
          </Form.Item>
        )}

        {type === 'usage' && (
          <>
            <Form.Item
              label="基于定义 *"
              name="definitionId"
              rules={[{ required: true, message: '请选择基于的定义' }]}
              initialValue={definitionId}
              extra="RequirementUsage必须基于Definition创建"
            >
              {definitionId ? (
                <Input value={definitionId} disabled />
              ) : (
                <Select 
                  placeholder="选择一个需求定义模板"
                  loading={loadingDefinitions}
                  showSearch
                  optionFilterProp="children"
                >
                  {definitions.map(def => (
                    <Option key={def.elementId} value={def.elementId}>
                      {def.declaredName || def.reqId || def.elementId}
                    </Option>
                  ))}
                </Select>
              )}
            </Form.Item>
          </>
        )}


        <Form.Item
          label="标签"
          extra="按回车添加标签"
        >
          <Space direction="vertical" style={{ width: '100%' }}>
            <Input
              placeholder="输入标签后按回车"
              value={inputTag}
              onChange={e => setInputTag(e.target.value)}
              onPressEnter={handleAddTag}
            />
            <div>
              {tags.map(tag => (
                <Tag
                  key={tag}
                  closable
                  onClose={() => handleRemoveTag(tag)}
                  style={{ marginBottom: 8 }}
                >
                  {tag}
                </Tag>
              ))}
            </div>
          </Space>
        </Form.Item>
      </Form>
    </Modal>
  )
}

export default CreateRequirementDialog