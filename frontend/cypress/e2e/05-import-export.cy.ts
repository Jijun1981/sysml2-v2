/**
 * 数据导入导出测试
 */

describe('数据导入导出功能', () => {
  beforeEach(() => {
    cy.visit('/')
    cy.waitForApp()
  })
  
  it('应该能够导出JSON格式数据', () => {
    // 创建测试数据
    cy.createRequirement({ reqId: 'EXPORT-001', name: '导出测试需求1' })
    cy.createRequirement({ reqId: 'EXPORT-002', name: '导出测试需求2' })
    cy.reload()
    
    // 点击导出按钮
    cy.contains('button', '导出').click()
    
    // 选择JSON格式
    cy.contains('JSON格式').click()
    
    // 验证下载
    cy.readFile('cypress/downloads/export.json').should('exist')
      .then((content) => {
        expect(content).to.have.property('requirements')
        expect(content.requirements).to.have.length.at.least(2)
      })
  })
  
  it('应该能够导入JSON数据', () => {
    // 准备导入文件
    const importData = {
      requirements: [
        {
          reqId: 'IMPORT-001',
          name: '导入测试需求1',
          text: '通过导入创建',
          eClass: 'RequirementDefinition'
        },
        {
          reqId: 'IMPORT-002',
          name: '导入测试需求2',
          text: '通过导入创建',
          eClass: 'RequirementDefinition'
        }
      ]
    }
    
    // 创建测试文件
    cy.writeFile('cypress/fixtures/import-test.json', importData)
    
    // 点击导入按钮
    cy.contains('button', '导入').click()
    
    // 上传文件
    cy.get('input[type="file"]').selectFile('cypress/fixtures/import-test.json', { force: true })
    
    // 确认导入
    cy.contains('button', '确定导入').click()
    
    // 验证导入成功
    cy.contains('导入成功').should('be.visible')
    cy.contains('导入测试需求1').should('be.visible')
    cy.contains('导入测试需求2').should('be.visible')
  })
  
  it('应该验证导入数据的完整性', () => {
    // 准备包含关系的复杂数据
    const complexData = {
      requirements: [
        {
          reqId: 'PARENT-001',
          name: '父需求',
          eClass: 'RequirementDefinition'
        }
      ],
      usages: [
        {
          id: 'USAGE-001',
          of: 'PARENT-001',
          name: '需求使用',
          eClass: 'RequirementUsage'
        }
      ],
      dependencies: [
        {
          fromId: 'PARENT-001',
          toId: 'USAGE-001',
          type: 'contain'
        }
      ]
    }
    
    cy.writeFile('cypress/fixtures/complex-import.json', complexData)
    
    // 导入复杂数据
    cy.contains('button', '导入').click()
    cy.get('input[type="file"]').selectFile('cypress/fixtures/complex-import.json', { force: true })
    cy.contains('button', '确定导入').click()
    
    // 切换到树视图验证层级关系
    cy.contains('.ant-tabs-tab', '树视图').click()
    
    // 展开父节点
    cy.contains('.ant-tree-treenode', '父需求')
      .find('.ant-tree-switcher')
      .click()
    
    // 验证子节点
    cy.contains('.ant-tree-treenode', '需求使用').should('be.visible')
  })
  
  it('应该处理导入错误', () => {
    // 准备错误格式的数据
    const invalidData = {
      invalid_field: 'test'
    }
    
    cy.writeFile('cypress/fixtures/invalid-import.json', invalidData)
    
    // 尝试导入
    cy.contains('button', '导入').click()
    cy.get('input[type="file"]').selectFile('cypress/fixtures/invalid-import.json', { force: true })
    
    // 应该显示错误提示
    cy.contains('数据格式错误').should('be.visible')
  })
  
  it('应该支持批量数据导出', () => {
    // 创建大量测试数据
    cy.createBulkRequirements(100)
    
    // 选择要导出的数据
    cy.contains('.ant-tabs-tab', '表格视图').click()
    cy.get('input[type="checkbox"]').first().click() // 全选
    
    // 导出选中数据
    cy.contains('button', '导出选中').click()
    
    // 验证导出文件
    cy.readFile('cypress/downloads/export-selected.json').should('exist')
      .then((content) => {
        expect(content.requirements).to.have.length.at.least(10) // 至少导出了一页的数据
      })
  })
  
  it('导出导入往返应保持数据一致性', () => {
    // 创建原始数据
    const originalData = {
      reqId: 'ROUNDTRIP-001',
      name: '往返测试需求',
      text: '测试导出后再导入的数据一致性',
      status: 'approved',
      tags: ['test', 'e2e']
    }
    
    cy.createRequirement(originalData)
    cy.reload()
    
    // 导出数据
    cy.contains('button', '导出').click()
    cy.contains('JSON格式').click()
    
    // 读取导出的数据
    cy.readFile('cypress/downloads/export.json').then((exported) => {
      // 清空数据库
      cy.contains('button', '清空数据').click()
      cy.contains('button', '确认清空').click()
      
      // 重新导入
      cy.writeFile('cypress/fixtures/reimport.json', exported)
      cy.contains('button', '导入').click()
      cy.get('input[type="file"]').selectFile('cypress/fixtures/reimport.json', { force: true })
      cy.contains('button', '确定导入').click()
      
      // 验证数据一致性
      cy.contains('往返测试需求').should('be.visible')
      cy.contains('ROUNDTRIP-001').should('be.visible')
      cy.contains('approved').should('be.visible')
    })
  })
})