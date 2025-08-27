import React, { useState } from 'react'
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

const { TextArea } = Input
const { Option } = Select

interface CreateRequirementDialogProps {
  open: boolean
  onClose: () => void
  onSuccess: () => void
  type?: 'definition' | 'usage'
  templateId?: string // 如果是从模板创建
}

const CreateRequirementDialog: React.FC<CreateRequirementDialogProps> = ({
  open,
  onClose,
  onSuccess,
  type = 'definition',
  templateId
}) => {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [tags, setTags] = useState<string[]>([])
  const [inputTag, setInputTag] = useState('')

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      setLoading(true)

      const data = {
        ...values,
        tags: tags.length > 0 ? tags : undefined,
        isAbstract: values.isAbstract || false
      }

      if (type === 'definition') {
        await requirementService.createRequirementDefinition(data)
        message.success('需求定义创建成功')
      } else {
        await requirementService.createRequirementUsage(data)
        message.success('需求使用创建成功')
      }

      form.resetFields()
      setTags([])
      onSuccess()
      onClose()
    } catch (error: any) {
      if (error.response?.status === 409) {
        message.error('reqId已存在，请使用其他ID')
      } else {
        message.error('创建失败：' + (error.message || '未知错误'))
      }
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
      okText="创建"
      cancelText="取消"
    >
      <Form
        form={form}
        layout="vertical"
        autoComplete="off"
      >
        {type === 'definition' && (
          <Form.Item
            label="需求ID (reqId)"
            name="reqId"
            rules={[
              { required: true, message: '请输入需求ID' },
              { pattern: /^REQ-[\w-]+$/, message: 'ID格式应为REQ-开头' }
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
          label="名称"
          name="declaredName"
          rules={[
            { required: true, message: '请输入需求名称' },
            { max: 200, message: '名称最多200个字符' }
          ]}
        >
          <Input placeholder="需求的完整名称" />
        </Form.Item>

        <Form.Item
          label="需求描述"
          name="text"
          rules={[
            { required: true, message: '请输入需求描述' },
            { max: 2000, message: '描述最多2000个字符' }
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

        {type === 'usage' && templateId && (
          <Form.Item
            label="基于模板"
            extra="此需求使用基于选定的模板创建"
          >
            <Input value={templateId} disabled />
          </Form.Item>
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