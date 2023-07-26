import { mount } from '@vue/test-utils'
import GradientBG from '@/components/Common/GradientBG.vue'

let wrapper: any

describe('GradientBG', () => {
    beforeAll(() => {
        wrapper = mount(GradientBG, { shallow: true })
    })

    test('Mount component', () => {
        const cmp = wrapper.get('[data-test="gradient-bg"]')
        expect(cmp.exists()).toBeTruthy()
    })

})
