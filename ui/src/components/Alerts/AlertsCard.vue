<template>
  <div class="card">
    <FeatherCheckbox
      :model-value="alert.isSelected"
      @update:model-value="alertSelectedHandler(alert.databaseId)"
      data-test="checkbox"
    />
    <FeatherExpansionPanel>
      <template #title>
        <div class="content">
          <div class="name-node-type">
            <div
              class="name headline"
              data-test="name"
            >
              {{ alert.label || 'Unknown' }}
            </div>
            <div data-test="node-type">
              {{ alert.nodeType || 'Unknown' }}
            </div>
          </div>
          <div
            class="severity"
            data-test="severity"
          >
            <PillColor :item="pillColor" />
          </div>
          <div
            class="cause headline"
            data-test="cause"
          >
            <div>{{ alert.type }}</div>
            <div>&nbsp;</div>
          </div>
          <div
            class="duration headline"
            data-test="duration"
          >
            <div>{{ fnsFormatDistanceToNow(alert.lastUpdateTimeMs) }}</div>
            <div>&nbsp;</div>
          </div>
          <div class="date-time">
            <div
              class="date headline"
              data-test="date"
            >
              <span>{{ fnsFormat(alert.lastUpdateTimeMs, 'M/dd/yyyy') }}</span>
            </div>
            <div
              class="time"
              data-test="time"
            >
              <span>{{ fnsFormat(alert.lastUpdateTimeMs, 'HH:mm:ssxxx') }}</span>
            </div>
          </div>
          <div class="check-circle">
            <FeatherIcon
              :icon="checkCircleIcon"
              :class="{ acknowledged: alert.acknowledged }"
              class="acknowledged-icon"
              data-test="check-icon"
            />
          </div>
        </div>
      </template>
      <slot>
        <div
          class="description"
          data-test="description"
        >
          {{ alert.description }}
        </div>
      </slot>
    </FeatherExpansionPanel>
  </div>
</template>

<script lang="ts" setup>
import CheckCircle from '@featherds/icon/action/CheckCircle'
import { format as fnsFormat, formatDistanceToNow as fnsFormatDistanceToNow } from 'date-fns'
import { IAlert } from '@/types/alerts'

const emits = defineEmits(['alert-selected'])

const props = defineProps<{
  alert: IAlert
}>()

const pillColor = { style: props.alert.severity! }

const alertSelectedHandler = (databaseId: number) => {
  emits('alert-selected', databaseId)
}

const checkCircleIcon = markRaw(CheckCircle)
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';
@use '@/styles/vars.scss';
@use '@/styles/mixins.scss';

.headline {
  font-size: 1rem;
  font-weight: 600;
}

.card {
  min-width: 900px;
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  padding: var(variables.$spacing-m) var(variables.$spacing-xl) var(variables.$spacing-s) var(variables.$spacing-m);
  border-width: 0 1px 1px;
  border-style: solid;
  border-color: var(variables.$border-on-surface);
}

.feather-expansion {
  box-shadow: none;
  background-color: unset;
  :deep(.feather-expansion-header-button) {
    height: auto;
    padding: 0;
    &.expanded {
      height: auto;
    }
  }
  :deep(.panel-content) {
    padding: 1rem 0 0 0 !important;
    width: 90%;
  }
}

.content {
  width: 95%;
  display: flex;
  flex-direction: row;
  gap: 2%;
  justify-content: flex-start;
  align-items: center;

  .name-node-type {
    width: 22%;
    .name {
      @include mixins.truncate-text;
    }
  }

  .severity {
    width: 14%;
    text-align: center;
  }

  .cause {
    width: 20%;
    > div {
      @include mixins.truncate-text;
    }
  }

  .duration {
    width: 15%;
  }

  .date-time {
    width: 15%;
  }

  .check-circle {
    width: 4%;
    .acknowledged-icon {
      width: 1.5rem;
      height: 1.5rem;
      color: var(variables.$shade-3);
      &.acknowledged {
        color: var(variables.$success);
      }
    }
  }
}

:deep(> .layout-container) {
  margin: 0.4rem 1rem 0 0;
  display: flex;
  .feather-checkbox {
    margin: 0;
    label {
      display: none;
    }
  }
}
</style>
