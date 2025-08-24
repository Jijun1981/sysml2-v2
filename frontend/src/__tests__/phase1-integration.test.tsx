import { describe, test, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ModelProvider, useModelContext } from '../contexts/ModelContext'
import { createUniversalElement, queryElementsByType } from '../services/universalApi'
import '@testing-library/jest-dom'

// Mock only the API calls
vi.mock('../services/universalApi', () => ({
  createUniversalElement: vi.fn(),
  queryElementsByType: vi.fn(),
  queryAllElements: vi.fn(),
  getElementById: vi.fn(),
  updateUniversalElement: vi.fn(),
  deleteUniversalElement: vi.fn(),
  setProjectId: vi.fn(),
  handleUniversalApiError: vi.fn()
}))

const mockCreateElement = vi.mocked(createUniversalElement)
const mockQueryElements = vi.mocked(queryElementsByType)

// Test component to interact with ModelContext
const TestComponent = () => {
  const { 
    elements, 
    selectedIds, 
    loading, 
    error,
    createElement,
    selectElement,
    getTableViewData
  } = useModelContext()

  return (
    <div data-testid="test-component">
      <div data-testid="elements-count">{Object.keys(elements).length}</div>
      <div data-testid="selected-count">{selectedIds.size}</div>
      <div data-testid="loading">{loading.toString()}</div>
      <div data-testid="error">{error?.message || 'null'}</div>
      
      <button 
        data-testid="create-button"
        onClick={async () => {
          await createElement('RequirementDefinition', {
            declaredShortName: 'TEST-001',
            declaredName: '测试需求'
          })
        }}
      >
        创建需求
      </button>
      
      <button 
        data-testid="select-button"
        onClick={() => selectElement('test-id')}
      >
        选择元素
      </button>
      
      <div data-testid="table-data">{JSON.stringify(getTableViewData())}</div>
    </div>
  )
}

describe('Phase 1 Integration Test - Universal API + SSOT', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  const TestWrapper = ({ children }: { children: React.ReactNode }) => (
    <ModelProvider projectId="test-project">{children}</ModelProvider>
  )

  test('should initialize ModelContext with universal API integration', () => {
    render(
      <TestWrapper>
        <TestComponent />
      </TestWrapper>
    )

    // Verify initial state
    expect(screen.getByTestId('elements-count')).toHaveTextContent('0')
    expect(screen.getByTestId('selected-count')).toHaveTextContent('0')
    expect(screen.getByTestId('loading')).toHaveTextContent('false')
    expect(screen.getByTestId('error')).toHaveTextContent('null')
  })

  test('should create element through universal API and update SSOT', async () => {
    const mockElement = {
      id: 'def-001',
      eClass: 'RequirementDefinition',
      attributes: {
        declaredShortName: 'TEST-001',
        declaredName: '测试需求'
      }
    }

    mockCreateElement.mockResolvedValueOnce(mockElement)

    const user = userEvent.setup()
    render(
      <TestWrapper>
        <TestComponent />
      </TestWrapper>
    )

    // Click create button
    await user.click(screen.getByTestId('create-button'))

    // Verify API was called
    await waitFor(() => {
      expect(mockCreateElement).toHaveBeenCalledWith('RequirementDefinition', {
        declaredShortName: 'TEST-001',
        declaredName: '测试需求'
      })
    })

    // Verify SSOT was updated
    await waitFor(() => {
      expect(screen.getByTestId('elements-count')).toHaveTextContent('1')
    })
  })

  test('should manage selection state across views', async () => {
    const user = userEvent.setup()
    render(
      <TestWrapper>
        <TestComponent />
      </TestWrapper>
    )

    // Select an element
    await user.click(screen.getByTestId('select-button'))

    await waitFor(() => {
      expect(screen.getByTestId('selected-count')).toHaveTextContent('1')
    })
  })

  test('should provide table view data projection', () => {
    const initialElements = {
      'def-001': {
        id: 'def-001',
        eClass: 'RequirementDefinition',
        attributes: {
          declaredShortName: 'REQ-001',
          declaredName: '需求1'
        }
      }
    }

    render(
      <ModelProvider projectId="test-project" initialElements={initialElements}>
        <TestComponent />
      </ModelProvider>
    )

    const tableDataText = screen.getByTestId('table-data').textContent
    expect(tableDataText).toContain('REQ-001')
    expect(tableDataText).toContain('需求1')
  })

  test('should handle loading and error states', async () => {
    mockCreateElement.mockRejectedValueOnce(new Error('API Error'))

    const user = userEvent.setup()
    render(
      <TestWrapper>
        <TestComponent />
      </TestWrapper>
    )

    // Trigger error
    await user.click(screen.getByTestId('create-button'))

    await waitFor(() => {
      expect(screen.getByTestId('error')).toHaveTextContent('创建元素失败')
    })
  })

  test('should verify universal API integration requirements', () => {
    // REQ-A1-1: Single Source of Truth 
    // REQ-D0-1: Universal element interface
    // REQ-D1-3: Three-view synchronization
    
    const initialElements = {
      'def-001': {
        id: 'def-001', 
        eClass: 'RequirementDefinition',
        attributes: { declaredName: '定义1' }
      },
      'usage-001': {
        id: 'usage-001',
        eClass: 'RequirementUsage', 
        attributes: { declaredName: '使用1', of: 'def-001' }
      }
    }

    render(
      <ModelProvider projectId="test-project" initialElements={initialElements}>
        <TestComponent />
      </ModelProvider>  
    )

    // Verify SSOT contains both elements
    expect(screen.getByTestId('elements-count')).toHaveTextContent('2')
    
    // Verify table projection includes both
    const tableData = screen.getByTestId('table-data').textContent
    expect(tableData).toContain('定义1')
    expect(tableData).toContain('使用1')
  })
})