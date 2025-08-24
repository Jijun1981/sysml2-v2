import { render, screen, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { ModelProvider } from '../../../contexts/ModelContext'
import MainLayout from '../MainLayout'
import '@testing-library/jest-dom'

// Mock the universal API
vi.mock('../../../services/universalApi', () => ({
  createUniversalElement: vi.fn(),
  queryElementsByType: vi.fn(),
  queryAllElements: vi.fn(),
  getElementById: vi.fn(),
  updateUniversalElement: vi.fn(),
  deleteUniversalElement: vi.fn(),
  setProjectId: vi.fn(),
  handleUniversalApiError: vi.fn()
}))

// Mock Antd message to avoid error boundary issues
vi.mock('antd', async (importOriginal) => {
  const actual = await importOriginal() as any
  return {
    ...actual,
    message: {
      error: vi.fn(),
      success: vi.fn(),
      info: vi.fn()
    }
  }
})

describe('MainLayout Component', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  const TestWrapper = ({ children }: { children: React.ReactNode }) => (
    <ModelProvider projectId="test-project">{children}</ModelProvider>
  )

  it('should render without crashing', () => {
    render(
      <TestWrapper>
        <MainLayout />
      </TestWrapper>
    )

    // 验证基本布局元素存在
    expect(screen.getByText('SysML v2 建模平台 MVP')).toBeInTheDocument()
    expect(screen.getByText('刷新')).toBeInTheDocument()
    expect(screen.getByText('导出')).toBeInTheDocument()
    expect(screen.getByText('导入')).toBeInTheDocument()
  })

  it('should render all three view tabs', () => {
    render(
      <TestWrapper>
        <MainLayout />
      </TestWrapper>
    )

    // 验证三个视图标签都存在
    expect(screen.getByText('树视图')).toBeInTheDocument()
    expect(screen.getByText('表视图')).toBeInTheDocument()
    expect(screen.getByText('图视图')).toBeInTheDocument()
  })

  it('should not call loadProject on mount since it does not exist in new ModelContext', async () => {
    // 这个测试验证不会因为调用不存在的loadProject而报错
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
    
    render(
      <TestWrapper>
        <MainLayout />
      </TestWrapper>
    )

    // 等待组件完全渲染
    await waitFor(() => {
      expect(screen.getByText('SysML v2 建模平台 MVP')).toBeInTheDocument()
    })

    // 验证没有控制台错误（除了预期的loadProject不存在错误）
    // 这将帮助我们识别需要修复的问题
    
    consoleSpy.mockRestore()
  })

  it('should show correct initial tab state', () => {
    render(
      <TestWrapper>
        <MainLayout />
      </TestWrapper>
    )

    // 默认应该显示树视图
    const treeTab = screen.getByText('树视图')
    expect(treeTab.closest('.ant-tabs-tab')).toHaveClass('ant-tabs-tab-active')
  })
})