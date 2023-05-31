<template>
  <div
    class="locations-card-wrapper"
    :class="{ selected: selectedCard }"
  >
    <div class="name">
      <ButtonTextIcon
        @click="locationStore.selectLocation(location.id)"
        :item="nameBtn"
        data-test="name"
      />
    </div>
    <PillColor
      :item="statusPill"
      class="status"
      data-test="status"
    />
    <div class="expiry">
      <FeatherIcon
        :icon="icons.cert"
        data-test="icon-expiry"
      />
    </div>
    <div class="context-menu">
      <HoverMenu
        :items="contextMenuItems"
        data-test="context-menu"
      />
    </div>
  </div>
</template>

<script lang="ts" setup>
import Cert from '@featherds/icon/communication/Certificate'
import { IButtonTextIcon } from '@/types'
import { Severity } from '@/types/graphql'
import { LocationTemp } from '@/types/locations.d'
import { useLocationStore } from '@/store/Views/locationStore'

const props = defineProps<{
  item: LocationTemp
}>()

const locationStore = useLocationStore()

const location = computed(() => props.item)

const selectedCard = computed(() => locationStore.selectedLocationId === props.item.id)

const nameBtn = computed<IButtonTextIcon>(() => ({
  label: props.item.location
}))

const statusPill = {
  label: props.item.status,
  style: props.item.status === 'UP' ? Severity.Normal : Severity.Critical
}

const contextMenuItems = [
  { label: 'Edit', handler: () => locationStore.selectLocation(props.item.id) },
  { label: 'Delete', handler: () => locationStore.deleteLocation(props.item.id) }
]

const icons = markRaw({
  cert: Cert
})
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';

.locations-card-wrapper {
  display: flex;
  align-items: center;
  gap: var(variables.$spacing-s);
  padding: 0 var(variables.$spacing-s);
}

.name {
  width: 40%;
}
.status {
  width: 30%;
  display: flex;
  justify-content: center;
}
.expiry {
  font-size: 20px;
  width: 15%;
  display: flex;
  justify-content: center;
}
.context-menu {
  width: 7%;
  display: flex;
  justify-content: flex-end;
}

.selected {
  background-color: var(variables.$shade-4);
}
</style>
