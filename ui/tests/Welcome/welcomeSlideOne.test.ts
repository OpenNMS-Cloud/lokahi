import mount from '../mountWithPiniaVillus'
import WelcomeSlideOne from '@/components/Welcome/WelcomeSlideOne.vue'

let wrapper: any

describe('WelcomeSlideOne', () => {
    beforeAll(() => {
        wrapper = mount({
            component: WelcomeSlideOne,
            shallow: false
        })
    })
    afterAll(() => {
        wrapper.unmount()
    })

    test('Mount', () => {
        expect(wrapper).toBeTruthy()
    })

    test('Should have a title', () => {
        const elem = wrapper.get('[data-test="welcome-slide-one-title"]')
        expect(elem.exists()).toBeTruthy()
    })

    test('Should have a requirements table', () => {
        const elem = wrapper.get('[data-test="welcome-slide-one-toggler"]')
        elem.trigger('click');
        const elem1 = wrapper.get('[data-test="welcome-system-requirements-table"]')
        expect(elem1.exists()).toBeTruthy()
    })

    test('Should have a button to advance to the next slide', () => {
        const elem = wrapper.get('[data-test="welcome-slide-one-setup-button"]')
        expect(elem.exists()).toBeTruthy()
    })

})

