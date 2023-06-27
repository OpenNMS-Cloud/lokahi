import { useWelcomeStore } from '@/store/Views/welcomeStore'
import mount from '../mountWithPiniaVillus'
import Welcome from '@/containers/Welcome.vue'

let wrapper: any

describe('WelcomeGuide', () => {
    beforeAll(() => {
        wrapper = mount({
            component: Welcome
        })
    })
    afterAll(() => {
        wrapper.unmount()
    })

    test('Mount', () => {
        expect(wrapper).toBeTruthy()
    })

    test('Should have a logo', () => {
        const elem = wrapper.get('[data-test="welcome-logo"]')
        expect(elem.exists()).toBeTruthy()
    })

    test('Should have a gradiant background', () => {
        const elem = wrapper.get('[data-test="gradiant-bg"]')
        expect(elem.exists()).toBeTruthy()
    })

})
