import { ValidationError } from 'yup'

/**
* 
* @param err Yup Validation Error(s)
* @returns a record of our validation errors, 
*          ready to be used in the FE as a basic dictionary instead of being Yup specific.
*/
export const validationErrorsToStringRecord = <T>(err: ValidationError): T => {
  const messages: Record<string, string | undefined> = {}
  const validationErrors = err.inner || [{ message: err.message, path: err.path }] || []
  for (const e of validationErrors) {
    if (e.path) {
      messages[e.path] = e.message
    }
  }
  const messagesAsUnknown = messages as unknown
  return messagesAsUnknown as T
}