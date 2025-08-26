/**
 * 需求CRUD操作测试
 */

describe('需求管理CRUD操作', () => {
  beforeEach(() => {
    cy.visit('/')
    cy.waitForApp()
    
    // 切换到表格视图进行CRUD操作
    cy.contains('.ant-tabs-tab', '表格视图').click()
  })
  
  it('应该创建新需求', () => {
    // 点击新建按钮
    cy.contains('button', '新建').click()
    
    // 填写表单
    cy.get('#reqId').type('E2E-001')
    cy.get('#declaredName').type('E2E测试需求')
    cy.get('#declaredShortName').type('E2E-TEST')
    cy.get('#text').type('这是通过E2E测试创建的需求')
    
    // 选择状态
    cy.get('#status').click()
    cy.contains('.ant-select-item', '草稿').click()
    
    // 提交表单
    cy.contains('button', '确定').click()
    
    // 验证创建成功
    cy.contains('创建成功').should('be.visible')
    cy.contains('E2E测试需求').should('be.visible')
  })
  
  it('应该编辑需求', () => {
    // 先创建一个需求
    cy.createRequirement({
      reqId: 'E2E-002',
      name: '待编辑需求'
    })
    cy.reload()
    
    // 找到需求并点击编辑
    cy.contains('tr', 'E2E-002')
      .find('button')
      .contains('编辑')
      .click()
    
    // 修改内容
    cy.get('input[value="待编辑需求"]')
      .clear()
      .type('已修改的需求名称')
    
    // 保存
    cy.contains('button', '保存').click()
    
    // 验证修改成功
    cy.contains('保存成功').should('be.visible')
    cy.contains('已修改的需求名称').should('be.visible')
  })
  
  it('应该删除需求', () => {
    // 创建待删除需求
    cy.createRequirement({
      reqId: 'E2E-003',
      name: '待删除需求'
    })
    cy.reload()
    
    // 找到需求并删除
    cy.contains('tr', 'E2E-003')
      .find('button')
      .contains('删除')
      .click()
    
    // 确认删除
    cy.contains('button', '确定').click()
    
    // 验证删除成功
    cy.contains('删除成功').should('be.visible')
    cy.contains('E2E-003').should('not.exist')
  })
  
  it('应该支持批量选择和操作', () => {
    // 创建多个需求
    cy.createRequirement({ reqId: 'BATCH-001', name: '批量需求1' })
    cy.createRequirement({ reqId: 'BATCH-002', name: '批量需求2' })
    cy.createRequirement({ reqId: 'BATCH-003', name: '批量需求3' })
    cy.reload()
    
    // 选中多个需求
    cy.get('input[type="checkbox"]').first().click() // 全选
    
    // 验证选中状态
    cy.contains('已选择').should('be.visible')
    cy.get('.ant-table-row-selected').should('have.length.at.least', 3)
    
    // 批量删除
    cy.contains('button', '批量删除').click()
    cy.contains('button', '确定').click()
    
    // 验证批量删除成功
    cy.contains('批量删除成功').should('be.visible')
  })
  
  it('应该支持搜索功能', () => {
    // 创建测试数据
    cy.createRequirement({ reqId: 'SEARCH-001', name: '电池系统需求' })
    cy.createRequirement({ reqId: 'SEARCH-002', name: '充电系统需求' })
    cy.createRequirement({ reqId: 'SEARCH-003', name: '控制系统需求' })
    cy.reload()
    
    // 搜索"电池"
    cy.get('input[placeholder="搜索需求..."]').type('电池')
    cy.wait(500) // 等待防抖
    
    // 验证搜索结果
    cy.contains('电池系统需求').should('be.visible')
    cy.contains('充电系统需求').should('not.exist')
    cy.contains('控制系统需求').should('not.exist')
    
    // 清空搜索
    cy.get('input[placeholder="搜索需求..."]').clear()
    cy.wait(500)
    
    // 验证所有数据重新显示
    cy.contains('充电系统需求').should('be.visible')
  })
  
  it('应该支持排序功能', () => {
    // 创建带序号的测试数据
    cy.createRequirement({ reqId: 'SORT-003', name: 'C需求' })
    cy.createRequirement({ reqId: 'SORT-001', name: 'A需求' })
    cy.createRequirement({ reqId: 'SORT-002', name: 'B需求' })
    cy.reload()
    
    // 点击名称列排序
    cy.contains('th', '名称').click()
    
    // 验证升序排列
    cy.get('.ant-table-tbody tr').then(($rows) => {
      const names = $rows.map((i, el) => 
        Cypress.$(el).find('td').eq(2).text()
      ).get()
      
      const sorted = [...names].sort()
      expect(names).to.deep.equal(sorted)
    })
    
    // 再次点击切换降序
    cy.contains('th', '名称').click()
    
    // 验证降序排列
    cy.get('.ant-table-tbody tr').then(($rows) => {
      const names = $rows.map((i, el) => 
        Cypress.$(el).find('td').eq(2).text()
      ).get()
      
      const sorted = [...names].sort().reverse()
      expect(names).to.deep.equal(sorted)
    })
  })
  
  it('应该支持分页功能', () => {
    // 创建15条数据以触发分页
    for (let i = 1; i <= 15; i++) {
      cy.createRequirement({
        reqId: `PAGE-${i.toString().padStart(3, '0')}`,
        name: `分页测试需求${i}`
      })
    }
    cy.reload()
    
    // 验证分页器存在
    cy.get('.ant-pagination').should('be.visible')
    
    // 切换到第二页
    cy.get('.ant-pagination-item-2').click()
    
    // 验证第二页数据
    cy.contains('PAGE-011').should('be.visible')
    
    // 修改每页显示条数
    cy.get('.ant-select-selector').contains('10').click()
    cy.contains('.ant-select-item', '20').click()
    
    // 验证每页20条
    cy.get('.ant-table-tbody tr').should('have.length.at.least', 15)
  })
})