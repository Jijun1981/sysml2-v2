import React from 'react'
import { ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import ModelViewerClean from './components/ModelViewerClean'
import 'antd/dist/reset.css'
import './styles/App.css'

/**
 * SysML v2 MVP 应用主组件
 */
const App: React.FC = () => {
  return (
    <ConfigProvider locale={zhCN}>
      <div className="app-container">
        <ModelViewerClean />
      </div>
    </ConfigProvider>
  )
}

export default App