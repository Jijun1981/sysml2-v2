/**
 * 前端数据流验证测试 - 验证ModelContext和三视图数据同步
 * TDD: 验证前端能正确处理从API获取的数据
 */

import { render } from '@testing-library/react';
import { act } from 'react-dom/test-utils';
import { ModelProvider, useModelContext } from '../contexts/ModelContext';
import React from 'react';

// Mock API调用
vi.mock('../services/universalApi', () => ({
  queryAllElements: vi.fn(() => Promise.resolve({
    data: [
      {
        id: 'req-def-1',
        eClass: 'RequirementDefinition',
        attributes: {
          declaredShortName: 'REQ-001',
          declaredName: '测试需求定义1'
        }
      },
      {
        id: 'req-usage-1',
        eClass: 'RequirementUsage',
        attributes: {
          declaredShortName: 'REQ-001-U1',
          declaredName: '测试需求使用1',
          of: 'req-def-1'
        }
      }
    ]
  })),
  queryElementsByType: vi.fn(() => Promise.resolve({
    data: [
      {
        id: 'req-def-1',
        eClass: 'RequirementDefinition',
        attributes: {
          declaredShortName: 'REQ-001',
          declaredName: '测试需求定义1'
        }
      }
    ]
  })),
  setProjectId: vi.fn()
}));

// 测试组件
const TestComponent: React.FC = () => {
  const { 
    elements, 
    getTreeViewData, 
    getTableViewData, 
    getGraphViewData,
    loadAllElements 
  } = useModelContext();

  return (
    <div>
      <div data-testid="elements-count">{Object.keys(elements).length}</div>
      <div data-testid="tree-data">{JSON.stringify(getTreeViewData())}</div>
      <div data-testid="table-data">{JSON.stringify(getTableViewData())}</div>
      <div data-testid="graph-data">{JSON.stringify(getGraphViewData())}</div>
      <button data-testid="load-data" onClick={() => loadAllElements()}>
        Load Data
      </button>
    </div>
  );
};

describe('前端数据流验证', () => {
  test('ModelContext能正确管理元素数据', async () => {
    const { getByTestId } = render(
      <ModelProvider>
        <TestComponent />
      </ModelProvider>
    );

    // 初始状态：无数据
    expect(getByTestId('elements-count').textContent).toBe('0');

    // 加载数据
    await act(async () => {
      getByTestId('load-data').click();
      // 等待异步操作完成
      await new Promise(resolve => setTimeout(resolve, 100));
    });

    // 验证数据已加载
    expect(getByTestId('elements-count').textContent).toBe('2');
  });

  test('树视图数据投影正确', async () => {
    const { getByTestId } = render(
      <ModelProvider initialElements={{
        'req-def-1': {
          id: 'req-def-1',
          eClass: 'RequirementDefinition',
          attributes: {
            declaredShortName: 'REQ-001',
            declaredName: '测试需求定义1'
          }
        },
        'req-usage-1': {
          id: 'req-usage-1',
          eClass: 'RequirementUsage',
          attributes: {
            declaredShortName: 'REQ-001-U1',
            declaredName: '测试需求使用1',
            of: 'req-def-1'
          }
        }
      }}>
        <TestComponent />
      </ModelProvider>
    );

    const treeData = JSON.parse(getByTestId('tree-data').textContent);
    
    // 验证树结构
    expect(treeData.definitions).toHaveLength(1);
    expect(treeData.definitions[0].id).toBe('req-def-1');
    expect(treeData.definitions[0].label).toBe('测试需求定义1');
    expect(treeData.definitions[0].usages).toHaveLength(1);
    expect(treeData.definitions[0].usages[0].id).toBe('req-usage-1');
  });

  test('表视图数据投影正确', () => {
    const { getByTestId } = render(
      <ModelProvider initialElements={{
        'req-def-1': {
          id: 'req-def-1',
          eClass: 'RequirementDefinition',
          attributes: {
            declaredShortName: 'REQ-001',
            declaredName: '测试需求定义1'
          }
        }
      }}>
        <TestComponent />
      </ModelProvider>
    );

    const tableData = JSON.parse(getByTestId('table-data').textContent);
    
    // 验证表数据结构
    expect(tableData).toHaveLength(1);
    expect(tableData[0].id).toBe('req-def-1');
    expect(tableData[0].eClass).toBe('RequirementDefinition');
    expect(tableData[0].declaredShortName).toBe('REQ-001');
    expect(tableData[0].declaredName).toBe('测试需求定义1');
    expect(tableData[0].status).toBe('active');
  });

  test('图视图数据投影正确', () => {
    const { getByTestId } = render(
      <ModelProvider initialElements={{
        'req-def-1': {
          id: 'req-def-1',
          eClass: 'RequirementDefinition',
          attributes: {
            declaredShortName: 'REQ-001',
            declaredName: '测试需求定义1'
          }
        },
        'req-usage-1': {
          id: 'req-usage-1',
          eClass: 'RequirementUsage',
          attributes: {
            declaredShortName: 'REQ-001-U1',
            declaredName: '测试需求使用1',
            of: 'req-def-1'
          }
        }
      }}>
        <TestComponent />
      </ModelProvider>
    );

    const graphData = JSON.parse(getByTestId('graph-data').textContent);
    
    // 验证图数据结构
    expect(graphData.nodes).toHaveLength(2);
    expect(graphData.edges).toHaveLength(1);
    
    // 验证节点
    const defNode = graphData.nodes.find(n => n.id === 'req-def-1');
    expect(defNode.label).toBe('测试需求定义1');
    expect(defNode.type).toBe('requirementdefinition');

    // 验证边
    expect(graphData.edges[0].source).toBe('req-def-1');
    expect(graphData.edges[0].target).toBe('req-usage-1');
    expect(graphData.edges[0].type).toBe('of');
  });

  test('SSOT原则 - 数据变更同步到所有视图', () => {
    const { getByTestId, rerender } = render(
      <ModelProvider initialElements={{
        'req-def-1': {
          id: 'req-def-1',
          eClass: 'RequirementDefinition',
          attributes: {
            declaredShortName: 'REQ-001',
            declaredName: '测试需求定义1'
          }
        }
      }}>
        <TestComponent />
      </ModelProvider>
    );

    // 初始状态
    let treeData = JSON.parse(getByTestId('tree-data').textContent);
    let tableData = JSON.parse(getByTestId('table-data').textContent);
    let graphData = JSON.parse(getByTestId('graph-data').textContent);

    expect(treeData.definitions).toHaveLength(1);
    expect(tableData).toHaveLength(1);
    expect(graphData.nodes).toHaveLength(1);

    // 重新渲染并添加新数据
    rerender(
      <ModelProvider initialElements={{
        'req-def-1': {
          id: 'req-def-1',
          eClass: 'RequirementDefinition',
          attributes: {
            declaredShortName: 'REQ-001',
            declaredName: '测试需求定义1'
          }
        },
        'req-def-2': {
          id: 'req-def-2',
          eClass: 'RequirementDefinition',
          attributes: {
            declaredShortName: 'REQ-002',
            declaredName: '测试需求定义2'
          }
        }
      }}>
        <TestComponent />
      </ModelProvider>
    );

    // 验证所有视图都同步更新
    treeData = JSON.parse(getByTestId('tree-data').textContent);
    tableData = JSON.parse(getByTestId('table-data').textContent);
    graphData = JSON.parse(getByTestId('graph-data').textContent);

    expect(treeData.definitions).toHaveLength(2);
    expect(tableData).toHaveLength(2);
    expect(graphData.nodes).toHaveLength(2);
  });
});