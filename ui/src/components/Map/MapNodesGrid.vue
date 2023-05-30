<template>
  <div class="container">
    <table aria-label="Map Nodes Table" class="tl1 tl2 tl3 data-table">
      <thead>
        <tr>
          <FeatherSortHeader
            scope="col"
            property="id"
            :sort="sortStates.id"
            @sort-changed="sortChanged"
            data-test="col-id"
          >ID</FeatherSortHeader>

          <FeatherSortHeader
            scope="col"
            property="foreignSource"
            :sort="sortStates.foreignSource"
            @sort-changed="sortChanged"
            data-test="col-foreign-source"
          >FOREIGN SOURCE</FeatherSortHeader>

          <FeatherSortHeader
            scope="col"
            property="foreignId"
            :sort="sortStates.foreignId"
            @sort-changed="sortChanged"
            data-test="col-foreign-id"
          >FOREIGN ID</FeatherSortHeader>

          <FeatherSortHeader
            scope="col"
            property="label"
            :sort="sortStates.label"
            @sort-changed="sortChanged"
            data-test="col-label"
          >LABEL</FeatherSortHeader>

          <FeatherSortHeader
            scope="col"
            property="labelSource"
            :sort="sortStates.labelSource"
            @sort-changed="sortChanged"
            data-test="col-label-source"
          >LABEL SOURCE</FeatherSortHeader>

          <!-- <FeatherSortHeader
            scope="col"
            property="lastCapabilitiesScan"
            :sort="sortStates.lastCapabilitiesScan"
            @sort-changed="sortChanged"
            data-test="col-last-cap-scan"
          >LAST CAP SCAN</FeatherSortHeader>

          <FeatherSortHeader
            scope="col"
            property="primaryInterface"
            :sort="sortStates.primaryInterface"
            @sort-changed="sortChanged"
            data-test="col-primary-interface"
          >PRIMARY INTERFACE</FeatherSortHeader> -->

          <FeatherSortHeader
            scope="col"
            property="sysObjectId"
            :sort="sortStates.sysObjectId"
            @sort-changed="sortChanged"
            data-test="col-sys-object-id"
          >SYSOBJECTID</FeatherSortHeader>

          <FeatherSortHeader
            scope="col"
            property="sysName"
            :sort="sortStates.sysName"
            @sort-changed="sortChanged"
            data-test="col-sys-name"
          >SYSNAME</FeatherSortHeader>

          <FeatherSortHeader
            scope="col"
            property="sysDescription"
            :sort="sortStates.sysDescription"
            @sort-changed="sortChanged"
            data-test="col-sys-description"
          >SYSDESCRIPTION</FeatherSortHeader>

          <FeatherSortHeader
            scope="col"
            property="sysContact"
            :sort="sortStates.sysContact"
            @sort-changed="sortChanged"
            data-test="col-sys-contact"
          >SYSCONTACT</FeatherSortHeader>

          <FeatherSortHeader
            scope="col"
            property="sysLocation"
            :sort="sortStates.sysLocation"
            @sort-changed="sortChanged"
            data-test="col-sys-location"
          >SYSLOCATION</FeatherSortHeader>
        </tr>
      </thead>
      <TransitionGroup name="data-table" tag="tbody" data-test="node-list">
        <tr v-for="node in nodes" :key="(node?.id as number)" @dblclick="doubleClickHandler(node as Partial<Node>)" :node="node?.id">
          <td :class="nodeLabelAlarmServerityMap[node?.label as string]">{{ node?.id }}</td>
          <td>{{ node?.foreignSource }}</td>
          <td>{{ node?.foreignId }}</td>
          <td>{{ node?.label }}</td>
          <td>{{ node?.labelSource }}</td>
          <td>{{ node?.sysOid }}</td>
          <td>{{ node?.sysName }}</td>
          <td>{{ node?.sysDescription }}</td>
          <td>{{ node?.sysContact }}</td>
          <td>{{ node?.sysLocation }}</td>
        </tr>
      </TransitionGroup>
    </table>
  </div>
</template>
<script setup lang="ts">
import { useMapStore } from '@/store/Views/mapStore'
import { FeatherSortObject } from '@/types'
import { FeatherSortHeader, SORT } from '@featherds/table'
import { Node, MonitoringLocation } from '@/types/graphql'

const mapStore = useMapStore()
const nodes = computed(() => mapStore.devicesInbounds)
const nodeLabelAlarmServerityMap = computed(() => mapStore.getDeviceAlarmSeverityMap())

// TODO: Switch to Node type once location available
const doubleClickHandler = (node: Partial<any>) => {
  if (node.location?.latitude && node.location.longitude) {
    const coordinate: any = { latitude: node?.location?.latitude, longitude: node?.location?.longitude }
    mapStore.mapCenter = coordinate

    // to highlighting the selected row
    const rows = document.querySelectorAll('.node-list-grid > tr')
    rows.forEach(row => {
      if(row.getAttribute('node') === node.id?.toString()) row.classList.add('selected')
      else row.classList.remove('selected')
    })
  }
}

const sortStates: any = reactive({
  label: SORT.ASCENDING,
  id: SORT.NONE,
  foreignSource: SORT.NONE,
  foreignId: SORT.NONE,
  labelSource: SORT.NONE,
  lastCapabilitiesScan: SORT.NONE,
  primaryInterface: SORT.NONE,
  sysObjectId: SORT.NONE,
  sysName: SORT.NONE,
  sysDescription: SORT.NONE,
  sysContact: SORT.NONE,
  sysLocation: SORT.NONE
})

const sortChanged = (sortObj: FeatherSortObject) => {
  for (const key in sortStates) {
    sortStates[key] = SORT.NONE
  }
  sortStates[`${sortObj.property}`] = sortObj.value
  mapStore.deviceSortObject = sortObj
}

onMounted(() => {
  const wrap = document.getElementById('wrap')
  const thead = document.querySelector('thead')

  if (wrap && thead) {
    wrap.addEventListener('scroll', function () {
      let translate = 'translate(0,' + this.scrollTop + 'px)'
      thead.style.transform = translate
    })
  }
})
</script>

<style lang="scss" scoped>
@use "@featherds/styles/themes/variables";
@use "@featherds/table/scss/table";
@use "@/styles//transitionDataTable";

.container {
  height: calc(100% - 29px);
  overflow: auto;
  background: var(variables.$surface);
}
table {
  width: 100%;
  @include table.table;
  @include table.table-condensed;
  @include table.row-select;
  @include table.row-hover;
  background: var(variables.$surface);
  color: var(variables.$primary-text-on-surface);
  padding-top: 4px;
  margin-top: 15px;
}
thead {
  z-index: 2;
  position: relative;
  background: var(variables.$surface);
  white-space: nowrap;
}
.selected > td:first-child {
  padding-left: 12px;
  border-left: 4px solid var(variables.$success);
}
.WARNING {
  border-left: 4px solid #fffb00ea
}
.MINOR {
  border-left: 4px solid var(variables.$warning);
}
.MAJOR {
  border-left: 4px solid #ff3c00;
}

.CRITICAL {
  border-left: 4px solid var(variables.$error);
}
</style>
