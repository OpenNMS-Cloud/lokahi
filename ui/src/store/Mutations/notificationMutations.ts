import { defineStore } from 'pinia'
import { useMutation } from 'villus'
import useSpinner from '@/composables/useSpinner'

import { SavePagerDutyConfigDocument } from '@/types/graphql'

const { startSpinner, stopSpinner } = useSpinner()

export const useNotificationMutations = defineStore('notificationMutations', () => {
  // send pager duty integration key
  const {
    execute: savePagerDutyIntegrationKey,
    isFetching,
    error
  } = useMutation(SavePagerDutyConfigDocument)

  watchEffect(() => {
    if (isFetching.value) {
      startSpinner()
    } else {
      stopSpinner()
    }
  })

  return {
    savePagerDutyIntegrationKey,
    error
  }
})
