import ipRegex from 'ip-regex'
import cidrRegex from 'cidr-regex'
import { useDiscoveryStore } from '@/store/Views/discoveryStore'

const validation = reactive({
  ipAddressesError: '' as any,
  cidrError: '' as any,
  fromIpError: '' as any,
  toIpError: '' as any
})

const error = ref(false)

const useDiscoveryValidation = () => {
  const store = useDiscoveryStore()

  /**
   * Validates an IP.
   * @param val IP
   * @returns Blank if Valid, Error Message if Not.
   */
  const validateIP = (val: string) => {
    if (!val) return 
    const isIpValid = ipRegex({ exact: true }).test(val) 
    if (!isIpValid) return 'Contains an invalid IP.'
  }

  /**
   * Validates multiple IPs.
   * @param ips array of ips
   * @returns Blank if Valid, Error Message if Not.
   */
  const validateIPs = (ips: string[]) => {
    for (const ip of ips) {
      const err = validateIP(ip)
      if (err) return err
    }
  }

  /**
   * Validates a CIDR.
   * @param val CIDR
   * @returns undefined if Valid, Error Message if Not.
   */
  const validateCIDR = (val: string) => {
    if (!val) return
    const isCidrValid = cidrRegex({ exact: true }).test(val) 
    if (!isCidrValid) return 'Contains an invalid CIDR.'
  }

  const validate = () => {
    validation.ipAddressesError = computed(() => validateIPs(store.ipAddresses))
    validation.cidrError = computed(() => validateCIDR(store.ipRange.cidr))
    validation.fromIpError = computed(() => validateIP(store.ipRange.fromIp))
    validation.toIpError = computed(() => validateIP(store.ipRange.toIp))

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
