/**
 * 最小化App - 用于调试
 */

import React from 'react'

const AppMinimal: React.FC = () => {
  return (
    <div style={{ padding: '20px', textAlign: 'center' }}>
      <h1>🚀 SysML v2 MVP - 最小化测试</h1>
      <p>如果你能看到这个页面，说明基础React组件工作正常</p>
      
      <div style={{ marginTop: '20px' }}>
        <button 
          onClick={() => alert('按钮点击成功！')}
          style={{ padding: '10px 20px', fontSize: '16px', cursor: 'pointer' }}
        >
          测试按钮
        </button>
      </div>
      
      <div style={{ marginTop: '20px', padding: '15px', background: '#f0f0f0', borderRadius: '5px' }}>
        <h3>导入测试状态：</h3>
        <ul style={{ textAlign: 'left', display: 'inline-block' }}>
          <li>✅ React导入成功</li>
          <li>✅ 组件渲染成功</li>
          <li>待测试: Ant Design组件</li>
          <li>待测试: ModelViewer组件</li>
        </ul>
      </div>
    </div>
  )
}

export default AppMinimal