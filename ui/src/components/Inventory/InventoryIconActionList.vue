<template>
  <ul class="icon-action-list">
    <!-- <li @click="onBubbleChart" data-test="bubble-chart"><Icon :icon="bubbleChartIcon" /></li> -->
    <li @click="onLineChart" data-test="line-chart" class="pointer"><Icon :icon="lineChartIcon" /></li>
    <!-- <li @click="onPieChart" data-test="pie-chart"><Icon :icon="pieChartIcon" /></li> -->
    <li @click="onWarning" data-test="warning" class="pointer"><Icon :icon="warningIcon" /></li>
    <li @click="onDelete" data-test="delete"><Icon :icon="deleteIcon" /></li>
  </ul>
  <PrimaryModal :visible="isVisible" :title="modal.title" :class="modal.cssClass">
    <template #content>
      <p>{{ modal.content }}</p>
    </template>
    <template #footer>
      <FeatherButton 
        data-testid="cancel-btn" 
        secondary 
        @click="closeModal">
          {{ modal.cancelLabel }}
      </FeatherButton>
      <FeatherButton 
        data-testid="save-btn" 
        primary
        @click="deleteHandler">
          {{ modal.saveLabel }}
      </FeatherButton>
    </template>
  </PrimaryModal>
</template>

<script lang="ts" setup>
import BubbleChart from '@material-design-icons/svg/outlined/bubble_chart.svg'
import MultilineChart from '@material-design-icons/svg/outlined/multiline_chart.svg'
import PieChart from '@material-design-icons/svg/outlined/pie_chart.svg'
import Warning from '@featherds/icon/notification/Warning'
import Delete from '@featherds/icon/action/Delete'
import { IIcon } from '@/types'
import { ModalPrimary } from '@/types/modal'
import { NodeContent } from '@/types/inventory'
import useSnackbar from '@/composables/useSnackbar'
import useModal from '@/composables/useModal'
import { useInventoryQueries } from '@/store/Queries/inventoryQueries'
import { useNodeMutations } from '@/store/Mutations/nodeMutations'

const { showSnackbar } = useSnackbar()
const { openModal, closeModal, isVisible } = useModal()
const inventoryQueries = useInventoryQueries()
const nodeMutations = useNodeMutations()

const router = useRouter()
const props = defineProps<{ node: NodeContent}>()

const onBubbleChart = () => {
  console.log('bubble chart')
}
const bubbleChartIcon: IIcon = {
  image: BubbleChart,
  title: 'Bubble Chart'
}

const onLineChart = () => {
  router.push({ 
    name: 'Graphs', 
    params: { id: props.node.id } 
  })
}
const lineChartIcon: IIcon = {
  image: MultilineChart,
  tooltip: 'Graphs'
}

const onPieChart = () => {
  console.log('pie chart')
}
const pieChartIcon: IIcon = {
  image: PieChart,
  title: 'Pie Chart'
}

const onWarning = () => {
  router.push({ 
    name: 'Node', 
    params: { id: props.node.id } 
  })
}
const warningIcon: IIcon = {
  image: markRaw(Warning),
  tooltip: 'Events/Alarms'
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
  const deleteNode = await nodeMutations.deleteNode({id: modal.value.id})

  if (!deleteNode.error) {
    closeModal()
    showSnackbar({
      msg: 'Node successfully deleted.'
    })
    // Timeout because minion may not be available right away
    // TODO: Replace timeout with websocket/polling
    setTimeout(() => {
      inventoryQueries.fetch()
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
  tooltip: 'Delete'
}
</script>

<style lang="scss" scoped>
@use "@featherds/styles/themes/variables";

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