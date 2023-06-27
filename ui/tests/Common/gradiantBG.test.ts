import { mount } from '@vue/test-utils'
import GradiantBG from '@/components/Common/GradiantBG.vue'

let wrapper: any

describe('GradiantBG', () => {
    beforeAll(() => {
        wrapper = mount(GradiantBG, { shallow: true })
    })

    test('Mount component', () => {
        const cmp = wrapper.get('[data-test="gradiant-bg"]')
        expect(cmp.exists()).toBeTruthy()
    })

})
