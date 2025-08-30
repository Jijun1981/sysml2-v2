import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { EditDialog } from '../EditDialog';
import { ModelContextProvider } from '../../contexts/ModelContext';
import { requirementService } from '../../services/requirementService';

// Mock服务
vi.mock('../../services/requirementService', () => ({
  requirementService: {
    getDefinitions: vi.fn(),
    updateUsage: vi.fn(),
    updateDefinition: vi.fn()
  }
}));

describe('EditDialog - REQ-EDIT-001', () => {
  const mockOnClose = vi.fn();
  const mockOnSave = vi.fn();
  
  const mockRequirementUsage = {
    elementId: 'REQ-001',
    declaredName: 'API响应时间',
    declaredShortName: 'API-Response',
    requirementDefinition: 'DEF-PERF',
    eClass: 'sysml:RequirementUsage'
  };

  const mockRequirementDefinitions = [
    { elementId: 'DEF-PERF', declaredName: '性能需求模板' },
    { elementId: 'DEF-SEC', declaredName: '安全需求模板' },
    { elementId: 'DEF-UI', declaredName: '界面需求模板' }
  ];

  beforeEach(() => {
    vi.clearAllMocks();
    // Mock API responses
    vi.mocked(requirementService.getDefinitions).mockResolvedValue(mockRequirementDefinitions);
    vi.mocked(requirementService.updateUsage).mockResolvedValue({ ...mockRequirementUsage });
  });

  describe('REQ-EDIT-001-1: 编辑按钮触发', () => {
    it('应该显示模态对话框', () => {
      render(
        <EditDialog
          visible={true}
          element={mockRequirementUsage}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      );

      expect(screen.getByRole('dialog')).toBeInTheDocument();
      expect(screen.getByText('编辑 RequirementUsage')).toBeInTheDocument();
    });

    it('应该显示元素的所有可编辑属性', () => {
      render(
        <EditDialog
          visible={true}
          element={mockRequirementUsage}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      );

      expect(screen.getByDisplayValue('REQ-001')).toBeDisabled(); // elementId只读
      expect(screen.getByDisplayValue('API响应时间')).toBeInTheDocument();
      expect(screen.getByDisplayValue('API-Response')).toBeInTheDocument();
    });
  });

  describe('REQ-EDIT-001-2: 表单字段动态生成', () => {
    it('RequirementUsage应该显示Definition选择框', async () => {
      render(
        <EditDialog
          visible={true}
          element={mockRequirementUsage}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      );

      await waitFor(() => {
        expect(screen.getByLabelText('关联定义')).toBeInTheDocument();
      });
    });

    it('RequirementDefinition不应该显示Definition选择框', () => {
      const mockDefinition = {
        ...mockRequirementUsage,
        eClass: 'sysml:RequirementDefinition',
        reqId: 'DEF-001'
      };

      render(
        <EditDialog
          visible={true}
          element={mockDefinition}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      );

      expect(screen.queryByLabelText('关联定义')).not.toBeInTheDocument();
      expect(screen.getByLabelText('需求ID')).toBeInTheDocument();
    });
  });

  describe('REQ-EDIT-001-3: RequirementDefinition引用选择', () => {
    it('应该加载并显示所有可用的Definition', async () => {
      render(
        <EditDialog
          visible={true}
          element={mockRequirementUsage}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      );

      const select = screen.getByLabelText('关联定义');
      fireEvent.mouseDown(select);

      await waitFor(() => {
        expect(screen.getByText('DEF-PERF - 性能需求模板')).toBeInTheDocument();
        expect(screen.getByText('DEF-SEC - 安全需求模板')).toBeInTheDocument();
        expect(screen.getByText('DEF-UI - 界面需求模板')).toBeInTheDocument();
      });
    });

    it('应该支持搜索过滤', async () => {
      render(
        <EditDialog
          visible={true}
          element={mockRequirementUsage}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      );

      const select = screen.getByLabelText('关联定义');
      await userEvent.type(select, '性能');

      await waitFor(() => {
        expect(screen.getByText('DEF-PERF - 性能需求模板')).toBeInTheDocument();
        expect(screen.queryByText('DEF-SEC - 安全需求模板')).not.toBeInTheDocument();
      });
    });

    it('应该允许清空选择', async () => {
      render(
        <EditDialog
          visible={true}
          element={mockRequirementUsage}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      );

      const clearButton = screen.getByLabelText('clear');
      fireEvent.click(clearButton);

      await waitFor(() => {
        const select = screen.getByLabelText('关联定义');
        expect(select).toHaveValue('');
      });
    });
  });

  describe('REQ-EDIT-001-4: 表单验证', () => {
    it('应该验证必填字段', async () => {
      render(
        <EditDialog
          visible={true}
          element={mockRequirementUsage}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      );

      const nameInput = screen.getByLabelText('名称');
      await userEvent.clear(nameInput);
      
      const saveButton = screen.getByText('保存');
      fireEvent.click(saveButton);

      await waitFor(() => {
        expect(screen.getByText('请输入名称')).toBeInTheDocument();
      });
    });

    it('应该验证引用的对象存在', async () => {
      render(
        <EditDialog
          visible={true}
          element={mockRequirementUsage}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      );

      // 手动输入不存在的ID
      const select = screen.getByLabelText('关联定义');
      await userEvent.type(select, 'INVALID-ID');
      
      const saveButton = screen.getByText('保存');
      fireEvent.click(saveButton);

      await waitFor(() => {
        expect(screen.getByText('引用的Definition不存在')).toBeInTheDocument();
      });
    });
  });

  describe('REQ-EDIT-001-5: 保存更新', () => {
    it('应该调用API更新数据', async () => {
      render(
        <EditDialog
          visible={true}
          element={mockRequirementUsage}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      );

      const nameInput = screen.getByLabelText('名称');
      await userEvent.clear(nameInput);
      await userEvent.type(nameInput, '新的名称');

      const saveButton = screen.getByText('保存');
      fireEvent.click(saveButton);

      await waitFor(() => {
        expect(requirementService.updateUsage).toHaveBeenCalledWith('REQ-001', {
          declaredName: '新的名称',
          declaredShortName: 'API-Response',
          requirementDefinition: 'DEF-PERF'
        });
      });
    });

    it('应该显示加载状态', async () => {
      vi.mocked(requirementService.updateUsage).mockImplementation(
        () => new Promise(resolve => setTimeout(resolve, 100))
      );

      render(
        <EditDialog
          visible={true}
          element={mockRequirementUsage}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      );

      const saveButton = screen.getByText('保存');
      fireEvent.click(saveButton);

      expect(screen.getByText('保存中...')).toBeInTheDocument();
    });

    it('成功后应该关闭对话框并回调', async () => {
      render(
        <EditDialog
          visible={true}
          element={mockRequirementUsage}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      );

      const saveButton = screen.getByText('保存');
      fireEvent.click(saveButton);

      await waitFor(() => {
        expect(mockOnSave).toHaveBeenCalled();
        expect(mockOnClose).toHaveBeenCalled();
      });
    });
  });

  describe('REQ-EDIT-001-6: 取消操作', () => {
    it('点击取消按钮应该关闭对话框', () => {
      render(
        <EditDialog
          visible={true}
          element={mockRequirementUsage}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      );

      const cancelButton = screen.getByText('取消');
      fireEvent.click(cancelButton);

      expect(mockOnClose).toHaveBeenCalled();
    });

    it('按ESC键应该关闭对话框', () => {
      render(
        <EditDialog
          visible={true}
          element={mockRequirementUsage}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      );

      fireEvent.keyDown(document, { key: 'Escape' });

      expect(mockOnClose).toHaveBeenCalled();
    });

    it('有未保存修改时应该提示确认', async () => {
      render(
        <EditDialog
          visible={true}
          element={mockRequirementUsage}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      );

      const nameInput = screen.getByLabelText('名称');
      await userEvent.type(nameInput, '修改内容');

      const cancelButton = screen.getByText('取消');
      fireEvent.click(cancelButton);

      expect(screen.getByText('确定要放弃未保存的修改吗？')).toBeInTheDocument();
    });
  });

  describe('REQ-EDIT-001-7: 错误处理', () => {
    it('应该显示后端错误消息', async () => {
      vi.mocked(requirementService.updateUsage).mockRejectedValue(
        new Error('网络错误')
      );

      render(
        <EditDialog
          visible={true}
          element={mockRequirementUsage}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      );

      const saveButton = screen.getByText('保存');
      fireEvent.click(saveButton);

      await waitFor(() => {
        expect(screen.getByText('保存失败: 网络错误')).toBeInTheDocument();
      });
    });

    it('错误后应该保持对话框打开', async () => {
      vi.mocked(requirementService.updateUsage).mockRejectedValue(
        new Error('服务器错误')
      );

      render(
        <EditDialog
          visible={true}
          element={mockRequirementUsage}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      );

      const saveButton = screen.getByText('保存');
      fireEvent.click(saveButton);

      await waitFor(() => {
        expect(screen.getByRole('dialog')).toBeInTheDocument();
        expect(mockOnClose).not.toHaveBeenCalled();
      });
    });
  });
});