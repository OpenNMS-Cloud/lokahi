<template>
  <!-- 
    View all events / Search (form) / Severity legend
    Search (event text @ time)
    Filter
    Pagination
    columns: ID, Severity, Time, Source Location, System-ID, Node, Node Location, Interface, Service, alarm ID 
  -->
  <Transition name="fade">
    <TableCard v-if="nodeData.events">
      <section class="header">
        <div class="title-container">
          <span class="title">
            Events ({{ nodeData.node.label }})
          </span>
        </div>
        <FeatherInput
          class="search" 
          v-model="searchValue" 
          label="Event">
          <template v-slot:pre>
            <FeatherIcon :icon="Search" />
          </template>
        </FeatherInput>
        <div class="btns">
          <FeatherButton icon="Filter">
            <FeatherIcon :icon="FilterAlt" />
          </FeatherButton>
          <FeatherButton icon="Sort">
            <FeatherIcon :icon="Sort" />
          </FeatherButton>
        </div>
      </section>
      <section class="table-container">
        <table class="tl1 tl2 tc3 tc4 tc5 data-table" summary="Events" data-test="data-table">
          <thead>
            <tr>
              <th scope="col" data-test="col-id">ID</th>
              <th scope="col" data-test="col-severity">Severity</th>
              <th scope="col" data-test="col-time">Time</th>
              <th scope="col" data-test="col-source-location">Source Location</th>
              <th scope="col" data-test="col-system-id">System-ID</th>
              <th scope="col" data-test="col-node">Node</th>
              <th scope="col" data-test="col-node-location">Node Location</th>
              <th scope="col" data-test="col-interface">Interface</th>
              <th scope="col" data-test="col-service">Service</th>
              <th scope="col" data-test="col-alarm-id">Alarm ID</th>
            </tr>
          </thead>
          <TransitionGroup name="data-table" tag="tbody">
            <tr v-for="event in nodeData.events" :key="event.id as number" data-test="data-item">
              <td>{{ event.id }}</td>
              <td>{{ event.severity }}</td>
              <td>{{ event.time }}</td>
              <td>{{ event.source }}</td>
              <td>--</td>
              <td>{{ event.nodeLabel }}</td>
              <td>{{ event.location }}</td>
              <td>{{ event.ipAddress || '--' }}</td>
              <td>--</td>
              <td>--</td>
            </tr>
          </TransitionGroup>
        </table>
      </section>
      <section>
        <FeatherPagination
          v-model="page"
          :pageSize="pageSize"
          :total="total"
          @update:pageSize="updatePageSize"
        />
      </section>
    </TableCard>
  </Transition>
</template>

<script lang="ts" setup>
import FilterAlt from '@featherds/icon/action/FilterAlt'
import Sort from '@featherds/icon/action/Sort'
import Search from '@featherds/icon/action/Search'
import { useNodeStatusStore } from '@/store/Views/nodeStatusStore'

const nodeStatusStore = useNodeStatusStore()
const route = useRoute()

const searchValue = ref()

// Pagination
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const updatePageSize = (v: number) => { pageSize.value = v }
  
const nodeData = computed(() => {
  const events = nodeStatusStore.fetchedData?.listEvents?.events?.filter((event: any) => event.nodeId == route.params.id)

  total.value = events?.length || 0

  return {
    events,
    node: nodeStatusStore.fetchedData?.device,
    latencies: nodeStatusStore.fetchedData?.deviceLatency,
    uptimes: nodeStatusStore.fetchedData?.deviceUptime
  }
})

onBeforeMount(() => {
  nodeStatusStore.setNodeId(Number(route.params.id))
})
</script>

<style lang="scss" scoped>
@use "@/styles/_listCard";
@use "@/styles/_transitionFade";
</style>
