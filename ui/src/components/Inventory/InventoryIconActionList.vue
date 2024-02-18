<template>
  <ul :class="[`icon-action-list`,className]">
    <li v-if="node.monitoredState === 'MONITORED'" @click="onLineChart" data-test="line-chart" class="pointer">
      <Icon :icon="lineChartIcon" />
    </li>
    <li @click="onNodeStatus" data-test="node-status" class="pointer">
      <Icon :icon="infoIcon" />
    </li>
    <li @click="onWarning" data-test="warning" class="pointer">
      <Icon :icon="warningIcon" />
    </li>
    <li @click="onDelete" data-test="delete">
      <Icon :icon="deleteIcon" />
    </li>
  </ul>
  <PrimaryModal :visible="isVisible" :title="modal.title" :class="modal.cssClass">
    <template #content>
      <p>{{ modal.content }}</p>
    </template>
    <template #footer>
      <FeatherButton data-testid="cancel-btn" secondary @click="closeModal">
        {{ modal.cancelLabel }}
      </FeatherButton>
      <FeatherButton data-testid="save-btn" primary @click="deleteHandler">
        {{ modal.saveLabel }}
      </FeatherButton>
    </template>
  </PrimaryModal>
</template>

<script lang="ts" setup>
import Warning from '@featherds/icon/notification/Warning'
import Delete from '@featherds/icon/action/Delete'
import Info from '@featherds/icon/action/Info'
import GraphIcon from '@/components/Common/GraphIcon.vue'
import { IIcon, InventoryItem } from '@/types'
import { ModalPrimary } from '@/types/modal'
import useSnackbar from '@/composables/useSnackbar'
import useModal from '@/composables/useModal'
import { useInventoryQueries } from '@/store/Queries/inventoryQueries'
import { useNodeMutations } from '@/store/Mutations/nodeMutations'
import { PropType } from 'vue'

const { showSnackbar } = useSnackbar()
const { openModal, closeModal, isVisible } = useModal()
const inventoryQueries = useInventoryQueries()
const nodeMutations = useNodeMutations()

const router = useRouter()
const props = defineProps({
  node: {type:Object as PropType<InventoryItem>, default: () => ({})},
  className:{type: String, default: ''},
  ['data-test']:{type: String, default: ''}
})

const onLineChart = () => {
  router.push({
    name: 'Graphs',
    params: { id: props.node.id }
  })
}
const lineChartIcon: IIcon = {
  image: markRaw(GraphIcon),
  tooltip: 'Graphs',
  size: 1.5,
  cursorHover: true
}

const onNodeStatus = () => {
  router.push({
    name: 'Node Status',
    params: { id: props.node.id }
  })
}
const infoIcon: IIcon = {
  image: markRaw(Info),
  tooltip: 'Node Status',
  size: 1.5,
  cursorHover: true
}

const onWarning = () => {
  router.push({
    name: 'Node',
    params: { id: props.node.id }
  })
}
const warningIcon: IIcon = {
  image: markRaw(Warning),
  tooltip: 'Node Details',
  size: 1.5,
  cursorHover: true
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
      inventoryQueries.buildNetworkInventory()
    }, 350)
  }
}
const onDelete = () => {
  modal.value = {
    ...modal.value,
    title: props.node.nodeLabel || '',
    cssClass: 'modal-delete',
    content: 'Do you want to delete?',
    id: props.node.id
  }

  openModal()
}

const deleteIcon: IIcon = {
  image: markRaw(Delete),
  tooltip: 'Delete',
  cursorHover: true,
  size: 1.5
}
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';

ul.icon-action-list {
  display: flex;
  gap: 0.2rem;

  li {
    padding: var(variables.$spacing-xxs);
    font-size: 1.5rem;
    color: var(variables.$primary);
    cursor: pointer;

    &:hover {
      cursor: pointer;
      color: var(variables.$disabled-text-on-surface);
    }
  }
}
</style>
