/**
 * 基础导航测试
 */

describe('基础导航和页面加载', () => {
  beforeEach(() => {
    cy.visit('/')
  })
  
  it('应该成功加载应用', () => {
    // 验证主要组件存在
    cy.get('.app-container').should('be.visible')
    cy.get('.ant-layout-sider').should('be.visible')
    cy.get('.ant-tabs').should('be.visible')
  })
  
  it('应该显示三个视图标签', () => {
    cy.get('.ant-tabs-tab').should('have.length', 4)
    cy.contains('.ant-tabs-tab', '树视图').should('be.visible')
    cy.contains('.ant-tabs-tab', '表格视图').should('be.visible')
    cy.contains('.ant-tabs-tab', '图视图').should('be.visible')
    cy.contains('.ant-tabs-tab', '分屏视图').should('be.visible')
  })
  
  it('应该能够切换视图', () => {
    // 切换到表格视图
    cy.contains('.ant-tabs-tab', '表格视图').click()
    cy.get('[data-testid="table-view"]').should('be.visible')
    
    // 切换到图视图
    cy.contains('.ant-tabs-tab', '图视图').click()
    cy.get('[data-testid="graph-view"]').should('be.visible')
    
    // 切换到树视图
    cy.contains('.ant-tabs-tab', '树视图').click()
    cy.get('[data-testid="tree-view"]').should('be.visible')
  })
  
  it('应该能够折叠侧边栏', () => {
    // 找到折叠按钮并点击
    cy.get('.ant-layout-sider-trigger').click()
    
    // 验证侧边栏已折叠
    cy.get('.ant-layout-sider-collapsed').should('exist')
    
    // 再次点击展开
    cy.get('.ant-layout-sider-trigger').click()
    cy.get('.ant-layout-sider-collapsed').should('not.exist')
  })
  
  it('应该显示刷新按钮并能够刷新数据', () => {
    cy.contains('button', '刷新数据').should('be.visible').click()
    
    // 验证loading状态
    cy.get('.ant-btn-loading').should('exist')
    
    // 等待加载完成
    cy.get('.ant-btn-loading', { timeout: 5000 }).should('not.exist')
  })
  
  it('应该检查页面加载性能', () => {
    cy.checkPerformance({
      maxRenderTime: 3000,
      maxResponseTime: 1000
    })
  })
})