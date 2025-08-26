/**
 * 错误边界组件 - TDD第五阶段
 * 
 * 实现统一的错误处理边界，捕获组件渲染错误、API错误等
 * 提供用户友好的错误展示和恢复机制
 */

import React, { Component, ErrorInfo, ReactNode } from 'react'
import { Button, Alert, Card, Collapse, Typography } from 'antd'
import { 
  ExclamationCircleOutlined, 
  ReloadOutlined, 
  WifiOutlined 
} from '@ant-design/icons'
import { AdvancedQueryError } from '../../services/advancedQueryApi'

const { Text, Paragraph } = Typography
const { Panel } = Collapse

// 错误边界属性接口
interface ErrorBoundaryProps {
  children: ReactNode
  fallback?: (props: { error: Error; resetError: () => void }) => ReactNode
  maxRetries?: number
  retryDelay?: number
  maxErrors?: number
  resetTimeout?: number
  showDetails?: boolean
  onErrorFeedback?: (feedback: ErrorFeedback) => void
}

// 错误反馈信息
interface ErrorFeedback {
  error: Error
  errorInfo: ErrorInfo
  userAgent: string
  timestamp: string
}

// 错误边界状态
interface ErrorBoundaryState {
  hasError: boolean
  error: Error | null
  errorInfo: ErrorInfo | null
  retryCount: number
  errorCount: number
  isRetrying: boolean
  showDetails: boolean
}

/**
 * 错误边界组件类
 */
