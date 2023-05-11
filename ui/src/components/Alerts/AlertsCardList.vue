<template>
  <div
    v-if="alerts?.length"
    data-test="alerts-list"
  >
    <AlertsCard
      v-for="alert in alerts"
      :key="alert.databaseId"
      :alert="alert"
      @alert-selected="emits('alert-selected', alert.databaseId)"
    />
  </div>
  <EmptyList
    v-else
    :content="emptyListContent"
    data-test="empty-list"
  />
</template>

<script lang="ts" setup>
import { useAlertsStore } from '@/store/Views/alertsStore'
import { IAlert } from '@/types/alerts'

defineProps<{
  alerts: IAlert[]
}>()

const emits = defineEmits(['alert-selected'])

const alertsStore = useAlertsStore()

const emptyListContent = {
  msg: 'No results found. Refine or reduce filter criteria.',
  btn: {
    label: 'clear all filters',
    action: alertsStore.clearAllFilters
  }
}
</script>
