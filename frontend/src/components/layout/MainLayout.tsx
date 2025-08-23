import React, { useState, useEffect } from 'react'
import { Layout, Tabs, Space, Button, message } from 'antd'
import { ReloadOutlined, DownloadOutlined, UploadOutlined } from '@ant-design/icons'
import TreeView from '../views/TreeView/TreeView'
import TableView from '../views/TableView/TableView'
import GraphView from '../views/GraphView/GraphView'
import { useModel } from '../../contexts/ModelContext'
import './MainLayout.css'

const { Header, Content } = Layout
const { TabPane } = Tabs

/**
 * 主布局组件
 * REQ-A1-1: 数据源唯一 - 三视图共享同一数据源
 */
const MainLayout: React.FC = () => {
  const [activeTab, setActiveTab] = useState('tree')
  const { loadProject, refresh, loading } = useModel()
  
  useEffect(() => {
    // 初始加载默认项目
    loadProject('default').catch(err => {
      message.error('加载项目失败')
    })
  }, [loadProject])
  
  const handleRefresh = async () => {
    try {
      await refresh()
      message.success('刷新成功')
    } catch (err) {
      message.error('刷新失败')
    }
  }
  
  const handleExport = () => {
    // TODO: 实现导出功能
    message.info('导出功能开发中')
  }
  
  const handleImport = () => {
    // TODO: 实现导入功能
    message.info('导入功能开发中')
  }
  
  return (
    <Layout className="main-layout">
      <Header className="layout-header">
        <div className="header-title">SysML v2 建模平台 MVP</div>
        <Space>
          <Button 
            icon={<ReloadOutlined />} 
            onClick={handleRefresh}
            loading={loading}
          >
            刷新
          </Button>
          <Button icon={<DownloadOutlined />} onClick={handleExport}>
            导出
          </Button>
          <Button icon={<UploadOutlined />} onClick={handleImport}>
            导入
          </Button>
        </Space>
      </Header>
      
      <Content className="layout-content">
        <Tabs 
          activeKey={activeTab} 
          onChange={setActiveTab}
          size="large"
          className="view-tabs"
        >
          <TabPane tab="树视图" key="tree">
            <TreeView />
          </TabPane>
          <TabPane tab="表视图" key="table">
            <TableView />
          </TabPane>
          <TabPane tab="图视图" key="graph">
            <GraphView />
          </TabPane>
        </Tabs>
      </Content>
    </Layout>
  )
}

export default MainLayout