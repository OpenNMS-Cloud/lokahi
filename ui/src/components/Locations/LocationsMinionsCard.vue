<template>
  <div class="minions-card-wrapper">
    <div class="header-content">
      <div class="header">
        <div
          class="name"
          data-test="header-name"
        >
          {{ minion.label }}
        </div>
        <div
          class="latency"
          data-test="header-latency"
        >
          Latency
        </div>
        <div
          class="status"
          data-test="header-status"
        >
          Status
        </div>
      </div>
      <div class="content">
        <div
          class="version"
          data-test="content-version"
        >
        </div>
        <div
          class="latency"
          data-test="content-latency"
        >
          <PillColor :item="latencyPill" />
        </div>
        <div
          class="status"
          data-test="content-status"
        >
          <PillColor :item="statusPill" />
        </div>
      </div>
    </div>
    <MoreOptionsMenu
      :items="contextMenuItems"
      data-test="context-menu"
    />
  </div>
  <DeleteConfirmationModal
    :isVisible="isVisible"
    :name="minion.label!"
    :closeModal="() => closeModal()"
    :deleteHandler="() => deleteMinion()"
    :isDeleting="minionMutations.isDeletingMinion"
  />
</template>

<script setup lang="ts">
import { Severity } from '@/types/graphql'
import { useMinionMutations } from '@/store/Mutations/minionMutations'
import { useLocationStore } from '@/store/Views/locationStore'
import { useMinionsQueries } from '@/store/Queries/minionsQueries'
import useModal from '@/composables/useModal'

const minionMutations = useMinionMutations()
const locationStore = useLocationStore()
const minionsQueries = useMinionsQueries()
const { openModal, closeModal, isVisible } = useModal()

const props = defineProps({
  item: {
    type: Object,
    required: true
  }
})

const latencyThreshold = (latency: number) => {
  let type = Severity.Warning

  if (latency < 200) type = Severity.Normal
  else if (latency > 500) type = Severity.Critical

  return type
}

let statusPill = reactive({} as Pill)
let latencyPill = reactive({} as Pill)
let ipPill = reactive({} as Pill)

const minion = computed(() => {
  statusPill = {
    label: props.item.status,
    style: props.item.status === 'UP' ? Severity.Normal : Severity.Critical
  }

  latencyPill = {
    label: `${props.item.latency?.value || 0}ms`, //Latency is not on this Object
    style: latencyThreshold(props.item.latency?.value || 0) //.match(/\d+/g))
  }

  ipPill = {
    label: props.item.ip || '1.1.1.1',
    style: Severity.Cleared
  }

  return props.item
})

const deleteMinion = async () => {
  await minionMutations.deleteMinion({ id: props.item.id })
  await minionsQueries.refreshMinionsById()
  locationStore.fetchLocations() // location may be gone if last minion deleted
}

const contextMenuItems = [{ label: 'Delete', handler: () => openModal() }]

type Pill = {
  style: string
  label?: string
}
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@/styles/vars.scss';

.minions-card-wrapper {
  display: flex;
  align-items: center;
  gap: 2%;
  border: 1px solid var(variables.$border-on-surface);
  border-radius: vars.$border-radius-xs;
  padding: var(variables.$spacing-s);

  .header-content {
    width: 96%;

    .header,
    .content {
      display: flex;
      align-items: center;
      .name {
        width: 50%;
        font-weight: bold;
      }
      .version {
        width: 50%;
      }
      .latency {
        width: 15%;
        display: flex;
        justify-content: center;
      }
      .status {
        width: 15%;
        display: flex;
        justify-content: center;
      }
      .utilization {
        width: 19%;
        display: flex;
        justify-content: center;
      }
      .ip {
        width: 26%;
      }
    }
  }

  .hover-menu {
    width: 5%;
  }
}
</style>
