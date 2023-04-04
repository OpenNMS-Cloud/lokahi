<template>
  <div
    v-if="alerts.length"
    data-test="alerts-list"
  >
    <AlertsCard
      v-for="alert in alerts"
      :key="alert.databaseId"
      :alert="alert"
      @alert-selected="emits('alert-selected', alert.databaseId)"
    />
  </div>
  <div
    v-else
    class="empty-list"
    data-test="empty-list"
  >
    <div data-test="msg">No results found. Refine or reduce filter criteria.</div>
    <FeatherButton
      secondary
      @click="alertsStore.clearAllFilters"
      data-test="clear-all-filters-btn"
      >clear all filters</FeatherButton
    >
  </div>
</template>

<script lang="ts" setup>
import { useAlertsStore } from '@/store/Views/alertsStore'
import { IAlert } from '@/types/alerts'

defineProps<{
  alerts: IAlert[]
}>()

const emits = defineEmits(['alert-selected'])

const alertsStore = useAlertsStore()
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@/styles/vars.scss';

.empty-list {
  display: flex;
  flex-direction: column;
  width: 100%;
  justify-content: center;
  align-items: center;
  height: 200px;
  border-width: 0 1px 1px;
  border-style: solid;
  border-color: var(variables.$border-on-surface);
  border-radius: 0 0 vars.$border-radius-s vars.$border-radius-s;
  > button {
    margin-top: var(variables.$spacing-l);
  }
}
</style>
