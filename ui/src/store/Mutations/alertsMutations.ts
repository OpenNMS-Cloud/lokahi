import { defineStore } from 'pinia'
import { useMutation } from 'villus'
import { AcknowledgeAlertsDocument, ClearAlertsDocument } from '@/types/graphql'

export const useAlertsMutations = defineStore('alertsMutations', () => {
  const { execute: acknowledgeAlerts, error: acknowledgeAlertsError } = useMutation(AcknowledgeAlertsDocument)

  const { execute: clearAlerts, error: clearAlertsError } = useMutation(ClearAlertsDocument)

  return {
    acknowledgeAlerts,
    acknowledgeAlertsError,
    clearAlerts,
    clearAlertsError
  }
})
