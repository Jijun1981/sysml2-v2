import React, { useState, useEffect, useCallback } from 'react';
import { 
  Modal, 
  Form, 
  Input, 
  Select, 
  Button, 
  message, 
  Spin,
  Typography 
} from 'antd';

const { TextArea } = Input;
const { Title } = Typography;

import { requirementService } from '../services/requirementService';
import { useModelContext } from '../contexts/ModelContext';
import { ElementDTO } from '../types';

interface EditDialogProps {
  visible: boolean;
  element: ElementDTO | null;
  onClose: () => void;
  onSave: (updatedElement: ElementDTO) => void;
}

export const EditDialog: React.FC<EditDialogProps> = ({
  visible,
  element,
  onClose,
  onSave
}) => {
  const { updateElement } = useModelContext(); // 使用ModelContext的updateElement
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [definitions, setDefinitions] = useState<ElementDTO[]>([]);
  const [hasChanges, setHasChanges] = useState(false);

  // 获取元素类型
  const getElementType = () => {
    if (!element) return '';
    const eClass = element.eClass || '';
    return eClass.includes('RequirementUsage') ? 'RequirementUsage' : 
           eClass.includes('RequirementDefinition') ? 'RequirementDefinition' : 
           'Element';
  };

  // 加载可用的Definition列表
  useEffect(() => {
    if (visible && getElementType() === 'RequirementUsage') {
      loadDefinitions();
    }
  }, [visible]);

  // 初始化表单数据
  useEffect(() => {
    if (visible && element) {
      const formData: any = {
        elementId: element.elementId,
        declaredName: element.declaredName || element.properties?.declaredName || '',
        declaredShortName: element.declaredShortName || element.properties?.declaredShortName || '',
      };

      // RequirementUsage特有字段
      if (getElementType() === 'RequirementUsage') {
        formData.requirementDefinition = 
          element.requirementDefinition || 
          element.properties?.requirementDefinition || 
          '';
      }

      // RequirementDefinition特有字段
      if (getElementType() === 'RequirementDefinition') {
        formData.reqId = element.reqId || element.properties?.reqId || '';
      }

      // declaredName已经在上面设置，用作描述字段
      // REQ-TEXT-SIMPLE-001-1: 使用标准字段

      form.setFieldsValue(formData);
      setHasChanges(false);
    }
  }, [visible, element, form]);

  // 加载Definition列表
  const loadDefinitions = async () => {
    setLoading(true);
    try {
      const data = await requirementService.getDefinitions();
      // 去重：按elementId去重
      const uniqueData = data.filter((item: ElementDTO, index: number, self: ElementDTO[]) =>
        index === self.findIndex((t) => t.elementId === item.elementId)
      );
      setDefinitions(uniqueData);
    } catch (error) {
      message.error('加载Definition列表失败');
      console.error('Load definitions error:', error);
    } finally {
      setLoading(false);
    }
  };

  // 处理表单值变化
  const handleFormChange = () => {
    setHasChanges(true);
  };

  // 处理保存
  const handleSave = async () => {
    // 防止重复提交
    if (saving) {
      console.log('EditDialog: 正在保存中，忽略重复提交');
      return;
    }

    try {
      const values = await form.validateFields();
      setSaving(true);

      console.log('EditDialog: 开始保存流程');
      
      // 准备更新数据
      // REQ-TEXT-SIMPLE-001-1: 使用declaredName作为描述
      const updateData: any = {
        declaredName: values.declaredName,  // 用作描述文本
        declaredShortName: values.declaredShortName,  // 用作简短名称
      };

      // 根据类型添加特定字段
      if (getElementType() === 'RequirementUsage') {
        updateData.requirementDefinition = values.requirementDefinition || null;
      }
      if (getElementType() === 'RequirementDefinition') {
        updateData.reqId = values.reqId;
      }

      // 使用ModelContext的updateElement（会自动更新本地状态）
      const elementId = element!.elementId || element!.id;
      console.log('EditDialog: 准备更新，原始element:', element);
      console.log('EditDialog: 使用的ID:', elementId);
      console.log('EditDialog: 更新数据:', updateData);
      
      const updatedElement = await updateElement(elementId, updateData);
      console.log('EditDialog: updateElement返回:', updatedElement);
      console.log('EditDialog: 返回的ID:', updatedElement.id);

      message.success('保存成功');
      console.log('EditDialog: 显示保存成功消息');
      
      // 调用onSave回调（但不需要在其中刷新数据，因为updateElement已经更新了状态）
      if (onSave) {
        onSave(updatedElement);
      }
      setHasChanges(false);  // 重置修改标记
      form.resetFields();
      onClose();
    } catch (error: any) {
      if (error.errorFields) {
        // 表单验证错误
        return;
      }
      message.error(`保存失败: ${error.message || '未知错误'}`);
    } finally {
      setSaving(false);
    }
  };

  // 处理关闭
  const handleClose = () => {
    if (hasChanges) {
      Modal.confirm({
        title: '确认',
        content: '确定要放弃未保存的修改吗？',
        onOk: () => {
          form.resetFields();
          setHasChanges(false);
          onClose();
        }
      });
    } else {
      form.resetFields();
      onClose();
    }
  };

  // 处理直接关闭（不提示）
  const handleDirectClose = () => {
    form.resetFields();
    setHasChanges(false);
    onClose();
  };

  return (
    <Modal
      title={`编辑 ${getElementType()}`}
      open={visible}
      onCancel={handleClose}
      footer={[
        <Button key="cancel" onClick={handleClose}>
          取消
        </Button>,
        <Button 
          key="save" 
          type="primary" 
          loading={saving}
          onClick={handleSave}
        >
          {saving ? '保存中...' : '保存'}
        </Button>
      ]}
      width={600}
    >
      <Form
        form={form}
        layout="vertical"
        onValuesChange={handleFormChange}
      >
        {/* Element ID - 只读 */}
        <Form.Item
          label="Element ID"
          name="elementId"
        >
          <Input disabled />
        </Form.Item>

        {/* 描述 - 用declaredName存储 */}
        <Form.Item
          label="描述"
          name="declaredName"
          tooltip="使用declaredName字段存储需求描述（简化方案）"
        >
          <TextArea 
            rows={4} 
            placeholder="请输入需求的详细描述"
          />
        </Form.Item>

        {/* 名称/简称 - 必填 */}
        <Form.Item
          label="名称"
          name="declaredShortName"
          rules={[{ required: true, message: '请输入名称' }]}
        >
          <Input placeholder="请输入需求名称" />
        </Form.Item>

        {/* RequirementDefinition特有 - reqId */}
        {getElementType() === 'RequirementDefinition' && (
          <Form.Item
            label="需求ID"
            name="reqId"
            rules={[{ required: true, message: '请输入需求ID' }]}
          >
            <Input placeholder="请输入需求ID" />
          </Form.Item>
        )}

        {/* RequirementUsage特有 - 关联定义 */}
        {getElementType() === 'RequirementUsage' && (
          <Form.Item
            label="关联定义"
            name="requirementDefinition"
            rules={[
              {
                validator: async (_, value) => {
                  if (value && !definitions.find(d => d.elementId === value)) {
                    throw new Error('引用的Definition不存在');
                  }
                }
              }
            ]}
          >
            <Select
              placeholder="选择关联的Definition（可选）"
              allowClear
              showSearch
              loading={loading}
              filterOption={(input, option) =>
                (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
              }
              options={definitions.map(def => ({
                value: def.elementId,
                label: `${def.elementId} - ${def.declaredName || def.properties?.declaredName || '未命名'}`
              }))}
            />
          </Form.Item>
        )}

        {/* 移除旧的documentation字段 */}
      </Form>
    </Modal>
  );
};