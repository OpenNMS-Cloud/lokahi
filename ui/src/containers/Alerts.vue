<template>
  <div class="container">
    <div class="content">
      <div class="header">
        <HeadlinePage
          text="Alerts"
          data-test="headline"
        />
        <FeatherButton
          secondary
          @click="alertsStore.clearAllFilters"
          data-test="clear-all-filters-btn"
          >clear all filters</FeatherButton
        >
      </div>
      <AlertsSeverityFilters
        data-test="severity-filters"
        isFilter
      />
      <div class="alerts-content">
        <div class="time-search-filters">
          <div
            class="time-filters"
            data-test="time-filters"
          >
            <span
              @click="alertsStore.selectTime(TimeRange.All)"
              :class="{ selected: alertsStore.alertsFilter.timeRange === TimeRange.All }"
              >All</span
            >
            <span
              @click="alertsStore.selectTime(TimeRange.Today)"
              :class="{ selected: alertsStore.alertsFilter.timeRange === TimeRange.Today }"
              >Today</span
            >
            <span
              @click="alertsStore.selectTime(TimeRange.Last_24Hours)"
              :class="{ selected: alertsStore.alertsFilter.timeRange === TimeRange.Last_24Hours }"
              >24H</span
            >
            <span
              @click="alertsStore.selectTime(TimeRange.SevenDays)"
              :class="{ selected: alertsStore.alertsFilter.timeRange === TimeRange.SevenDays }"
              >7D</span
            >
          </div>
          <div class="search-filter">
            <FeatherInput
              v-model="alertsStore.alertsFilter.nodeLabel"
              label="Search Alerts"
              type="search"
              @update:model-value="onSearchAlerts"
              class="search-alerts-input"
              data-test="search-filter"
            />
          </div>
        </div>
        <div class="alerts-list">
          <div class="card-list-top">
            <div class="select-all-checkbox-btns">
              <FeatherCheckbox
                v-model="isAllAlertsSelected"
                @update:model-value="onSelectAllAlertsCheck"
                :disabled="isAlertsListEmpty"
                data-test="select-all-checkbox"
                >Select All</FeatherCheckbox
              >
              <FeatherButton
                v-if="atLeastOneAlertSelected"
                text
                @click="onClearAlerts"
                data-test="clear-btn"
                >clear</FeatherButton
              >
              <FeatherButton
                v-if="atLeastOneAlertSelected"
                text
                @click="onAcknowledgeAlerts"
                data-test="acknowledge-btn"
                >acknowledge</FeatherButton
              >
            </div>
            <FeatherPagination
              v-if="!isAlertsListEmpty"
              v-model="page"
              :pageSize="pageSize"
              :total="total"
              data-test="list-count"
            />
          </div>
          <Spinner />
          <AlertsCardList
            :alerts="alerts"
            @alertSelected="onAlertSelected"
            data-test="alerts-list"
          />
          <div
            class="card-list-bottom"
            v-if="!isAlertsListEmpty"
          >
            <FeatherPagination
              v-model="page"
              :pageSize="pageSize"
              :total="total"
              :pageSizes="[10, 20, 50]"
              @update:model-value="onPageChanged"
              @update:pageSize="onPageSizeChanged"
              data-test="pagination"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import useSpinner from '@/composables/useSpinner'
import { useAlertsStore } from '@/store/Views/alertsStore'
import { fncArgVoid } from '@/types'
import { IAlert } from '@/types/alerts'
import { TimeRange } from '@/types/graphql'

const alertsStore = useAlertsStore()
const { startSpinner, stopSpinner } = useSpinner()

// current page of alert data (and selection status) to display in the table
const alerts = ref([] as IAlert[])
const isAllAlertsSelected = ref(false)

const isAlertsListEmpty = computed(() => alertsStore.isAlertsListEmpty)
const page = computed(() => alertsStore.alertsPagination.page || 1)
const pageSize = computed(() => alertsStore.alertsPagination.pageSize)
const atLeastOneAlertSelected = computed(() => alerts.value.some((a: IAlert) => a.isSelected))
const total = computed(() => alertsStore.alertsPagination.total)

// For now, we clear all selected items when the user pages. Need to decide on exact selection behavior.
// This behavior ensures only visible items are selected, so the user is not surprised
// when selected but not visible items are cleared/acked.
const onPageChanged = (p: number) => {
  startSpinner()
  alertsStore.setAllAlertsSelected(false)
  alertsStore.setPage(p)
}

const onPageSizeChanged = (p: number) => {
  startSpinner()
  alertsStore.setAllAlertsSelected(false)
  alertsStore.setPageSize(p)
}

