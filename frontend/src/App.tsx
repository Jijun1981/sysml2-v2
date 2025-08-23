import React from 'react'
import { BrowserRouter } from 'react-router-dom'
import { ModelProvider } from './contexts/ModelContext'
import MainLayout from './components/layout/MainLayout'
import './styles/App.css'

/**
 * 应用主组件
 */
const App: React.FC = () => {
  return (
    <BrowserRouter>
      <ModelProvider>
        <MainLayout />
      </ModelProvider>
    </BrowserRouter>
  )
}

export default App