import { useWelcomeStore } from '@/store/Views/welcomeStore'
import mount from '../mountWithPiniaVillus'
import WelcomeSlideTwo from '@/components/Welcome/WelcomeSlideTwo.vue'

let wrapper: any

describe.skip('WelcomeSlideTwo', () => {
    beforeAll(() => {
        wrapper = mount({
            component: WelcomeSlideTwo,
        })
    })
    afterAll(() => {
        wrapper.unmount()
    })

    test('Mount', () => {
        expect(wrapper).toBeTruthy()
    })

    test('Should have a title', () => {
        const elem = wrapper.get('[data-test="welcome-slide-two-title"]')
        expect(elem.exists()).toBeTruthy()
    })

    test('Should have a location information callout', () => {
        const elem = wrapper.get('[data-test="welcome-slide-two-callout"]')
        expect(elem.exists()).toBeTruthy()
    })

    test('Welcome Slide Two Download Button Works', async () => {

        const welcomeStore = useWelcomeStore();
        const elem = wrapper.get('[data-test="welcome-slide-two-download-button"]')
        await elem.trigger('click');
        const hiddenElem = wrapper.get('[data-test="welcome-slide-two-internal"]')
        expect(hiddenElem).toBeDefined();
        expect(welcomeStore.minionStatusLoading).toBeTruthy()
    })
    test('Welcome Slide Two Minion Detector is working', async () => {

        const welcomeStore = useWelcomeStore();
        const elem = wrapper.get('[data-test="welcome-slide-two-download-button"]')
        await elem.trigger('click');
        expect(welcomeStore.minionStatusLoading).toBeTruthy()
    })

    test('Should have a back button', () => {
        const elem = wrapper.get('[data-test="welcome-slide-two-back-button"]')
        expect(elem.exists()).toBeTruthy()
    })

    test('Should have a continue button', () => {
        const elem = wrapper.get('[data-test="welcome-slide-two-continue-button"]')
        expect(elem.exists()).toBeTruthy()
    })

})
