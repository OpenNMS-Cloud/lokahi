<template>
  <div
    v-if="count && isFilter"
    @click="alertsStore.toggleSeverity(severity)"
    :class="{ selected: isTypeAdded }"
    class="card border pointer"
    data-test="severity-card"
  >
    <div class="label-add-icon">
      <PillColor
        :item="pillColor"
        data-test="severity-label"
      />
      <Transition name="icon-anim">
        <FeatherIcon
          :icon="isTypeAdded ? Cancel : Add"
          class="icon"
          focusable="false"
          data-test="add-cancel-icon"
        />
      </Transition>
    </div>
    <div
      class="count"
      data-test="count"
    >
      {{ count }}
    </div>
    <!-- <div data-test="percentage-time">
      <span class="percentage">%5</span>
      <span>Past 24 Hours</span>
    </div> -->
  </div>
  <div
    v-else
    class="card border pointer"
    data-test="severity-card"
  >
    <div class="label-add-icon">
      <PillColor
        :item="pillColor"
        data-test="severity-label"
      />
    </div>
    <div
      class="count"
      data-test="count"
    >
      {{ count || 0 }}
    </div>
  </div>
</template>

<script lang="ts" setup>
import Add from '@featherds/icon/action/Add'
import Cancel from '@featherds/icon/navigation/Cancel'
import { TimeRange } from '@/types/graphql'
import { useAlertsStore } from '@/store/Views/alertsStore'
import { useAlertsQueries } from '@/store/Queries/alertsQueries'

const alertsStore = useAlertsStore()
const alertsQueries = useAlertsQueries()

const props = defineProps<{
  severity: string
  isFilter?: boolean
  timeRange?: TimeRange
}>()

const pillColor = { style: props.severity as string }

const count = ref()
onMounted(async () => {
  try {
    const { data } = await alertsQueries.fetchCountAlerts([props.severity], props.timeRange || TimeRange.All)
    count.value = data.value?.countAlerts?.count || 0
  } catch (err) {
    count.value = 0
  }
})

const isTypeAdded = computed(() => alertsStore.alertsFilter.severities?.includes(props.severity))
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';
@use '@/styles/mediaQueriesMixins.scss';
@use '@/styles/vars.scss';

.card {
  display: flex;
  flex-direction: column;
  background-color: var(variables.$surface);
  padding: var(variables.$spacing-s);
  border-radius: vars.$border-radius-s;
  &.selected {
    background-color: var(variables.$shade-4);
    border-color: var(variables.$secondary-variant);
  }
}

.label-add-icon {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--feather-spacing-xl);
  > .icon {
    font-size: 24px;
  }
  > *:first-child {
    margin-right: var(variables.$spacing-l);
  }
}

.count {
  font-size: var(variables.$display3-font-size);
  margin-bottom: var(variables.$spacing-l);
  font-family: var(variables.$header-font-family);
  color: var(variables.$primary-text-on-surface);
}

.percentage {
  margin-right: var(variables.$spacing-xs);
}

.icon-anim-enter-active {
  transition: all 0.3s ease-out;
}

.icon-anim-leave-active {
  transition: all 0.8s cubic-bezier(1, 0.5, 0.8, 1);
}

.icon-anim-enter-from,
.icon-anim-leave-to {
  transform: rotateZ(90deg);
  opacity: 0;
}
</style>
