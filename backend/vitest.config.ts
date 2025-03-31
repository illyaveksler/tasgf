import { defineConfig } from 'vitest/config'

export default defineConfig({
  test: {
    fileParallelism: false,
    coverage: {
      reporter: ['lcov', 'text'],
      reportsDirectory: './coverage'
    },
  },
})