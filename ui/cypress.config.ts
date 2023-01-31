import { defineConfig } from 'cypress'
import viteConfig from './vite.config'
import { loadEnv } from 'vite'

const env = loadEnv('development', process.cwd())

export default defineConfig({
  component: {
    specPattern: 'cypress/component/**/*.cy.*.{js,jsx,ts,tsx}',
    devServer: {
      framework: 'vue',
      bundler: 'vite',
      viteConfig
    }
  },
  e2e: {
    baseUrl: env.VITE_CYPRESS_BASE_URL,
    specPattern: 'cypress/e2e/**/*.cy.*.{js,jsx,ts,tsx}',
    setupNodeEvents(on, config) {
      // implement node event listeners here
    }
  }
})
