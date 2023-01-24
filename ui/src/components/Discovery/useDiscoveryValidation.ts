import ipRegex from 'ip-regex'
import cidrRegex from 'cidr-regex'
import { useDiscoveryStore } from '@/store/Views/discoveryStore'

const validation = reactive({
  ipAddressesError: '',
  cidrError: '',
  fromIpError: '',
  toIpError: ''
})

const error = ref(false)

const useDiscoveryValidation = () => {
  const store = useDiscoveryStore()

  /**
   * Validates an IP.
   * @param val IP
   * @returns Empty str if Valid, Error Message if Not.
   */
  const validateIP = (val: string): string => {
    if (!val) return ''
    const isIpValid = ipRegex({ exact: true }).test(val) 
    return !isIpValid ? 'Contains an invalid IP.' : ''
  }

  /**
   * Validates multiple IPs.
   * @param ips array of ips
   * @returns Empty str if Valid, Error Message if Not.
   */
  const validateIPs = (ips: string[]): string => {
    for (const ip of ips) {
      const err = validateIP(ip)
      if (err) return err
    }
    return ''
  }

  /**
   * Validates a CIDR.
   * @param val CIDR
   * @returns Empty str if Valid, Error Message if Not.
   */
  const validateCIDR = (val: string): string => {
    if (!val) return ''
    const isCidrValid = cidrRegex({ exact: true }).test(val) 
    return !isCidrValid ? 'Contains an invalid CIDR.' : ''
  }

  const validate = () => {
    validation.ipAddressesError = computed(() => validateIPs(store.ipAddresses)) as unknown as string
    validation.cidrError = computed(() => validateCIDR(store.ipRange.cidr)) as unknown as string
    validation.fromIpError = computed(() => validateIP(store.ipRange.fromIp)) as unknown as string
    validation.toIpError = computed(() => validateIP(store.ipRange.toIp)) as unknown as string

    if (validation.ipAddressesError 
        || validation.cidrError
        || validation.fromIpError 
        || validation.toIpError) {
      error.value = true
      return
    } 

    // no errors
    error.value = false
  }

  return { error, validate, validation }
}

export default useDiscoveryValidation
