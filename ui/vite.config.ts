import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import svgLoader from 'vite-svg-loader'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import featherResolver from './auto-feather-resolver'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())

  return {
    server: {
      port: parseInt(env.VITE_SERVER_PORT) || 8080
    },
    resolve: {
      alias: {
        '@/': new URL('./src/', import.meta.url).pathname,
        '~@featherds': '@featherds'
      },
      dedupe: ['vue']
    },
    plugins: [
      vue(),
      svgLoader(),
      AutoImport({
        imports: ['vue', 'vue-router', '@vueuse/core'],
        eslintrc: {
          enabled: true,
          filepath: './.eslintrc-auto-import.json'
        }
      }),
      Components({
        resolvers: [featherResolver]
      })
    ],
    test: {
      globals: true,
      environment: 'happy-dom',
      deps: {
        inline: ['/@featherds/']
      },
      coverage: {
        reporter: ['lcov', 'html']
      }
    }
  }
})
