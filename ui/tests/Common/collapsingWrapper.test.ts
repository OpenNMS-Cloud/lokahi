import { mount } from '@vue/test-utils'
import CollapsingWrapper from '@/components/Common/CollapsingWrapper.vue'

let wrapper: any

describe('CollapsingWrapper', () => {
    beforeAll(() => {
        wrapper = mount(CollapsingWrapper as any, { shallow: false, props: { open: false }, slots: { default: '<div></div>' } })
    })

    test('Mount component', () => {
        const cmp = wrapper.get('[data-test="collapsing-wrapper-wrapper"]')
        expect(cmp.exists()).toBeTruthy()
    })

})
