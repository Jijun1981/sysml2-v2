/**
 * TypeScript类型定义测试 - TDD第五阶段
 * 
 * 测试覆盖：
 * - ElementDTO与后端对应
 * - 高级查询相关类型
 * - 视图数据类型
 * - 类型安全验证
 */

import { describe, it, expect } from 'vitest'
import type {
  ElementDTO,
  RequirementDefinitionDTO,
  RequirementUsageDTO,
  DependencyDTO,
  QueryParams,
  AdvancedQueryResponse,
  TreeViewData,
  TableRowData,
  GraphViewData,
  ValidationResult
} from '../models'

describe('TypeScript类型定义 - 与后端DTO对应', () => {
  
  describe('ElementDTO基础类型', () => {
    it('应该支持基本ElementDTO结构', () => {
      const element: ElementDTO = {
        elementId: 'R-001',
        eClass: 'RequirementDefinition',
        properties: {
          declaredName: '测试需求',
          declaredShortName: 'TEST-001',
          reqId: 'REQ-001'
        }
      }

      expect(element.elementId).toBe('R-001')
      expect(element.eClass).toBe('RequirementDefinition')
      expect(element.properties.declaredName).toBe('测试需求')
    })

    it('应该支持任意properties映射', () => {
      const element: ElementDTO = {
        elementId: 'U-001', 
        eClass: 'RequirementUsage',
        properties: {
          of: 'R-001',
          status: 'approved',
          text: '这是需求文本',
          customField: 'custom value',
          numericField: 42,
          booleanField: true,
          arrayField: ['a', 'b', 'c']
        }
      }

      // 验证动态属性支持任意类型
      expect(element.properties.customField).toBeTypeOf('string')
      expect(element.properties.numericField).toBeTypeOf('number')
      expect(element.properties.booleanField).toBeTypeOf('boolean')
      expect(Array.isArray(element.properties.arrayField)).toBe(true)
    })
  })

  describe('专门的DTO类型', () => {
    it('应该正确定义RequirementDefinitionDTO', () => {
      const reqDef: RequirementDefinitionDTO = {
        elementId: 'R-001',
        eClass: 'RequirementDefinition',
        properties: {
          reqId: 'REQ-001',
          declaredName: '系统需求',
          text: '系统应当提供...',
          documentation: '这是文档说明',
          createdAt: '2025-08-25T21:30:00.000+0800',
          updatedAt: '2025-08-25T21:30:00.000+0800'
        }
      }

      expect(reqDef.eClass).toBe('RequirementDefinition')
      expect(reqDef.properties.reqId).toBe('REQ-001')
      expect(reqDef.properties.declaredName).toBe('系统需求')
    })

    it('应该正确定义RequirementUsageDTO', () => {
      const reqUsage: RequirementUsageDTO = {
        elementId: 'U-001',
        eClass: 'RequirementUsage', 
        properties: {
          of: 'R-001',
          declaredName: '需求使用实例',
          status: 'approved',
          text: '基于需求的实施',
          createdAt: '2025-08-25T21:30:00.000+0800'
        }
      }

      expect(reqUsage.eClass).toBe('RequirementUsage')
      expect(reqUsage.properties.of).toBe('R-001')
      expect(reqUsage.properties.status).toBe('approved')
    })

    it('应该正确定义DependencyDTO', () => {
      const dependency: DependencyDTO = {
        elementId: 'T-001',
        eClass: 'Dependency',
        properties: {
          fromId: 'R-001',
          toId: 'U-001', 
          type: 'derive',
          createdAt: '2025-08-25T21:30:00.000+0800'
        }
      }

      expect(dependency.eClass).toBe('Dependency')
      expect(dependency.properties.fromId).toBe('R-001')
      expect(dependency.properties.type).toBe('derive')
    })
  })

  describe('高级查询类型', () => {
    it('应该支持完整的查询参数', () => {
      const queryParams: QueryParams = {
        page: 1,
        size: 20,
        sort: [
          { field: 'declaredName', direction: 'asc' },
          { field: 'createdAt', direction: 'desc' }
        ],
        filter: [
          { field: 'eClass', value: 'RequirementDefinition' },
          { field: 'status', value: 'approved' }
        ],
        search: '电池'
      }

      expect(queryParams.page).toBe(1)
      expect(queryParams.sort![0].direction).toSatisfy((dir: string) => 
        dir === 'asc' || dir === 'desc'
      )
      expect(queryParams.filter![0].field).toBe('eClass')
      expect(queryParams.search).toBe('电池')
    })

    it('应该支持查询响应格式', () => {
      const response: AdvancedQueryResponse = {
        content: [
          {
            elementId: 'R-001',
            eClass: 'RequirementDefinition',
            properties: { declaredName: '测试需求' }
          }
        ],
        page: 0,
        size: 50,
        totalElements: 100,
        totalPages: 2,
        first: true,
        last: false,
        sort: { declaredName: 'asc' },
        filter: { eClass: 'RequirementDefinition' },
        search: '电池'
      }

      expect(response.content).toHaveLength(1)
      expect(response.totalElements).toBe(100)
      expect(response.sort!.declaredName).toBe('asc')
    })
  })

  describe('视图数据类型', () => {
    it('应该支持树视图数据结构', () => {
      const treeData: TreeViewData = {
        definitions: [
          {
            id: 'R-001',
            label: '系统需求',
            type: 'definition',
            usages: [
              {
                id: 'U-001',
                label: '需求实例1',
                type: 'usage'
              },
              {
                id: 'U-002', 
                label: '需求实例2',
                type: 'usage'
              }
            ]
          }
        ]
      }

      expect(treeData.definitions).toHaveLength(1)
      expect(treeData.definitions[0].usages).toHaveLength(2)
      expect(treeData.definitions[0].type).toBe('definition')
    })

    it('应该支持表视图数据结构', () => {
      const tableRows: TableRowData[] = [
        {
          id: 'R-001',
          eClass: 'RequirementDefinition',
          declaredShortName: 'REQ-001',
          declaredName: '系统需求',
          status: 'approved',
          customColumn: '自定义数据'
        }
      ]

      expect(tableRows[0].eClass).toBe('RequirementDefinition')
      expect(tableRows[0].customColumn).toBe('自定义数据')
    })

    it('应该支持图视图数据结构', () => {
      const graphData: GraphViewData = {
        nodes: [
          {
            id: 'R-001',
            title: '系统需求',
            type: 'requirement',
            position: { x: 100, y: 200 },
            data: { label: '系统需求', type: 'requirement' }
          }
        ],
        edges: [
          {
            id: 'E-001',
            source: 'R-001',
            target: 'U-001', 
            type: 'of',
            label: 'defines'
          }
        ]
      }

      expect(graphData.nodes).toHaveLength(1)
      expect(graphData.edges).toHaveLength(1)
      expect(graphData.nodes[0].position.x).toBe(100)
      expect(graphData.edges[0].type).toBe('of')
    })
  })

  describe('验证结果类型', () => {
    it('应该支持验证结果数据结构', () => {
      const validationResult: ValidationResult = {
        violations: [
          {
            elementId: 'R-001',
            rule: 'REQ-B3-1',
            message: '重复的reqId: REQ-001',
            severity: 'error'
          },
          {
            elementId: 'R-002',
            rule: 'REQ-E1-2', 
            message: '循环依赖检测到',
            severity: 'warning'
          }
        ],
        summary: {
          totalElements: 100,
          errorCount: 1,
          warningCount: 1,
          validElements: 98
        }
      }

      expect(validationResult.violations).toHaveLength(2)
      expect(validationResult.violations[0].severity).toBe('error')
      expect(validationResult.summary.errorCount).toBe(1)
    })
  })

  describe('类型安全验证', () => {
    it('应该在编译时捕获类型错误', () => {
      // 这些测试主要是编译时验证
      // 如果类型定义错误，TypeScript编译器会报错

      // 正确的类型使用
      const element: ElementDTO = {
        elementId: 'test',
        eClass: 'RequirementDefinition', 
        properties: {}
      }

      // 验证必需字段
      expect(element.elementId).toBeDefined()
      expect(element.eClass).toBeDefined()
      expect(element.properties).toBeDefined()
    })

    it('应该支持可选字段', () => {
      const queryParams: QueryParams = {
        // 只提供page，其他都是可选的
        page: 0
      }

      // 应该编译成功
      expect(queryParams.page).toBe(0)
      expect(queryParams.size).toBeUndefined()
      expect(queryParams.sort).toBeUndefined()
    })
  })
})