const onSelectAllAlertsCheck = (isSelected: boolean | undefined) => {
  alerts.value = alerts.value.map((a: IAlert) => ({
    ...a,
    isSelected: isSelected || false
  }))

  alertsStore.setAllAlertsSelected(isSelected || false)
}

const onAlertSelected = (id: number) => {
  let newSelectedValue = false

  alerts.value = alerts.value.map((a: IAlert) => {
    if (a.databaseId === id) {
      a.isSelected = !a.isSelected // toggle selection
      newSelectedValue = a.isSelected
    }

    return a
  })

  isAllAlertsSelected.value = alerts.value.every(({ isSelected }) => isSelected)

  alertsStore.setAlertSelected(id, newSelectedValue)
}

const onClearAlerts = async () => {
  await alertsStore.clearSelectedAlerts()

  alertsStore.setAllAlertsSelected(false)
  isAllAlertsSelected.value = false
}

const onAcknowledgeAlerts = async () => {
  await alertsStore.acknowledgeSelectedAlerts()

  alertsStore.setAllAlertsSelected(false)
  isAllAlertsSelected.value = false
}

const onSearchAlerts: fncArgVoid = (val: string | null) => {
  if (val) {
    alertsStore.alertsFilter.nodeLabel = val
  } else {
    alertsStore.alertsFilter.nodeLabel = ''
  }
  alertsStore.resetPaginationAndFetchAlerts()
}

watch(() => [alertsStore.alertsPagination, alertsStore.alertsList], () => {
  // refresh our alert data when the store is updated, adding selection state
  alerts.value = alertsStore.alertsList?.alerts?.map((a: IAlert) => ({ ...a, isSelected: alertsStore.isAlertSelected(a.databaseId) })) || []
  isAllAlertsSelected.value = alerts.value.every(({ isSelected }) => isSelected === true)
  stopSpinner()
})

onMounted(() => {
  stopSpinner()
})

onBeforeRouteLeave(() => {
  stopSpinner()
})
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@/styles/vars.scss';

.container {
  display: flex;
  justify-content: center;
}

.content {
  width: vars.$max-width-constrained;
  margin-right: var(variables.$spacing-l);
  margin-left: var(variables.$spacing-l);
}

.header {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
}

.time-search-filters {
  width: 100%;
  display: flex;
  flex-direction: row;
  justify-content: space-between;
}

.time-filters {
  display: flex;
  flex-direction: row;
  align-items: center;
  border-radius: vars.$border-radius-xs;
  border: 1px solid var(variables.$border-on-surface);
  background-color: var(variables.$background);
  height: 2.5rem;
  padding: 3px;
  > span {
    padding: 4px 15px;
    border-right: 1px solid var(variables.$border-on-surface);
    &:last-child {
      border-right: none;
    }
    &.selected {
      border-right-color: var(variables.$background);
      background-color: var(variables.$border-on-surface);
    }
    &:hover {
      cursor: pointer;
    }
  }
}

.search-filter {
  width: 30%;
  .search-alerts-input {
    width: 100%;
    :deep(.feather-input-border) {
      .pre-border,
      .label-border,
      .post-border {
        border-color: var(variables.$border-on-surface);
      }
    }
  }
}

.alerts-content {
  background: var(variables.$surface);
  padding: var(variables.$spacing-l);
  border-radius: vars.$border-radius-surface;
  border: 1px solid var(variables.$border-on-surface);
}

.alerts-list {
  border: 1px solid var(variables.$border-on-surface);
  border-radius: vars.$border-radius-s;
}

.card-list-top {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  padding: var(variables.$spacing-s) var(variables.$spacing-xl);
  background-color: var(variables.$background);
  border-bottom: 1px solid var(variables.$border-on-surface);
  :deep(.layout-container) {
    margin-bottom: 0;
  }
  :deep(.feather-pagination) {
    border: 0;
    min-height: auto;
    padding-left: 0;
    > .range-text {
      margin-right: 0;
      min-width: auto;
    }
    > .per-page-text,
    > .page-size-select,
    > nav {
      display: none;
    }
  }
}

.select-all-checkbox-btns {
  display: flex;
  flex-direction: row;
  > button {
    margin-left: var(variables.$spacing-xl);
  }
}

.card-list-bottom {
  display: flex;
  flex-direction: row;
  justify-content: flex-end;
  align-items: center;
  padding: var(variables.$spacing-s) 0 var(variables.$spacing-s) var(variables.$spacing-xl);
  > * {
    margin-left: var(variables.$spacing-xl);
  }
  :deep(> .feather-pagination) {
    border: 0;
    min-height: auto;
  }
}
</style>
