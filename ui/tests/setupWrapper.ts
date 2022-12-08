import { mount, shallowMount } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import { VILLUS_CLIENT, createClient } from 'villus'

/**
 * Mounting component with pinia/testing, villus and global options parameters, 
 * @param mountingOption 
 * @returns mounted component
 */
const wrapper = (mountingOption: Record<string, any>): any => {
  const { component, global = {}, type = 'mount', attachTo, props } = mountingOption
  const  { stubs = {}, plugins = [], provide = {} } = global

  const globalOptions: Record<string, any> = {
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
    props,
    attachTo
  }
  
  if(type === 'mount') return mount(component, globalOptions)

  return shallowMount(component, globalOptions)
}

export default wrapper