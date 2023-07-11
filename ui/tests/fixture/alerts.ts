import { alert, alertsList } from './data/alerts'
import { Alert } from '@/types/graphql'

export const getAlert = (): Alert => alert
export const getAlertsList = (): Alert[] => alertsList
