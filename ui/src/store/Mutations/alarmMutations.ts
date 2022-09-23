import { defineStore } from 'pinia'
import { useMutation } from 'villus'
import { ClearAlarmDocument } from '@/types/graphql'

export const useAlarmMutations = defineStore('alarmMutations', () => {
  const {
    execute: clearAlarm
  } = useMutation(ClearAlarmDocument)

  return {
    clearAlarm
  }
})
