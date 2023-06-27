import { validationErrorsToStringRecord } from '@/services/validationService'
import { ValidationError } from 'yup'


describe('Validation Service Tests', () => {
    test('The Validation Service properly transforms an error message', () => {
        const errors = validationErrorsToStringRecord({ inner: [{ path: 'name', message: 'error with name' }] } as ValidationError)
        expect(errors).toEqual({ name: 'error with name' })
    })
});
