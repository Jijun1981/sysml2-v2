import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  test: {
    globals: true,
    environment: 'jsdom', // 回退到jsdom以保证兼容性
    setupFiles: './src/test/setup.ts',
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
    },
    // 使用线程池提升性能
    pool: 'threads',
    poolOptions: {
      threads: {
        // 允许并发测试
        singleThread: false
      }
    },
    // 减少超时时间
    testTimeout: 5000,
    // 禁用隔离以提升性能（测试需要正确清理）
    isolate: false
  },
})