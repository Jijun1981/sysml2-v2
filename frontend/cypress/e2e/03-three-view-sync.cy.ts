/**
 * 三视图联动测试
 */

describe('三视图选中联动', () => {
  beforeEach(() => {
    cy.visit('/')
    cy.waitForApp()
    
    // 创建测试数据
    cy.createRequirement({ reqId: 'SYNC-001', name: '联动测试需求1' })
    cy.createRequirement({ reqId: 'SYNC-002', name: '联动测试需求2' })
    cy.createRequirement({ reqId: 'SYNC-003', name: '联动测试需求3' })
    cy.reload()
    
    // 切换到分屏视图
    cy.contains('.ant-tabs-tab', '分屏视图').click()
  })
  
  it('树视图选中应同步到其他视图', () => {
    // 在树视图中选中节点
    cy.get('[data-testid="tree-view"]')
      .contains('联动测试需求1')
      .click()
    
    // 验证表格视图同步选中
    cy.get('[data-testid="table-view"]')
      .find('tr')
      .contains('联动测试需求1')
      .parent('tr')
      .should('have.class', 'ant-table-row-selected')
    
    // 验证图视图同步选中
    cy.get('[data-testid="graph-view"]')
      .find('[data-id="SYNC-001"]')
      .should('have.class', 'selected')
  })
  
  it('表格视图选中应同步到其他视图', () => {
    // 在表格视图中选中行
    cy.get('[data-testid="table-view"]')
      .contains('tr', 'SYNC-002')
      .click()
    
    // 验证树视图同步选中
    cy.get('[data-testid="tree-view"]')
      .contains('.ant-tree-node-content-wrapper', '联动测试需求2')
      .should('have.class', 'ant-tree-node-selected')
    
    // 验证图视图同步选中
    cy.get('[data-testid="graph-view"]')
      .find('[data-id="SYNC-002"]')
      .should('have.class', 'selected')
  })
  
  it('图视图选中应同步到其他视图', () => {
    // 在图视图中选中节点
    cy.get('[data-testid="graph-view"]')
      .find('[data-id="SYNC-003"]')
      .click()
    
    // 验证树视图同步选中
    cy.get('[data-testid="tree-view"]')
      .contains('.ant-tree-node-content-wrapper', '联动测试需求3')
      .should('have.class', 'ant-tree-node-selected')
    
    // 验证表格视图同步选中
    cy.get('[data-testid="table-view"]')
      .find('tr')
      .contains('联动测试需求3')
      .parent('tr')
      .should('have.class', 'ant-table-row-selected')
  })
  
  it('应支持多选联动', () => {
    // 在树视图中多选
    cy.get('[data-testid="tree-view"]')
      .contains('联动测试需求1')
      .click()
    
    cy.get('body').type('{ctrl}', { release: false })
    
    cy.get('[data-testid="tree-view"]')
      .contains('联动测试需求2')
      .click()
    
    cy.get('body').type('{ctrl}', { release: true })
    
    // 验证表格视图多选同步
    cy.get('[data-testid="table-view"]')
      .find('.ant-table-row-selected')
      .should('have.length', 2)
    
    // 验证选中计数
    cy.contains('已选中').should('be.visible')
    cy.contains('2 项').should('be.visible')
  })
  
  it('清除选中应同步到所有视图', () => {
    // 先选中一些项
    cy.get('[data-testid="tree-view"]')
      .contains('联动测试需求1')
      .click()
    
    // 验证已选中
    cy.contains('已选中').should('be.visible')
    
    // 清除选中
    cy.contains('button', '清除选中').click()
    
    // 验证所有视图都清除了选中
    cy.get('.ant-tree-node-selected').should('not.exist')
    cy.get('.ant-table-row-selected').should('not.exist')
    cy.get('[data-testid="graph-view"] .selected').should('not.exist')
  })
  
  it('选中联动响应时间应小于100ms', () => {
    const startTime = Date.now()
    
    // 触发选中
    cy.get('[data-testid="tree-view"]')
      .contains('联动测试需求1')
      .click()
    
    // 验证其他视图同步
    cy.get('[data-testid="table-view"] .ant-table-row-selected')
      .should('exist')
      .then(() => {
        const responseTime = Date.now() - startTime
        cy.log(`联动响应时间: ${responseTime}ms`)
        expect(responseTime).to.be.lessThan(100)
      })
  })
})