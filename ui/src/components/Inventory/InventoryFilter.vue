<template>
  <ul class="filter-container">
    <li class="autocomplete">
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
      <InventoryTagManagerCtrl data-test="tag-manager-ctrl" />
    </li>
    <!-- Sort/A-Z -->
    <li
      @click="onSort"
      class="action-btn"
      data-test="sort-btn"
    >
      <Icon :icon="sortIcon" />
    </li>
    <li
      @click="onSortAlpha"
      class="action-btn"
      data-test="sort-alpha-btn"
    >
      <Icon :icon="sortAlphaIcon" />
    </li>
    <li
      @click="inventoryStore.toggleFilter"
      class="action-btn"
      :data-test="expandCollapseBtn"
    >
      <Icon :icon="expandCollapseIcon" />
    </li>
  </ul>
  <InventoryTagManager v-if="isTagManagerOpen" />
</template>

<script lang="ts" setup>
import { IAutocompleteItemType } from '@featherds/autocomplete'
import Sort from '@material-design-icons/svg/outlined/sort.svg'
import SortByAlpha from '@material-design-icons/svg/outlined/sort_by_alpha.svg'
import KeyboardDoubleArrowDown from '@material-design-icons/svg/outlined/keyboard_double_arrow_down.svg'
import KeyboardDoubleArrowUp from '@material-design-icons/svg/outlined/keyboard_double_arrow_up.svg'
import { ISelectDropdown } from '@/types/select'
import { fncArgVoid, IIcon } from '@/types'
import { useInventoryStore } from '@/store/Views/inventoryStore'

const inventoryStore = useInventoryStore()

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
    }, 500)
  }
}

const nodeTypeState = ref(undefined)
const onNodeTypeSelect: fncArgVoid = (selectedType: any) => {
  // use store to query new list
}
const nodeType: ISelectDropdown = {
  label: 'Node Type',
  options: [
    {
      id: 1,
      name: 'type1'
    },
    {
      id: 2,
      name: 'type2'
    },
    {
      id: 3,
      name: 'type3'
    }
  ],
  optionText: 'name'
}

const monitoringLocationState = ref(undefined)
const onMonitoringLocationSelect: fncArgVoid = (selectedItem: any) => {
  // use store to query new list
}
const monitoringLocation: ISelectDropdown = {
  label: 'Monitoring Location',
  options: [
    {
      id: 1,
      name: 'location1'
    },
    {
      id: 2,
      name: 'location2'
    },
    {
      id: 3,
      name: 'location3'
    }
  ],
  optionText: 'name'
}

const severityState = ref(undefined)
const onSeveritySelect: fncArgVoid = (selectedItem: any) => {
  // use store to query new list
}
const severity: ISelectDropdown = {
  label: 'Severity',
  options: [
    {
      id: 1,
      name: 'severity1'
    },
    {
      id: 2,
      name: 'severity2'
    },
    {
      id: 3,
      name: 'severity3'
    }
  ],
  optionText: 'name'
}

// Sort/A-Z
const onSort = () => null
const sortIcon: IIcon = {
  image: Sort,
  title: 'Sort',
  size: 2
}
const onSortAlpha = () => null
const sortAlphaIcon: IIcon = {
  image: SortByAlpha,
  title: 'Sort Alpha',
  size: 2
}

const isTagManagerOpen = computed(() => inventoryStore.isTagManagerOpen)

const expandCollapseBtn = ref('expand-btn')
const expandIcon: IIcon = {
  image: KeyboardDoubleArrowDown,
  title: 'Expand',
  size: 2
}
const collapseIcon: IIcon = {
  image: KeyboardDoubleArrowUp,
  title: 'Collapse',
  size: 2
}
const expandCollapseIcon = ref(computed<IIcon>(() => (inventoryStore.isFilterOpen ? collapseIcon : expandIcon)))
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@/styles/btns.scss';

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
  > .autocomplete {
    min-width: 13rem;
  }

  > .action-btn {
    margin-right: 0;
    padding: var(variables.$spacing-xxs);
    font-size: 1.5rem;
    color: var(variables.$secondary-text-on-surface);
    &:hover {
      color: var(variables.$disabled-text-on-surface);
    }
  }
}
</style>
