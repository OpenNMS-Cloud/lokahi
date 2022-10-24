<template>
  <Transition name="fade">
    <TableCard v-if="appliancesStore.minionsTableOpen">
      <div class="header">
        <div class="title">
          Minions ({{ applianceQueries.tableMinions.length }})
        </div>
        <FeatherButton 
          data-test="hide-minions-btn"
          icon="Hide Minions" 
          @click="appliancesStore.hideMinionsTable"
          v-if="!widgetProps?.isWidget"
        >
          <FeatherIcon :icon="ChevronLeft" />
        </FeatherButton>
      </div>
      <div class="table-container">
        <table class="tl1 tl2 tc3 tc4 tc5 data-table" summary="Minions" data-test="minions-table">
          <thead>
            <tr>
              <th scope="col" data-test="col-date">Time</th>
              <th scope="col" data-test="col-minion">Name</th>
              <th scope="col" data-test="col-latency">Latency</th>
              <th scope="col" data-test="col-uptime">Uptime</th>
              <th scope="col" data-test="col-status">Status</th>
            </tr>
          </thead>
          <TransitionGroup name="data-table" tag="tbody">
            <tr v-for="(minion, index) in listMinionsWithBgColor" :key="(minion.id as string)" :data-index="index" data-test="minion-item">
              <td>{{ minion.lastUpdated }}</td>
              <td>{{ minion.id }}</td>
              <td>
                <div :class="minion.latencyBgColor">
                  {{ formatLatencyDisplay(minion.icmp_latency) }}
                </div>
              </td>
              <td>
                <div :class="minion.latencyBgColor">
                  {{ getHumanReadableDuration(minion.snmp_uptime) }}
                </div>
              </td>
              <td>
                <div :class="minion.statusBgColor" data-test="minion-item-status">
                  {{ minion.status }}
                </div>
              </td>
            </tr>
          </TransitionGroup>
        </table>
      </div>
    </TableCard>
  </Transition>
</template>

<script setup lang="ts">
import { useAppliancesQueries } from '@/store/Queries/appliancesQueries'
import { useAppliancesStore } from '@/store/Views/appliancesStore'
import ChevronLeft from '@featherds/icon/navigation/ChevronLeft'
import { ExtendedMinionDTOWithBGColors } from '@/types/minion'
import { ComputedRef } from 'vue'
import { formatItemBgColor, getHumanReadableDuration, formatLatencyDisplay } from './appliances.helpers'
import { WidgetProps } from '@/types'

defineProps<{widgetProps?: WidgetProps}>()

const appliancesStore = useAppliancesStore()
const applianceQueries = useAppliancesQueries()
const listMinionsWithBgColor: ComputedRef<ExtendedMinionDTOWithBGColors[]> = computed<any[]>(() => formatItemBgColor(applianceQueries.tableMinions))
</script>

<style lang="scss" scoped>
@import "@featherds/table/scss/table";
@import "@featherds/styles/mixins/typography";

.header {
  display: flex;
  justify-content: space-between;
  .title {
    @include headline3;
    margin-left: 15px;
  }
}

.table-container {
  display: block;
  overflow-x: auto;

  table {
  @include table-condensed();

    thead {
      background: var($background);
      text-transform: uppercase;
    }

    td {
      white-space: nowrap;
      div {
        border-radius: 5px;
        padding: 0px 5px 0px 5px;
      }
    }
  }
}

// hide / show transition
.fade-enter-active {
  transition: all 0.2s;
}
.fade-leave-active {
  transition: all 0.2s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
