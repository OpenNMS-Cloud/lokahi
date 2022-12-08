import { mount } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import { VILLUS_CLIENT, createClient } from 'villus'

/**
 * Mounting component with pinia/testing, villus and additional options. 
 * @param mountingOption
 *            component
 *            shallow
 *            global
 *              stubs
 *              plugins
 *              provide
 *            attachTo
 * @returns mounted component
 */
const wrapper = (mountingOption: Record<string, any>): any => {
  const { component, shallow = false, global = {}, attachTo } = mountingOption
  const  { stubs = {}, plugins = [], provide = {} } = global

  const globalOptions: Record<string, any> = {
    shallow,
    global: {
      stubs: { ...stubs },
      plugins: [ 
        createTestingPinia(),
        ...plugins 
      ],
      provide: {
        [VILLUS_CLIENT as unknown as string]: createClient({
          url: 'https://test/graphql'
        }),
        ...provide
      }
    },
    attachTo
  }
  
  return mount(component, globalOptions)
}

export default wrapper