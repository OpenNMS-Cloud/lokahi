import { useWelcomeStore } from '@/store/Views/welcomeStore'
import mount from '../mountWithPiniaVillus'
import WelcomeSlideThree from '@/components/Welcome/WelcomeSlideThree.vue'

let wrapper: any

describe.skip('WelcomeSlideTwo', () => {
    beforeAll(() => {
        wrapper = mount({
            component: WelcomeSlideThree,
        })
    })
    afterAll(() => {
        wrapper.unmount()
    })

    test('Mount', () => {
        expect(wrapper).toBeTruthy()
    })

    test('Should have a title', () => {
        const elem = wrapper.get('[data-test="welcome-slide-three-title"]')
        expect(elem.exists()).toBeTruthy()
    })

    test('Should have a name field', () => {
        const elem = wrapper.get('[data-test="welcome-slide-three-name-input"]')
        expect(elem.exists()).toBeTruthy()
    })

    test('Should have an ip field', () => {
        const elem = wrapper.get('[data-test="welcome-slide-three-ip-input"]')
        expect(elem.exists()).toBeTruthy()
    })

    test('Should have a community string field ', () => {
        const elem = wrapper.get('[data-test="welcome-slide-three-communityString-input"]')
        expect(elem.exists()).toBeTruthy()
    })

    test('Should have a port field ', () => {
        const elem = wrapper.get('[data-test="welcome-slide-three-port-input"]')
        expect(elem.exists()).toBeTruthy()
    })

    test('Should have a start discovery button', () => {
        const elem = wrapper.get('[data-test="welcome-slide-three-start-discovery-button"]')
        expect(elem.exists()).toBeTruthy()
    })

    test('Should have a skip button', () => {
        const elem = wrapper.get('[data-test="welcome-slide-three-skip-button"]')
        expect(elem.exists()).toBeTruthy()
    })

    test('Should have a continue button', () => {
        const elem = wrapper.get('[data-test="welcome-slide-two-continue-button"]')
        expect(elem.exists()).toBeTruthy()
    })

})
