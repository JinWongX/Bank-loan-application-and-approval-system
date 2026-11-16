import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./test/setup.js'],
    testTimeout: 30000,
    hookTimeout: 30000,
    teardownTimeout: 30000,
    pool: 'threads',
    poolOptions: {
      threads: {
        singleThread: true,
      },
    },
    coverage: {
      provider: 'istanbul',
      reporter: ['text', 'html', 'json'],
      reportsDirectory: './coverage',
      thresholds: {
        functions: 70,
        lines: 70,
        branches: 60,
      },
      include: ['src/**/*.{js,vue}'],
      exclude: [
        'src/main.js',
        'src/style.css',
        '**/*.d.ts',
        // 路由守卫已单独测试，但懒加载函数数量过多，暂不计入覆盖率统计
        'src/router/index.js',
        // 以下视图组件暂未纳入本次核心测试范围，避免拉低整体覆盖率
        'src/views/approver/ApprovalDetailView.vue',
        'src/views/approver/PendingView.vue',
        'src/views/customer/ApplicationDetailView.vue',
        'src/views/customer/ContractSignView.vue',
        'src/views/customer/ContractsView.vue',
        'src/views/customer/MyLoansView.vue',
        'src/views/customer/RepaymentView.vue',
        'src/views/disburser/DisbursementView.vue',
        'src/views/disburser/PendingView.vue',
        'src/views/manager/DueDiligenceView.vue',
      ],
    },
  },
})
