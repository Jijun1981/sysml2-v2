/**
 * 性能基准测试
 */

describe('性能基准测试', () => {
  it('应该在3秒内完成初始加载', () => {
    const startTime = Date.now()
    
    cy.visit('/')
    cy.waitForApp()
    
    cy.then(() => {
      const loadTime = Date.now() - startTime
      cy.log(`初始加载时间: ${loadTime}ms`)
      expect(loadTime).to.be.lessThan(3000)
    })
    
    // 使用Performance API验证
    cy.checkPerformance({
      maxRenderTime: 3000,
      maxResponseTime: 1000
    })
  })
  
  it('应该支持500个节点的渲染', () => {
    cy.visit('/')
    cy.waitForApp()
    
    // 创建500个测试数据
    cy.log('创建500个测试节点...')
    cy.createBulkRequirements(500)
    
    // 记录渲染开始时间
    const startTime = Date.now()
    
    // 切换到树视图
    cy.contains('.ant-tabs-tab', '树视图').click()
    cy.get('[data-testid="tree-view"]').should('be.visible')
    
    // 验证节点数量
    cy.get('.ant-tree-treenode').should('have.length.at.least', 100)
    
    // 切换到表格视图
    cy.contains('.ant-tabs-tab', '表格视图').click()
    cy.get('[data-testid="table-view"]').should('be.visible')
    
    // 验证表格行数（分页后）
    cy.get('.ant-table-row').should('have.length.at.least', 10)
    cy.contains('共 500 条').should('be.visible')
    
    // 切换到图视图
    cy.contains('.ant-tabs-tab', '图视图').click()
    cy.get('[data-testid="graph-view"]').should('be.visible')
    
    // 验证渲染时间
    cy.then(() => {
      const renderTime = Date.now() - startTime
      cy.log(`500节点渲染时间: ${renderTime}ms`)
      expect(renderTime).to.be.lessThan(5000)
    })
  })
  
  it('搜索响应时间应小于500ms', () => {
    cy.visit('/')
    cy.waitForApp()
    
    // 创建测试数据
    cy.createBulkRequirements(100)
    
    // 切换到表格视图
    cy.contains('.ant-tabs-tab', '表格视图').click()
    
    // 记录搜索开始时间
    const startTime = Date.now()
    
    // 执行搜索
    cy.get('input[placeholder="搜索需求..."]')
      .type('Test Requirement 50')
    
    // 等待搜索结果
    cy.contains('Test Requirement 50').should('be.visible')
    
    // 验证响应时间
    cy.then(() => {
      const searchTime = Date.now() - startTime
      cy.log(`搜索响应时间: ${searchTime}ms`)
      expect(searchTime).to.be.lessThan(500)
    })
  })
  
  it('排序操作响应时间应小于300ms', () => {
    cy.visit('/')
    cy.waitForApp()
    
    // 创建测试数据
    cy.createBulkRequirements(50)
    
    // 切换到表格视图
    cy.contains('.ant-tabs-tab', '表格视图').click()
    
    // 记录排序开始时间
    const startTime = Date.now()
    
    // 点击列头排序
    cy.contains('th', '名称').click()
    
    // 等待排序完成
    cy.get('.ant-table-column-sort').should('exist')
    
    // 验证响应时间
    cy.then(() => {
      const sortTime = Date.now() - startTime
      cy.log(`排序响应时间: ${sortTime}ms`)
      expect(sortTime).to.be.lessThan(300)
    })
  })
  
  it('视图切换响应时间应小于200ms', () => {
    cy.visit('/')
    cy.waitForApp()
    
    // 记录切换开始时间
    const startTime = Date.now()
    
    // 快速切换视图
    cy.contains('.ant-tabs-tab', '表格视图').click()
    cy.get('[data-testid="table-view"]').should('be.visible')
    
    cy.contains('.ant-tabs-tab', '图视图').click()
    cy.get('[data-testid="graph-view"]').should('be.visible')
    
    cy.contains('.ant-tabs-tab', '树视图').click()
    cy.get('[data-testid="tree-view"]').should('be.visible')
    
    // 验证响应时间
    cy.then(() => {
      const switchTime = Date.now() - startTime
      cy.log(`视图切换总时间: ${switchTime}ms`)
      const avgSwitchTime = switchTime / 3
      cy.log(`平均切换时间: ${avgSwitchTime}ms`)
      expect(avgSwitchTime).to.be.lessThan(200)
    })
  })
  
  it('内存使用应保持稳定', () => {
    cy.visit('/')
    cy.waitForApp()
    
    // 获取初始内存使用
    cy.window().then((win) => {
      if ('memory' in win.performance) {
        const initialMemory = (win.performance as any).memory.usedJSHeapSize
        cy.log(`初始内存: ${(initialMemory / 1024 / 1024).toFixed(2)} MB`)
        
        // 执行一系列操作
        cy.createBulkRequirements(100)
        
        // 切换视图多次
        for (let i = 0; i < 10; i++) {
          cy.contains('.ant-tabs-tab', '表格视图').click()
          cy.contains('.ant-tabs-tab', '图视图').click()
          cy.contains('.ant-tabs-tab', '树视图').click()
        }
        
        // 检查内存增长
        cy.window().then((win2) => {
          const finalMemory = (win2.performance as any).memory.usedJSHeapSize
          cy.log(`最终内存: ${(finalMemory / 1024 / 1024).toFixed(2)} MB`)
          
          const memoryGrowth = finalMemory - initialMemory
          const growthPercentage = (memoryGrowth / initialMemory) * 100
          
          cy.log(`内存增长: ${(memoryGrowth / 1024 / 1024).toFixed(2)} MB (${growthPercentage.toFixed(1)}%)`)
          
          // 内存增长不应超过50%
          expect(growthPercentage).to.be.lessThan(50)
        })
      }
    })
  })
})