import mountWithPiniaVillus from '../mountWithPiniaVillus'
import DiscoverySyslogSNMPTrapsForm from '@/components/Discovery/DiscoverySyslogSNMPTrapsForm.vue'
import tabIndexDirective from '@/directives/v-tabindex'

let wrapper: any

describe('DiscoverySyslogSNMPTrapsForm', () => {
  beforeAll(() => {
    wrapper = mountWithPiniaVillus({
      component: DiscoverySyslogSNMPTrapsForm,
      shallow: false,
      props: { successCallback: () => ({}), cancel: () => ({}) },
      global: {
        directives: {
          tabindex: tabIndexDirective
        }
      }
    })
  })

  test('Mount component', () => {
    const elem = wrapper.get('[data-test="syslog-snmp-traps-form"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should have a headline', () => {
    const elem = wrapper.get('[data-test="headline"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should have a form', () => {
    const elem = wrapper.get('[data-test="form"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should have Locations autocomplete', () => {
    const elem = wrapper.get('[data-test="locations-autocomplete"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should have Help Configuring', () => {
    const elem = wrapper.get('[data-test="help-configuring"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should have Tags autocomplete', () => {
    const elem = wrapper.get('[data-test="tags-autocomplete"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should have Community String content editable', () => {
    const elem = wrapper.get('[data-test="cmmunity-string-content-editable"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should have UDP Port content editable', () => {
    const elem = wrapper.get('[data-test="cmmunity-string-content-editable"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should have a cancel button', async () => {
    const elem = wrapper.get('[data-test="btn-cancel"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should have a submit button', () => {
    const elem = wrapper.get('[data-test="btn-submit"]')
    expect(elem.exists()).toBeTruthy()
  })
})
