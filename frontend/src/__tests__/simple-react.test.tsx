import React from 'react'
import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'

describe('简单React测试', () => {
  it('应该渲染文本', () => {
    render(<div>Hello World</div>)
    expect(screen.getByText('Hello World')).toBeDefined()
  })
})