class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  private retryTimer: NodeJS.Timeout | null = null
  private resetTimer: NodeJS.Timeout | null = null

  constructor(props: ErrorBoundaryProps) {
    super(props)
    
    this.state = {
      hasError: false,
      error: null,
      errorInfo: null,
      retryCount: 0,
      errorCount: 0,
      isRetrying: false,
      showDetails: false
    }
  }

  // React错误边界生命周期
  static getDerivedStateFromError(error: Error): Partial<ErrorBoundaryState> {
    return {
      hasError: true,
      error,
      errorCount: 0 // 将通过componentDidCatch更新
    }
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('ErrorBoundary caught an error:', error, errorInfo)
    
    const newErrorCount = this.state.errorCount + 1
    
    this.setState({
      errorInfo,
      errorCount: newErrorCount
    })

    // 如果错误次数超过限制，停止渲染
    if (newErrorCount >= (this.props.maxErrors || 10)) {
      return
    }

    // 自动重试逻辑
    const maxRetries = this.props.maxRetries || 0
    if (maxRetries > 0 && this.state.retryCount < maxRetries) {
      this.setState({ isRetrying: true })
      this.retryTimer = setTimeout(() => {
        this.setState(prevState => ({
          hasError: false,
          error: null,
          errorInfo: null,
          retryCount: prevState.retryCount + 1,
          isRetrying: false
        }))
      }, this.props.retryDelay || 1000)
    }

    // 设置重置定时器
    if (this.props.resetTimeout) {
      this.resetTimer = setTimeout(() => {
        this.resetError()
      }, this.props.resetTimeout)
    }
  }

  componentWillUnmount() {
    if (this.retryTimer) {
      clearTimeout(this.retryTimer)
    }
    if (this.resetTimer) {
      clearTimeout(this.resetTimer)
    }
  }

  // 重置错误状态
  resetError = () => {
    this.setState({
      hasError: false,
      error: null,
      errorInfo: null,
      retryCount: 0,
      errorCount: 0,
      isRetrying: false,
      showDetails: false
    })
  }

  // 切换错误详情显示
  toggleDetails = () => {
    this.setState(prevState => ({
      showDetails: !prevState.showDetails
    }))
  }

  // 发送错误反馈
  sendFeedback = () => {
    if (this.props.onErrorFeedback && this.state.error && this.state.errorInfo) {
      const feedback: ErrorFeedback = {
        error: this.state.error,
        errorInfo: this.state.errorInfo,
        userAgent: navigator.userAgent,
        timestamp: new Date().toISOString()
      }
      this.props.onErrorFeedback(feedback)
    }
  }

  // 获取错误类型
  getErrorType(): 'network' | 'api' | 'auth' | 'permission' | 'generic' {
    const error = this.state.error
    
    if (error instanceof AdvancedQueryError) {
      if (error.statusCode === 0 || error.message.includes('Network Error')) {
        return 'network'
      }
      if (error.statusCode === 401) {
        return 'auth'
      }
      if (error.statusCode === 403) {
        return 'permission'
      }
      return 'api'
    }
    
    return 'generic'
  }

  // 渲染错误UI
  renderErrorUI() {
    const { error, errorInfo, errorCount, isRetrying, showDetails } = this.state
    const { maxErrors = 10 } = this.props
    
    // 错误过多，停止渲染
    if (errorCount >= maxErrors) {
      return (
        <Card className="error-boundary-card">
          <Alert
            message="错误过多，已停止渲染"
            description="系统检测到过多错误，已停止渲染以保护性能。请刷新页面重试。"
            type="error"
            showIcon
          />
          <Button type="primary" onClick={() => window.location.reload()} className="mt-4">
            刷新页面
          </Button>
        </Card>
      )
    }

    // 正在重试
    if (isRetrying) {
      return (
        <Card className="error-boundary-card">
          <Alert
            message="正在重试..."
            description="系统正在自动恢复，请稍候。"
            type="info"
            showIcon
          />
        </Card>
      )
    }

    const errorType = this.getErrorType()

    // 自定义fallback组件
    if (this.props.fallback && error) {
      return this.props.fallback({ error, resetError: this.resetError })
    }

    // 网络错误
    if (errorType === 'network') {
      return (
        <Card className="error-boundary-card">
          <Alert
            message="网络连接失败"
            description="请检查您的网络连接并重试"
            type="error"
            showIcon
            icon={<WifiOutlined  />}
          />
          <div className="mt-4 space-x-2">
            <Button type="primary" onClick={this.resetError} icon={<ReloadOutlined  />}>
              重新连接
            </Button>
            {this.props.onErrorFeedback && (
              <Button onClick={this.sendFeedback}>
                报告错误
              </Button>
            )}
          </div>
        </Card>
      )
    }

    // 认证错误
    if (errorType === 'auth') {
      return (
        <Card className="error-boundary-card">
          <Alert
            message="认证失败"
            description="请重新登录"
            type="warning"
            showIcon
          />
          <div className="mt-4">
            <Button type="primary" onClick={() => window.location.href = '/login'}>
              重新登录
            </Button>
          </div>
        </Card>
      )
    }

    // 权限错误
    if (errorType === 'permission') {
      return (
        <Card className="error-boundary-card">
          <Alert
            message="权限不足"
            description="您没有执行此操作的权限"
            type="warning"
            showIcon
          />
          <div className="mt-4">
            <Button onClick={this.resetError}>
              返回
            </Button>
          </div>
        </Card>
      )
    }

    // API错误
    if (errorType === 'api' && error instanceof AdvancedQueryError) {
      return (
        <Card className="error-boundary-card">
          <Alert
            message="API请求失败"
            description={
              <div>
                <Text>{error.message}</Text>
                <br />
                <Text type="secondary">状态码: {error.statusCode}</Text>
              </div>
            }
            type="error"
            showIcon
            icon={<ExclamationCircleOutlined  />}
          />
          <div className="mt-4 space-x-2">
            <Button type="primary" onClick={this.resetError} icon={<ReloadOutlined  />}>
              重新加载页面
            </Button>
            {this.props.onErrorFeedback && (
              <Button onClick={this.sendFeedback}>
                报告错误
              </Button>
            )}
          </div>
        </Card>
      )
    }

    // 通用错误
    return (
      <Card className="error-boundary-card">
        <Alert
          message="系统遇到了问题"
          description={error?.message || '未知错误'}
          type="error"
          showIcon
          icon={<ExclamationCircleOutlined  />}
        />
        
        <div className="mt-4 space-x-2">
          <Button type="primary" onClick={this.resetError} icon={<ReloadOutlined  />}>
            重新加载页面
          </Button>
          {this.props.showDetails && (
            <Button onClick={this.toggleDetails}>
              {showDetails ? '隐藏详情' : '显示详情'}
            </Button>
          )}
          {this.props.onErrorFeedback && (
            <Button onClick={this.sendFeedback}>
              报告错误
            </Button>
          )}
        </div>

        {this.props.showDetails && showDetails && (
          <div className="mt-4">
            <Collapse>
              <Panel header="错误详情" key="details">
                <div className="space-y-2">
                  <div>
                    <Text strong>错误信息:</Text>
                    <Paragraph code>{error?.message}</Paragraph>
                  </div>
                  <div>
                    <Text strong>错误栈:</Text>
                    <Paragraph code style={{ whiteSpace: 'pre-wrap', fontSize: '12px' }}>
                      {error?.stack}
                    </Paragraph>
                  </div>
                  {errorInfo?.componentStack && (
                    <div>
                      <Text strong>组件栈:</Text>
                      <Paragraph code style={{ whiteSpace: 'pre-wrap', fontSize: '12px' }}>
                        {errorInfo.componentStack}
                      </Paragraph>
                    </div>
                  )}
                </div>
              </Panel>
            </Collapse>
          </div>
        )}
      </Card>
    )
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="error-boundary-wrapper p-4">
          {this.renderErrorUI()}
        </div>
      )
    }

    return this.props.children
  }
}

export default ErrorBoundary

// 导出类型供其他组件使用
export type { ErrorBoundaryProps, ErrorFeedback }