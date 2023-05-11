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
              v-model="searchAlerts"
              label="Search Alerts"
              type="search"
              @update:model-value="searchAlertsListener"
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
                @update:model-value="allAlertsCheckboxHandler"
                :disabled="isAlertsListEmpty"
                data-test="select-all-checkbox"
                >Select All</FeatherCheckbox
              >
              <FeatherButton
                :disabled="!atLeastOneAlertSelected"
                text
                @click="clearAlertsHandler"
                data-test="clear-btn"
                >clear</FeatherButton
              >
              <FeatherButton
                :disabled="!atLeastOneAlertSelected"
                text
                @click="acknowledgeAlertsHandler"
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
          <AlertsCardList
            :alerts="alerts"
            @alert-selected="alertSelectedListener"
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
              @update:model-value="alertsStore.setPage"
              @update:pageSize="alertsStore.setPageSize"
              data-test="pagination"
              ref="refPagination"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { TimeRange } from '@/types/graphql'
import { useAlertsStore } from '@/store/Views/alertsStore'
import { IAlert } from '@/types/alerts'

onMounted(async () => {
  await alertsStore.fetchAlerts()
})

const alertsStore = useAlertsStore()

const refPagination = ref()

const alerts = ref([] as IAlert[])
watchEffect(() => {
  alerts.value = alertsStore.alertsList?.alerts?.map((a: IAlert) => ({ ...a, isSelected: false })) || []
})

const isAlertsListEmpty = computed(() => alertsStore.isAlertsListEmpty)
watchEffect(() => {
  if (alertsStore.gotoFirstPage && refPagination.value) {
    refPagination.value.first() // goto first page on 'severity' and/or 'time' filter change
    alertsStore.gotoFirstPage = false
  }
})

const page = alertsStore.alertsPagination.page
const pageSize = computed(() => alertsStore.alertsPagination.pageSize)
const total = computed(() => alertsStore.alertsPagination.total)

const atLeastOneAlertSelected = computed(() => alerts.value.some((a: IAlert) => a.isSelected))

const isAllAlertsSelected = ref(false)
const allAlertsCheckboxHandler = (isSelected: boolean | undefined) => {
  alerts.value = alerts.value.map((a: IAlert) => ({
    ...a,
    isSelected
  }))

  alertsStore.setAlertsSelected(isSelected as boolean)
}

const alertSelectedListener = (id: number) => {
  alerts.value = alerts.value.map((a: IAlert) => {
    if (a.databaseId === id) {
      a.isSelected = !a.isSelected // toggle selection
    }

    return a
  })

  isAllAlertsSelected.value = alerts.value.every(({ isSelected }) => isSelected)

  alertsStore.setAlertsSelected(id)
}

const clearAlertsHandler = () => {
  alertsStore.clearSelectedAlerts()

  isAllAlertsSelected.value = false
}

const acknowledgeAlertsHandler = () => {
  alertsStore.acknowledgeSelectedAlerts()

  isAllAlertsSelected.value = false
}

// TODO: search not avail for EAR
const searchAlerts = ref('')
const searchAlertsListener = (v: any) => {
  // need to define (with BE dev), when to send request, how many chars,...
}
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
