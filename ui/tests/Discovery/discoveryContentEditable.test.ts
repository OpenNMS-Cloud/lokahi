import { mount } from '@vue/test-utils'
import DiscoveryContentEditable from '@/components/Discovery/DiscoveryContentEditable.vue'
import { ContentEditableType, IP_RANGE, REGEX_EXPRESSIONS } from '@/components/Discovery/discovery.constants'
let wrapper: any

describe('DiscoveryContentEditable', () => {
  beforeEach(() => {
    const props = {
      contentType: ContentEditableType.IP,
      regexDelim: IP_RANGE.regexDelim,
      id: 1,
      regexExpression: REGEX_EXPRESSIONS.IP
    }

    wrapper = mount(DiscoveryContentEditable, {
      props
    })
  })

  test('Mount component', () => {
    expect(wrapper.exists()).toBeTruthy()
  })

  test('Should have a label', () => {
    const elem = wrapper.get('label')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should have an input text', () => {
    const elem = wrapper.get('.content-editable')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should not have a validate icon', async () => {
    await wrapper.setProps({ regexDelim: '' })

    const elem = wrapper.get('.content-editable')
    elem.wrapperElement.textContent = ''
    await elem.trigger('keyup')

    const icon = wrapper.find('.validate-format')
    expect(icon.exists()).toBeFalsy()
  })

  test('Should have a validate icon', async () => {
    const elem = wrapper.get('.content-editable')
    elem.wrapperElement.textContent = 'some IP addresses'
    await elem.trigger('keyup')

    const icon = wrapper.get('.validate-format')
    expect(icon.exists()).toBeTruthy()
  })

  test('Should validate the text content', async () => {
    const fn = vi.spyOn(wrapper.vm, 'validateContent')

    const elem = wrapper.get('.content-editable')
    elem.wrapperElement.textContent = 'some IP addresses'
    await elem.trigger('keyup')

    const icon = wrapper.get('.validate-format')
    await icon.trigger('click')

    expect(fn).toHaveBeenCalledOnce()
  })

  test('Should validate regex for IP addreess in different format', async () => {
    const regexp = wrapper.vm.props.regexExpression?.map((r: string) => new RegExp(r))

    const validIpv4Addresses = [
      '192.0.2.1',
      '172.17.21.0',
      '10.10.10.10',
      '255.255.255.0',
      '127.0.0.1',
      '0.0.0.0',
      '192.168.0.1',
      '172.31.255.255',
      '169.254.0.1',
      '224.0.0.1',
      '239.255.255.250',
      '198.51.100.1',
      '203.0.113.1',
      '100.64.0.1',
      '192.0.0.0',
      '185.199.108.153',
      '104.26.5.199',
      '198.18.0.1',
      '100.127.255.255',
      '172.32.0.1'
    ]
    const validIpAddressRanges = [
      '192.0.2.0-192.0.2.255',
      '172.16.0.0-172.31.255.255',
      '10.0.0.0-10.255.255.255',
      '192.168.0.1-192.168.0.100',
      '172.17.21.0/24',
      '192.0.2.0/23',
      '10.10.0.0/16',
      '192.168.0.0/16',
      '192.0.2.0/24',
      '172.16.0.0/12',
      '10.0.0.0/8',
      '198.51.100.0/22',
      '203.0.113.0/24',
      '100.64.0.0/10',
      '0.0.0.0/0',
      '172.16.0.0/13',
      '10.64.0.0/10',
      '172.16.0.0/11',
      '192.0.0.0/24',
      '198.18.0.0/15'
    ]
    const validIpv6Addresses = [
      '2001:0db8:85a3:0000:0000:8a2e:0370:7334',
      '2001:db8:0:0:0:0:2:1',
      '2001:db8::1',
      '::1',
      'fe80::',
      '2001:0db8:1234:5678::',
      '2001:0db8:0000:0000:0000:0000:0000:0001',
      '2001:0db8:abcd:ef01:2345:6789:abcd:ef01',
      '2001:0db8::abcd:ef01',
      '2001:0db8:1234::/48',
      '::/128',
      '::/0',
      '2001:0db8::/32',
      'fe80::/10',
      'ff00::/8',
      '2001:0db8:abcd:ef01::/64'
    ]
    let isInvalid = false
    const IPs = [...validIpv4Addresses, ...validIpAddressRanges, ...validIpv6Addresses]
    IPs.map((ip: string) => {
      if (!regexp.map((t: RegExp) => t.test(ip)).includes(true)) {
        isInvalid = true
      }
    })

    expect(isInvalid).toBeFalsy()
  })
})
