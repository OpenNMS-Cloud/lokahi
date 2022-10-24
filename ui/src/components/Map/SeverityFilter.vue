<template>
  <FeatherSelect
    class="severity-select"
    v-model="selectedSeverity"
    :options="options"
    text-prop="option"
    @update:modelValue="onSeveritySelect"
    label="Show Severity >="
  />
</template>

<script
  setup
  lang="ts"
>
import { useMapStore } from '@/store/Views/mapStore'
import { FeatherSelect } from '@featherds/select'

const mapStore = useMapStore()

const options = [
  { id: 'NORMAL', option: 'Normal' },
  { id: 'WARNING', option: 'Warning' },
  { id: 'MINOR', option: 'Minor' },
  { id: 'MAJOR', option: 'Major' },
  { id: 'CRITICAL', option: 'Critical' }
]
const selectedSeverity = ref(options[0])

const onSeveritySelect = () => mapStore.selectedSeverity = selectedSeverity.value.id

// positioning the severity element in the header bar
const elemWidth = 250
const headerContentLeftWidth = document.querySelector('.left.center-horiz')?.clientWidth || 0
const elemPosRight = `${headerContentLeftWidth - elemWidth}px`
</script>

<style lang="scss">
@import "@featherds/styles/themes/variables";

.severity-select {
  position: absolute;
  width: v-bind(elemWidth);
  right: v-bind(elemPosRight);
  top: 11px;
  z-index: var($zindex-tooltip);
  .feather-input-wrapper {
    background: var($primary-text-on-color);
    border: 2px solid var($secondary);
  }
  .feather-input-label {
    border: 2px solid var($secondary);
    border-bottom: none;
  }
}
</style>

