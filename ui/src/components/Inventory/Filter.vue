<template>
  <ul class="filter-container">
    <li class="autocomplete">
      <!-- Search -->
      <FeatherAutocomplete
        v-model="search.value"
        :loading="search.loading"
        :results="search.results"
        @search="search.getItem"
        label="Search"
        type="multi"
        data-test="search"
      />
    </li>
    <li>
      <!-- Node type -->
      <FeatherSelect
        v-model="nodeTypeState"
        :options="nodeType.options"
        :text-prop="nodeType.optionText"
        :label="nodeType.label"
        @update:modelValue="onNodeTypeSelect"
        data-test="node-type"
      />
    </li>
    <li>
      <!-- Monitoring Location -->
      <FeatherSelect
        v-model="monitoringLocationState"
        :options="monitoringLocation.options"
        :text-prop="monitoringLocation.optionText"
        :label="monitoringLocation.label"
        @update:modelValue="onMonitoringLocationSelect"
        data-test="monitoring-location"
      />
    </li>
    <li>
      <!-- Severity -->
      <FeatherSelect
        v-model="severityState"
        :options="severity.options"
        :text-prop="severity.optionText"
        :label="severity.label"
        @update:modelValue="onSeveritySelect"
        data-test="severity"
      />
    </li>
    <li>
      <!-- Tagging -->
      <TagManagerCtrl data-test="tag-manager-ctrl" />
      <TagManager />
    </li>
    <!-- Sort / A-Z -->
    <li>
      <FeatherIcon @click="onSort" :icon="sort?.icon" :title="sort?.title" :viewBox="sort.viewBox" data-test="sort-icon" />
    </li>
    <li>
      <FeatherIcon @click="onSortAlpha" :icon="sortAlpha.icon" :title="sortAlpha.title" :viewBox="sortAlpha.viewBox" data-test="sort-alpha-icon" />
    </li>
    <!-- Expand/Collapse -->
    <li v-if="isExpanded">
      <FeatherIcon @click="isExpanded = !isExpanded" :icon="collapse.icon" :title="collapse.title" :viewBox="collapse.viewBox" data-test="collapse-icon" />
    </li>
    <li v-else>
      <FeatherIcon @click="isExpanded = !isExpanded" :icon="expand.icon" :title="expand.title" :viewBox="expand.viewBox" data-test="expand-icon" />
    </li>
  </ul>
</template>

<script lang="ts" setup>
import { fncArgVoid } from '@/types'
import { ISelectDropdown } from '@/types/select'
import {
  FeatherAutocomplete,
  IAutocompleteItemType
} from '@featherds/autocomplete'
import Sort from '@material-design-icons/svg/outlined/sort.svg'
import SortByAlpha from '@material-design-icons/svg/outlined/sort_by_alpha.svg'
import KeyboardDoubleArrowDown from '@material-design-icons/svg/outlined/keyboard_double_arrow_down.svg'
import KeyboardDoubleArrowUp from '@material-design-icons/svg/outlined/keyboard_double_arrow_up.svg'

const emits = defineEmits(['selectedItem'])

// Search
const search = {
  timeout: -1,
  loading: false,
  results: [] as IAutocompleteItemType[],
  value: [] as IAutocompleteItemType[],
  items: [''],
  getItem: (q: string) => {
    search.loading = true
    clearTimeout(search.timeout)
    search.timeout = window.setTimeout(() => {
      search.results = search.items
        .filter((x) => x.toLowerCase().indexOf(q) > -1)
        .map((x) => ({
          _text: x
        }))
      search.loading = false
    }, 500)}
}
// Node Type
const nodeTypeState = ref(undefined)
const onNodeTypeSelect: fncArgVoid = (selectedItem: any) => {
  emits('selectedItem', selectedItem)
}
const nodeType: ISelectDropdown = {
  label: 'Node Type',
  options: [
    {
      id: 1,
      type: 'type1'
    },
    {
      id: 2,
      type: 'type2'
    },
    {
      id: 3,
      type: 'type3'
    }
  ],
  optionText: 'type'
}

// Monitoring Location
const monitoringLocationState = ref(undefined)
const onMonitoringLocationSelect: fncArgVoid = (selectedItem: any) => {
  emits('selectedItem', selectedItem)
}
const monitoringLocation: ISelectDropdown = {
  label: 'Monitoring Location',
  options: [
    {
      id: 1,
      type: 'location1'
    },
    {
      id: 2,
      type: 'location2'
    },
    {
      id: 3,
      type: 'location3'
    }
  ],
  optionText: 'type'
}

// Severity
const severityState = ref(undefined)
const onSeveritySelect: fncArgVoid = (selectedItem: any) => {
  emits('selectedItem', selectedItem)
}
const severity: ISelectDropdown = {
  label: 'Severity',
  options: [
    {
      id: 1,
      type: 'severity1'
    },
    {
      id: 2,
      type: 'severity2'
    },
    {
      id: 3,
      type: 'severity3'
    }
  ],
  optionText: 'type'
}

const getViewBox = (icon: any) => {
  const iconProps = icon.render().props
  return iconProps.viewBox || `0 0 ${iconProps.width} ${iconProps.height}`
}
// Sort / A-Z
const onSort = () => null
const sort = {
  title: 'Sort',
  icon: Sort,
  viewBox: getViewBox(Sort)
}
const onSortAlpha = () => null
const sortAlpha = {
  title: 'Sort Alpha',
  icon: SortByAlpha,
  viewBox: getViewBox(SortByAlpha)
}

// Expand/Collapse
const isExpanded = ref(false)
const expand = {
  title: 'Expand',
  icon: KeyboardDoubleArrowDown,
  viewBox: getViewBox(KeyboardDoubleArrowDown)
}
const collapse = {
  title: 'Collapse',
  icon: KeyboardDoubleArrowUp,
  viewBox: getViewBox(KeyboardDoubleArrowUp)
}
</script>

<style lang="scss" scoped>
@use "@featherds/styles/themes/variables";

.filter-container {
  margin: var(variables.$spacing-l) 0;
  display: flex;
  flex-flow: row wrap;
  > * {
    margin-right: var(variables.$spacing-l);
    &:last-child {
      margin-right: 0;
      flex-grow: 1;
      text-align: right;
    }
  }
}

.autocomplete {
  min-width: 13rem;
}
.feather-icon {
  margin-top: var(variables.$spacing-xs);
  font-size: 1.5rem;
  color: var(variables.$disabled-text-on-surface);
  &:hover {
    color: var(variables.$primary-text-on-surface);
  }
}
</style>