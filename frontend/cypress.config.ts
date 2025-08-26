import { defineConfig } from 'cypress'

export default defineConfig({
  e2e: {
    // 基础URL配置
    baseUrl: 'http://localhost:5173',
    
    // 视口大小
    viewportWidth: 1280,
    viewportHeight: 720,
    
    // 超时配置
    defaultCommandTimeout: 10000,
    requestTimeout: 10000,
    responseTimeout: 10000,
    
    // 测试文件模式
    specPattern: 'cypress/e2e/**/*.{cy,spec}.{js,jsx,ts,tsx}',
    
    // 支持文件
    supportFile: 'cypress/support/e2e.ts',
    
    // 视频和截图
    video: true,
    screenshotOnRunFailure: true,
    
    // 重试配置
    retries: {
      runMode: 2,
      openMode: 0
    },
    
    setupNodeEvents(on, config) {
      // 性能测试插件
      on('task', {
        // 记录性能指标
        recordPerformance(metrics) {
          console.log('Performance Metrics:', metrics)
          return null
        },
        
        // 清理测试数据
        cleanupTestData() {
          // 清理测试产生的数据
          return null
        }
      })
      
      return config
    }
  },
  
  component: {
    devServer: {
      framework: 'react',
      bundler: 'vite'
    },
    specPattern: 'src/**/*.cy.{js,jsx,ts,tsx}',
    supportFile: 'cypress/support/component.ts'
  }
})