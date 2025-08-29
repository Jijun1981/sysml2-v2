/**
 * 最简单的按钮点击测试
 * 测试Ant Design Modal按钮
 */

import React from 'react'
import { render, screen, fireEvent } from '@testing-library/react'
import { Modal, Button } from 'antd'
import { vi, describe, it, expect } from 'vitest'

describe('Ant Design Modal按钮测试', () => {
  it('应该能点击Modal的确定按钮', () => {
    const handleOk = vi.fn()
    
    render(
      <Modal
        title="测试Modal"
        open={true}
        onOk={handleOk}
        onCancel={() => {}}
        okText="创建"
      >
        <div>测试内容</div>
      </Modal>
    )
    
    // 查找按钮 - Ant Design会在中文字符间加空格
    const okButton = screen.getByRole('button', { name: '创 建' })
    console.log('Found button:', okButton)
    
    // 点击按钮
    fireEvent.click(okButton)
    console.log('Clicked button')
    
    // 验证处理函数被调用
    expect(handleOk).toHaveBeenCalled()
  })
  
  it('应该能通过表单提交', async () => {
    const handleSubmit = vi.fn()
    
    const TestForm = () => {
      return (
        <Modal
          title="测试表单"
          open={true}
          onOk={handleSubmit}
          onCancel={() => {}}
          okText="创建"
        >
          <input name="test" defaultValue="test value" />
        </Modal>
      )
    }
    
    render(<TestForm />)
    
    // 查找并点击按钮
    const okButton = screen.getByRole('button', { name: '创 建' })
    fireEvent.click(okButton)
    
    // 验证处理函数被调用
    expect(handleSubmit).toHaveBeenCalled()
  })
})