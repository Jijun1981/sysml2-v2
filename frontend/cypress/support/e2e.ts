/**
 * Cypress E2E测试支持文件
 */

// 导入Cypress命令
import './commands'

// 性能测试辅助
Cypress.on('window:before:load', (win) => {
  // 添加性能标记
  win.performance.mark('cypress-start')
})

// 未捕获异常处理
Cypress.on('uncaught:exception', (err, runnable) => {
  // 忽略ResizeObserver相关错误（Ant Design组件常见）
  if (err.message.includes('ResizeObserver')) {
    return false
  }
  // 忽略React开发模式警告
  if (err.message.includes('Warning:')) {
    return false
  }
  return true
})

// 添加测试前后钩子
beforeEach(() => {
  // 清理localStorage
  cy.window().then((win) => {
    win.localStorage.clear()
    win.sessionStorage.clear()
  })
  
  // 记录测试开始时间
  cy.wrap(Date.now()).as('testStartTime')
})

afterEach(() => {
  // 记录测试结束时间并计算耗时
  cy.get('@testStartTime').then((startTime) => {
    const duration = Date.now() - (startTime as number)
    cy.log(`Test duration: ${duration}ms`)
    
    // 如果测试耗时超过3秒，发出警告
    if (duration > 3000) {
      cy.log(`⚠️ Test took longer than 3 seconds: ${duration}ms`)
    }
  })
})