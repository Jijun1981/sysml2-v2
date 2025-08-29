/**
 * 错误处理工具类 - REQ-F1-4
 * 提供统一的错误处理和用户友好的错误消息
 */

import { message } from 'antd'

interface ApiError {
  response?: {
    status: number
    data?: {
      message?: string
      errors?: Record<string, string>
    }
  }
  code?: string
  message?: string
}

class ErrorHandler {
  /**
   * 判断是否为网络错误
   */
  isNetworkError(error: any): boolean {
    return (
      error.message === 'Network Error' ||
      error.code === 'ERR_NETWORK' ||
      error.code === 'ERR_INTERNET_DISCONNECTED'
    )
  }

  /**
   * 判断是否为超时错误
   */
  isTimeoutError(error: any): boolean {
    return (
      error.code === 'ECONNABORTED' ||
      error.message?.includes('timeout') ||
      error.message?.includes('超时')
    )
  }

  /**
   * 判断是否为409冲突错误
   */
  isConflictError(error: any): boolean {
    return error.response?.status === 409
  }

  /**
   * 判断是否为400验证错误
   */
  isValidationError(error: any): boolean {
    return error.response?.status === 400
  }

  /**
   * 格式化验证错误
   */
  formatValidationErrors(errors: Record<string, string>): string {
    const messages = []
    
    for (const [field, error] of Object.entries(errors)) {
      // 处理特定字段的错误消息
      if (field === 'requirementDefinition') {
        messages.push('RequirementUsage必须关联一个RequirementDefinition')
      } else if (field === 'reqId') {
        messages.push('需求ID格式不正确或已存在')
      } else if (field === 'declaredName') {
        messages.push('名称不能为空')
      } else if (field === 'documentation') {
        messages.push('文档说明不能为空')
      } else {
        // 默认处理
        messages.push(`${field}: ${error}`)
      }
    }
    
    return messages.join('；')
  }

  /**
   * 获取用户友好的错误消息
   */
  getErrorMessage(error: ApiError): string {
    // 网络错误
    if (this.isNetworkError(error)) {
      return '网络连接失败，请检查网络后重试'
    }

    // 超时错误
    if (this.isTimeoutError(error)) {
      return '请求超时，请稍后重试'
    }

    // API响应错误
    if (error.response) {
      const { status, data } = error.response

      switch (status) {
        case 400:
          // 验证错误
          if (data?.errors) {
            return this.formatValidationErrors(data.errors)
          }
          return data?.message || '请求参数错误，请检查输入'

        case 401:
          return '未授权，请重新登录'

        case 403:
          return '没有权限执行此操作'

        case 404:
          return '请求的资源不存在'

        case 409:
          // 冲突错误（如ID重复）
          if (data?.message?.includes('reqId')) {
            return '需求ID已存在，请使用其他ID'
          }
          return data?.message || '资源冲突，请检查是否重复'

        case 500:
          return data?.message || '服务器内部错误，请稍后重试'

        case 502:
        case 503:
        case 504:
          return '服务暂时不可用，请稍后重试'

        default:
          return data?.message || `请求失败（错误码：${status}）`
      }
    }

    // 其他错误
    return error.message || '操作失败，请重试'
  }

  /**
   * 显示错误消息
   */
  showError(error: ApiError): void {
    const errorMessage = this.getErrorMessage(error)
    message.error(errorMessage)
  }

  /**
   * 处理API错误的统一方法
   */
  handleApiError(error: ApiError, customMessage?: string): void {
    console.error('API Error:', error)
    
    const errorMessage = customMessage || this.getErrorMessage(error)
    
    // 如果是网络错误或超时，显示带重试按钮的消息
    if (this.isNetworkError(error) || this.isTimeoutError(error)) {
      message.error({
        content: errorMessage,
        duration: 5,
        key: 'network-error'
      })
    } else {
      message.error(errorMessage)
    }
  }

  /**
   * 包装异步操作，自动处理错误
   */
  async wrapAsync<T>(
    asyncFn: () => Promise<T>,
    options?: {
      loadingMessage?: string
      successMessage?: string
      errorMessage?: string
    }
  ): Promise<T | null> {
    const loadingKey = 'async-operation'
    
    try {
      if (options?.loadingMessage) {
        message.loading({ content: options.loadingMessage, key: loadingKey })
      }

      const result = await asyncFn()

      if (options?.successMessage) {
        message.success({ content: options.successMessage, key: loadingKey })
      } else if (options?.loadingMessage) {
        message.destroy(loadingKey)
      }

      return result
    } catch (error) {
      message.destroy(loadingKey)
      this.handleApiError(error as ApiError, options?.errorMessage)
      return null
    }
  }
}

// 导出单例
export const errorHandler = new ErrorHandler()

// 导出类型
export type { ApiError }