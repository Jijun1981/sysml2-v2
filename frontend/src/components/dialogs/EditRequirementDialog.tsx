import React, { useState, useEffect } from 'react'
import {
  Modal,
  Form,
  Input,
  Select,
  Switch,
  Tag,
  Space,
  message,
  Spin
} from 'antd'
import { requirementService } from '../../services/requirementService'

const { TextArea } = Input
const { Option } = Select

interface EditRequirementDialogProps {
  open: boolean
  onClose: () => void
  onSuccess: () => void
  requirementId: string
  type?: 'definition' | 'usage'
}

const EditRequirementDialog: React.FC<EditRequirementDialogProps> = ({
  open,
  onClose,
  onSuccess,
  requirementId,
  type = 'definition'
}) => {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [fetching, setFetching] = useState(false)
  const [tags, setTags] = useState<string[]>([])
  const [inputTag, setInputTag] = useState('')

  // 加载需求数据
  useEffect(() => {
    if (open && requirementId) {
      setFetching(true)
      requirementService.getRequirementById(requirementId)
        .then(data => {
          form.setFieldsValue({
            reqId: data.reqId,
            declaredShortName: data.declaredShortName,
            declaredName: data.declaredName,
            text: data.text,
            status: data.status || 'draft',
            isAbstract: data.isAbstract || false
          })
          setTags(data.tags || [])
        })
        .catch(error => {
          message.error('加载需求数据失败')
          console.error(error)
        })
        .finally(() => {
          setFetching(false)
        })
    }
  }, [open, requirementId, form])

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      setLoading(true)

      const updateData = {
        ...values,
        tags: tags.length > 0 ? tags : undefined
      }

      // 不允许修改reqId
      delete updateData.reqId

      await requirementService.updateRequirement(requirementId, updateData)
      message.success('需求更新成功')

      form.resetFields()
      setTags([])
      onSuccess()
      onClose()
    } catch (error: any) {
      message.error('更新失败：' + (error.message || '未知错误'))
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
      title={type === 'definition' ? '编辑需求定义' : '编辑需求使用'}
      open={open}
      onOk={handleSubmit}
      onCancel={onClose}
      confirmLoading={loading}
      width={600}
      okText="保存"
      cancelText="取消"
    >
      <Spin spinning={fetching}>
        <Form
          form={form}
          layout="vertical"
          autoComplete="off"
        >
          {type === 'definition' && (
            <Form.Item
              label="需求ID (reqId)"
              name="reqId"
              extra="ID创建后不可修改"
            >
              <Input disabled />
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
              placeholder="需求的详细描述"
            />
          </Form.Item>

          <Form.Item
            label="状态"
            name="status"
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
      </Spin>
    </Modal>
  )
}

export default EditRequirementDialog