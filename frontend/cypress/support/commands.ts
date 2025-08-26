/**
 * Cypress自定义命令
 */

/// <reference types="cypress" />

declare namespace Cypress {
  interface Chainable {
    /**
     * 等待应用加载完成
     */
    waitForApp(): Chainable<void>
    
    /**
     * 创建测试需求
     */
    createRequirement(data: {
      name: string
      reqId: string
      text?: string
    }): Chainable<void>
    
    /**
     * 选中树视图节点
     */
    selectTreeNode(nodeId: string): Chainable<void>
    
    /**
     * 选中表格行
     */
    selectTableRow(rowId: string): Chainable<void>
    
    /**
     * 选中图视图节点
     */
    selectGraphNode(nodeId: string): Chainable<void>
    
    /**
     * 验证性能指标
     */
    checkPerformance(metrics: {
      maxRenderTime?: number
      maxResponseTime?: number
    }): Chainable<void>
    
    /**
     * 批量创建测试数据
     */
    createBulkRequirements(count: number): Chainable<void>
  }
}

// 等待应用加载
Cypress.Commands.add('waitForApp', () => {
  cy.get('[data-testid="app-container"]', { timeout: 10000 }).should('be.visible')
  cy.wait(500) // 等待初始渲染完成
})

// 创建需求
Cypress.Commands.add('createRequirement', (data) => {
  cy.log(`Creating requirement: ${data.name}`)
  
  // 通过API创建
  cy.request({
    method: 'POST',
    url: 'http://localhost:8080/api/v1/requirements',
    body: {
      reqId: data.reqId,
      name: data.name,
      text: data.text || `Description for ${data.name}`,
      eClass: 'RequirementDefinition'
    }
  }).then((response) => {
    expect(response.status).to.eq(201)
  })
})

// 选中树节点
Cypress.Commands.add('selectTreeNode', (nodeId) => {
  cy.get(`[data-testid="tree-node-${nodeId}"]`)
    .should('be.visible')
    .click()
  
  // 验证节点被选中
  cy.get(`[data-testid="tree-node-${nodeId}"]`)
    .should('have.class', 'ant-tree-node-selected')
})

// 选中表格行
Cypress.Commands.add('selectTableRow', (rowId) => {
  cy.get(`[data-row-key="${rowId}"]`)
    .should('be.visible')
    .find('.ant-table-cell')
    .first()
    .click()
  
  // 验证行被选中
  cy.get(`[data-row-key="${rowId}"]`)
    .should('have.class', 'ant-table-row-selected')
})

// 选中图节点
Cypress.Commands.add('selectGraphNode', (nodeId) => {
  cy.get(`[data-id="${nodeId}"]`)
    .should('be.visible')
    .click()
  
  // 验证节点被选中
  cy.get(`[data-id="${nodeId}"]`)
    .should('have.class', 'selected')
})

// 性能检查
Cypress.Commands.add('checkPerformance', (metrics) => {
  cy.window().then((win) => {
    const perf = win.performance
    const entries = perf.getEntriesByType('navigation')[0] as PerformanceNavigationTiming
    
    // 检查页面加载时间
    const loadTime = entries.loadEventEnd - entries.fetchStart
    cy.log(`Page load time: ${loadTime}ms`)
    
    if (metrics.maxRenderTime && loadTime > metrics.maxRenderTime) {
      throw new Error(`Page load time ${loadTime}ms exceeds limit ${metrics.maxRenderTime}ms`)
    }
    
    // 检查响应时间
    const responseTime = entries.responseEnd - entries.requestStart
    cy.log(`Response time: ${responseTime}ms`)
    
    if (metrics.maxResponseTime && responseTime > metrics.maxResponseTime) {
      throw new Error(`Response time ${responseTime}ms exceeds limit ${metrics.maxResponseTime}ms`)
    }
    
    // 记录性能指标
    cy.task('recordPerformance', {
      loadTime,
      responseTime,
      domContentLoaded: entries.domContentLoadedEventEnd - entries.domContentLoadedEventStart,
      domInteractive: entries.domInteractive - entries.fetchStart
    })
  })
})

// 批量创建数据
Cypress.Commands.add('createBulkRequirements', (count) => {
  cy.log(`Creating ${count} requirements`)
  
  const requirements = Array.from({ length: count }, (_, i) => ({
    reqId: `TEST-${i.toString().padStart(3, '0')}`,
    name: `Test Requirement ${i + 1}`,
    text: `This is test requirement number ${i + 1}`,
    eClass: 'RequirementDefinition',
    status: i % 3 === 0 ? 'approved' : i % 3 === 1 ? 'draft' : 'rejected'
  }))
  
  // 批量发送请求
  requirements.forEach((req) => {
    cy.request({
      method: 'POST',
      url: 'http://localhost:8080/api/v1/requirements',
      body: req,
      failOnStatusCode: false
    })
  })
  
  // 等待数据加载
  cy.wait(1000)
  cy.reload()
})

export {}