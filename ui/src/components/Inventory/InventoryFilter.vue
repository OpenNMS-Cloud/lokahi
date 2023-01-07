<template>
  <ul class="filter-container">
    <!-- Search -->
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
    <!-- Node type -->
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
    <!-- Monitoring Location -->
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
    <!-- Severity -->
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
    <!-- Tagging -->
    <li>
      <InventoryTagManagerCtrl data-test="tag-manager-ctrl" /> 
    </li>
    <!-- Sort/A-Z -->
    <li @click="onSort" class="action-btn" data-test="sort-btn"><Icon :icon="sort" /></li>
    <li @click="onSortAlpha" class="action-btn" data-test="sort-alpha-btn" ><Icon :icon="sortAlpha" /></li>
    <!-- Expand/Collapse -->
    <li @click="inventoryStore.toggleFilter" :data-test="expandCollapseBtn" class="action-btn"><Icon :icon="expandCollapse" /></li>
  </ul>
  <InventoryTagManager />
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

// Monitoring Location
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

// Severity
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
const sort: IIcon = {
  image: Sort,
  title: 'Sort'
}
const onSortAlpha = () => null
const sortAlpha: IIcon = {
  image: SortByAlpha,
  title: 'Sort Alpha'
}

// Expand/Collapse
const expandCollapseBtn = ref('expand-btn')
const expand: IIcon = {
  image: KeyboardDoubleArrowDown,
  title: 'Expand'
}
const collapse: IIcon = {
  image: KeyboardDoubleArrowUp,
  title: 'Collapse'
}
const expandCollapse = ref(computed<IIcon>(() => {
  expandCollapseBtn.value = inventoryStore.isFilterOpen ? 'collapse-btn' : 'expand-btn'
  return inventoryStore.isFilterOpen ? collapse : expand
}))

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