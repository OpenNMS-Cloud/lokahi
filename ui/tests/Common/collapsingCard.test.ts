import { mount } from '@vue/test-utils'
import CollapsingCard from '@/components/Common/CollapsingCard.vue'

let wrapper: any

describe('CollapsingCard', () => {
    beforeAll(() => {
        wrapper = mount(CollapsingCard, { shallow: true })
    })

    test('Mount component', () => {
        const cmp = wrapper.get('[data-test="collapsing-card-wrapper"]')
        expect(cmp.exists()).toBeTruthy()
    })

})
