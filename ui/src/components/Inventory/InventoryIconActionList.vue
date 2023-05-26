<template>
  <ul class="icon-action-list">
    <li
      v-if="isMonitored(node)"
      @click="onLineChart"
      data-test="line-chart"
      class="pointer"
    >
      <Icon :icon="lineChartIcon" />
    </li>
    <li
      @click="onWarning"
      data-test="warning"
      class="pointer"
    >
      <Icon :icon="warningIcon" />
    </li>
    <li
      @click="onDelete"
      data-test="delete"
    >
      <Icon :icon="deleteIcon" />
    </li>
  </ul>
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

<script lang="ts" setup>
import MultilineChart from '@material-design-icons/svg/outlined/multiline_chart.svg'
import Warning from '@featherds/icon/notification/Warning'
import Delete from '@featherds/icon/action/Delete'
import { IIcon, MonitoredNode, UnmonitoredNode, DetectedNode } from '@/types'
import { ModalPrimary } from '@/types/modal'
import useSnackbar from '@/composables/useSnackbar'
import useModal from '@/composables/useModal'
import { useInventoryQueries } from '@/store/Queries/inventoryQueries'
import { useNodeMutations } from '@/store/Mutations/nodeMutations'
import { isMonitored } from './inventory.utils'

const { showSnackbar } = useSnackbar()
const { openModal, closeModal, isVisible } = useModal()
const inventoryQueries = useInventoryQueries()
const nodeMutations = useNodeMutations()

const router = useRouter()
const props = defineProps<{ node: MonitoredNode | UnmonitoredNode | DetectedNode }>()

const onLineChart = () => {
  router.push({
    name: 'Graphs',
    params: { id: props.node.id }
  })
}
const lineChartIcon: IIcon = {
  image: MultilineChart,
  tooltip: 'Graphs',
  size: 1.5
}

const onWarning = () => {
  router.push({
    name: 'Node',
    params: { id: props.node.id }
  })
}
const warningIcon: IIcon = {
  image: markRaw(Warning),
  tooltip: 'Events/Alarms',
  size: 1.5
}

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
  const deleteNode = await nodeMutations.deleteNode({ id: modal.value.id })

  if (!deleteNode.error) {
    closeModal()
    showSnackbar({
      msg: 'Node successfully deleted.'
    })
    // Timeout because minion may not be available right away
    // TODO: Replace timeout with websocket/polling
    setTimeout(() => {
      inventoryQueries.fetchByState(props.node.type)
    }, 350)
  }
}
const onDelete = () => {
  modal.value = {
    ...modal.value,
    title: props.node.label || '',
    cssClass: 'modal-delete',
    content: 'Do you want to delete?',
    id: props.node.id
  }

  openModal()
}

const deleteIcon: IIcon = {
  image: markRaw(Delete),
  tooltip: 'Delete',
  size: 1.5
}
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';

ul.icon-action-list {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  > li {
    padding: var(variables.$spacing-xxs);
    font-size: 1.5rem;
    color: var(variables.$secondary-text-on-surface);
    &:hover {
      color: var(variables.$disabled-text-on-surface);
    }
  }
}
</style>
