<template>
  <TableCard v-if="appliancesStore.minionsTableOpen">
    <div class="header">
      <div class="title">Minions ({{ appliancesQueries.tableMinions.length }})</div>
      <FeatherButton
        data-test="hide-minions-btn"
        icon="Hide Minions"
        @click="appliancesStore.hideMinionsTable"
      >
        <FeatherIcon :icon="ChevronLeft" />
      </FeatherButton>
    </div>
    <div class="container">
      <table
        class="tl1 tl2 tl3 tc4 tc5 tc6 data-table"
        aria-label="Minions Table"
        data-test="minions-table"
      >
        <thead>
          <tr>
            <th
              scope="col"
              data-test="col-label"
            >
              Label
            </th>
            <th
              scope="col"
              data-test="col-date"
            >
              Time
            </th>
            <th
              scope="col"
              data-test="col-minion"
            >
              Id
            </th>
            <th
              scope="col"
              data-test="col-latency"
            >
              Latency
            </th>
            <th
              scope="col"
              data-test="col-status"
            >
              Status
            </th>
            <th
              v-if="isAnyMinionDown"
              scope="col"
              data-test="col-delete"
            >
              Delete
            </th>
          </tr>
        </thead>
        <TransitionGroup
          name="data-table"
          tag="tbody"
        >
          <tr
            v-for="(minion, index) in minionsTable"
            :key="(minion.id as string)"
            :data-index="index"
            data-test="minion-item"
          >
            <td>{{ minion.label }}</td>
            <td v-date>{{ minion.lastCheckedTime }}</td>
            <td>{{ minion.id }}</td>
            <MetricChip
              tag="td"
              :metric="{ value: minion.latency?.value }"
              :data-metric="minion.latency?.value"
              class="bg-status"
              data-test="minion-item-latency"
            />
            <MetricChip
              tag="td"
              :metric="{ status: minion.status }"
              class="bg-status"
              data-test="minion-item-status"
            />
            <td v-if="minion.status === 'DOWN'">
              <FeatherButton
                icon="Delete"
                @click="onDelete(minion.systemId as string, minion.label as string)"
                data-test="minion-item-delete-btn"
              >
                <FeatherIcon :icon="deleteIcon" />
              </FeatherButton>
            </td>
          </tr>
        </TransitionGroup>
      </table>
    </div>
  </TableCard>
  <PrimaryModal
    :visible="isVisible"
    :title="modal.title"
    :class="modal.cssClass"
  >
    <template #content>
      <p>{{ modal.content }}</p>
    </template>
    <template #footer>
      <FeatherButton
        data-testid="cancel-btn"
        secondary
        @click="closeModal"
      >
        {{ modal.cancelLabel }}
      </FeatherButton>
      <FeatherButton
        data-testid="save-btn"
        primary
        @click="deleteHandler"
      >
        {{ modal.saveLabel }}
      </FeatherButton>
    </template>
  </PrimaryModal>
</template>

<script setup lang="ts">
import { useAppliancesQueries } from '@/store/Queries/appliancesQueries'
import { useAppliancesStore } from '@/store/Views/appliancesStore'
import ChevronLeft from '@featherds/icon/navigation/ChevronLeft'
import Delete from '@featherds/icon/action/Delete'
import { Monitor } from '@/types'
import { ModalPrimary } from '@/types/modal'
import { GraphProps } from '@/types/graphs'
import { ExtendedMinion } from '@/types/minion'
import { TimeRangeUnit } from '@/types/graphql'
import MetricChip from '../Common/MetricChip.vue'
import useSnackbar from '@/composables/useSnackbar'
import useModal from '@/composables/useModal'
import { useMinionMutations } from '@/store/Mutations/minionMutations'

const { showSnackbar } = useSnackbar()
const { openModal, closeModal, isVisible } = useModal()

const deleteIcon = markRaw(Delete)

const graphProps = ref({} as GraphProps)

const appliancesStore = useAppliancesStore()
const appliancesQueries = useAppliancesQueries()
const minionMutations = useMinionMutations()

const isAnyMinionDown = ref(false)
const minionsTable = computed<ExtendedMinion[]>(() => {
  const minions = appliancesQueries.tableMinions

  isAnyMinionDown.value = minions.some(({ status }) => status === 'DOWN')

  return minions
})

const modal = ref<ModalPrimary>({
  title: '',
  cssClass: '',
  content: '',
  id: '',
  cancelLabel: 'cancel',
  saveLabel: 'delete',
  hideTitle: true
})

const deleteHandler = async () => {
  const deleteMinion = await minionMutations.deleteMinion({ id: modal.value.id })

  if (!deleteMinion.error) {
    closeModal()
    showSnackbar({
      msg: 'Minion successfully deleted.'
    })
    // Timeout because minion may not be available right away
    // TODO: Replace timeout with websocket/polling
    setTimeout(() => {
      appliancesQueries.fetchMinionsForTable()
    }, 350)
  }
}
const onDelete = (id: string, label: string) => {
  modal.value = {
    ...modal.value,
    title: label || '',
    cssClass: 'modal-delete',
    content: 'Do you want to delete?',
    id
  }

  openModal()
}

const openLatencyGraph = (minion: ExtendedMinion) => {
  graphProps.value = {
    label: 'Minion Latency',
    metrics: ['response_time_msec'],
    monitor: Monitor.ECHO,
    nodeId: minion.id,
    instance: minion.systemId as string, // for minions, can use systemId for instance
    timeRange: 10,
    timeRangeUnit: TimeRangeUnit.Minute
  }
}
</script>

<style lang="scss" scoped>
@use '@featherds/styles/mixins/typography';
@use '@featherds/table/scss/table';
@use '@/styles/_statusBackground';

.header {
  display: flex;
  justify-content: space-between;
  .title {
    @include typography.headline3;
    margin-left: 15px;
  }
}

.container {
  display: block;
  overflow-x: auto;

  table {
    width: 100%;
    @include table.table;
    @include table.table-condensed;
    thead {
      background: var(typography.$background);
      text-transform: uppercase;
    }
    td {
      white-space: nowrap;
      display: table-cell;
      div {
        border-radius: 5px;
        padding: 0px 5px 0px 5px;
      }
    }
  }
}
</style>
