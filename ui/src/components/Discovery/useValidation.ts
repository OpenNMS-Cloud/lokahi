import ipRegex from 'ip-regex'
import cidrRegex from 'cidr-regex'

const useValidation = () => {
  const error = ref(false)
  const submitted = ref(false)
  const setSubmitted = () => (submitted.value = true)

  /**
   * Validates an IP.
   * @param val IP
   * @returns Blank if Valid, Error Message if Not.
   */
  const validateIP = (val: string) => {
    console.log(val, error.value)
    if (!val) {
      error.value = false
      return
    }

    const isIpValid = ipRegex({ exact: true }).test(val) 
    if (submitted.value && !isIpValid) {
      error.value = true
      return 'Contains an invalid IP.'
    }
  }

  /**
   * Validates multiple IPs.
   * @param val comma separate ips
   * @returns Blank if Valid, Error Message if Not.
   */
  const validateIPs = (val: string) => {
    if (!val) {
      error.value = false
      return
    }
    
    const ipsSplit = val.split(',')
    for (const ip of ipsSplit) {
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
    if (!val) {
      error.value = false
      return
    }

    const isCidrValid = cidrRegex({ exact: true }).test(val) 
    if (submitted.value && !isCidrValid) {
      error.value = true
      return !isCidrValid ? 'Contains an invalid CIDR.' : ''
    }
  }

  return { error, submitted, setSubmitted, validateIP, validateIPs, validateCIDR,  }
}

export default useValidation
