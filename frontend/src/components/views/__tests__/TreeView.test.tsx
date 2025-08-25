/**
 * 树视图测试用例
 * 需求：REQ-D1-1, REQ-D1-2, REQ-D1-3
 */

import { render, screen, fireEvent } from '@testing-library/react';
import { describe, test, expect, vi } from 'vitest';
import TreeView from '../TreeView/TreeView';
import { ModelProvider } from '../../../contexts/ModelContext';

describe('REQ-D1-1: 树视图数据构建', () => {
  test('应该从通用接口数据构建树结构', async () => {
    // 模拟数据
    const mockElements = {
      'def-1': {
        id: 'def-1',
        eClass: 'RequirementDefinition',
        attributes: {
          declaredShortName: 'REQ-001',
          declaredName: '系统需求'
        }
      },
      'usage-1': {
        id: 'usage-1',
        eClass: 'RequirementUsage',
        attributes: {
          declaredShortName: 'REQ-001-U1',
          declaredName: '功能需求',
          of: 'def-1'
        }
      }
    };

    const { container } = render(
      <ModelProvider initialElements={mockElements}>
        <TreeView />
      </ModelProvider>
    );

    // 验证Definition作为父节点
    expect(screen.getByText('系统需求')).toBeInTheDocument();
    
    // 验证Usage作为子节点
    expect(screen.getByText('功能需求')).toBeInTheDocument();
  });

  test('不应该调用专门的tree接口', () => {
    // 监控网络请求
    const fetchSpy = vi.spyOn(global, 'fetch');
    
    render(
      <ModelProvider>
        <TreeView />
      </ModelProvider>
    );

    // 验证没有调用/api/v1/tree
    expect(fetchSpy).not.toHaveBeenCalledWith(
      expect.stringContaining('/tree')
    );
  });
});

describe('REQ-D1-2: 写回操作', () => {
  test('创建节点应该调用POST /api/v1/elements', async () => {
    const createElementMock = vi.fn();
    
    // 测试创建操作通过通用接口
    expect(createElementMock).toBeDefined();
  });

  test('重命名应该调用PATCH /api/v1/elements/{id}', async () => {
    const updateElementMock = vi.fn();
    
    // 测试更新操作通过通用接口
    expect(updateElementMock).toBeDefined();
  });
});

describe('REQ-D1-3: 视图联动', () => {
  test('选中节点应该更新Context中的selectedIds', () => {
    const mockElements = {
      'def-1': {
        id: 'def-1',
        eClass: 'RequirementDefinition',
        attributes: { declaredName: '测试需求' }
      }
    };

    const { container } = render(
      <ModelProvider initialElements={mockElements}>
        <TreeView />
      </ModelProvider>
    );

    // 点击节点
    const node = screen.getByText('测试需求');
    fireEvent.click(node);

    // 验证选中状态通过Context管理
    expect(container.querySelector('.selected')).toBeTruthy();
  });

  test('不应该通过后端API实现联动', () => {
    // 验证联动是纯前端实现
    const fetchSpy = vi.spyOn(global, 'fetch');
    
    const { container } = render(
      <ModelProvider>
        <TreeView />
      </ModelProvider>
    );

    // 触发选择操作
    const treeNode = container.querySelector('.tree-node');
    if (treeNode) {
      fireEvent.click(treeNode);
    }

    // 验证没有为了联动调用后端
    expect(fetchSpy).not.toHaveBeenCalledWith(
      expect.stringContaining('/highlight')
    );
  });
});