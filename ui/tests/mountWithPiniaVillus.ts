import { mount } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import { VILLUS_CLIENT, createClient } from 'villus'

/**
 * Mounting component with pinia/testing, villus and additional options.
 * Usage:
 *  wrapper = mountWithPiniaVillus({
 *    component: SomeComponent,
 *    shallow: true, // by default
 *    props,
 *    ...
 *  })
 *
 * @param mountingOption
 *            component
 *            shallow
 *            global
 *              stubs
 *              plugins
 *              provide
 *                directives
 *            attachTo
 * @returns mounted component
 */
const wrapper = (mountingOption: Record<string, any>): any => {
  const { component, shallow = true, props = {}, global = {}, attachTo } = mountingOption
  const { stubs = {}, plugins = [], provide = {}, directives = {} } = global

  const globalOptions: Record<string, any> = {
    shallow,
    props,
    global: {
      stubs: { ...stubs },
      plugins: [createTestingPinia(), ...plugins],
      provide: {
        [VILLUS_CLIENT as unknown as string]: createClient({
          url: 'https://test/graphql'
        }),
        ...provide,
        directives: { ...directives }
      }
    },
    attachTo
  }

  return mount(component, globalOptions)
}

export default wrapper
