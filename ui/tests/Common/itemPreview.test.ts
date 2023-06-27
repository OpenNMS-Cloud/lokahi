import { mount } from '@vue/test-utils'
import ItemPreview from '@/components/Common/ItemPreview.vue'

let wrapper: any

describe('ItemPreview', () => {
    beforeAll(() => {
        wrapper = mount(ItemPreview, { shallow: true })
    })

    test('Mount component', () => {
        const cmp = wrapper.get('[data-test="item-preview"]')
        expect(cmp.exists()).toBeTruthy()
    })

})
