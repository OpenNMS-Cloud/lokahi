<template>
  <TableCard>
   <div>
    <section class="feather-row">
      <h3 data-test="heading" class="feather-col-6 title">Latest Events</h3>
      <div class="btns action-container feather-col-6">
        <div class="search-container">
          <FeatherInput
            :label="searchLabel"
            v-model.trim="searchEvents"
            type="search"
            data-test="search-input"
            @update:model-value="onSearchChanged"
          >
            <template #pre>
              <FeatherIcon :icon="icons.Search" />
            </template>
          </FeatherInput>
        </div>
            <FeatherButton
              primary
              icon="Download"
            >
              <FeatherIcon :icon="icons.DownloadFile"/>
            </FeatherButton>
            <FeatherButton
              primary
              icon="Refresh"
              @click="nodeStatusQueries.fetchNodeStatus"
            >
              <FeatherIcon :icon="icons.Refresh"/>
            </FeatherButton>
          </div>
    </section>

    <div class="container">
      <table class="data-table" aria-label="Events Table" data-test="data-table">
        <thead>
          <tr>
            <FeatherSortHeader
                v-for="col of columns"
                :key="col.label"
                scope="col"
                :property="col.id"
                :sort="(sort as any)[col.id]"
                v-on:sort-changed="sortChanged"
              >
                {{ col.label }}
              </FeatherSortHeader>
          </tr>
        </thead>
        <TransitionGroup name="data-table" tag="tbody" v-if="hasEvents && pageInfo.total">
          <tr v-for="event in paginatedEvents" :key="event.id as number" data-test="data-item">
            <td>{{ fnsFormat(event.producedTime, 'M/dd/yyyy HH:mm:ssxxx') }}</td>
            <td>{{ event.uei }}</td>
            <td>{{ event.ipAddress }}</td>
          </tr>
        </TransitionGroup>
      </table>
      <div v-if="!hasEvents || pageInfo.total === 0">
        <EmptyList
          :content="emptyListContent"
          data-test="empty-list"
        />
      </div>
    </div>
        <FeatherPagination
          v-model="pageInfo.page"
          :pageSize="pageInfo.pageSize"
          :total="pageInfo.total"
          @update:modelValue="onPageChanged"
          @update:pageSize="onPageSizeChanged"
          data-test="pagination"
          v-if="hasEvents && pageInfo.total"
        />
  </div>
  </TableCard>
</template>

<script lang="ts" setup>
import DownloadFile from '@featherds/icon/action/DownloadFile'
import Refresh from '@featherds/icon/navigation/Refresh'
import { useNodeStatusStore } from '@/store/Views/nodeStatusStore'
import { useNodeStatusQueries } from '@/store/Queries/nodeStatusQueries'
import { format as fnsFormat } from 'date-fns'
import { SORT } from '@featherds/table'
import Search from '@featherds/icon/action/Search'
import { sortBy } from 'lodash'

const nodeStatusQueries = useNodeStatusQueries()

const nodeStatusStore = useNodeStatusStore()

const searchEvents = ref('')

const searchableAttributes = ['uei', 'ipAddress', 'producedTime']

const eventSearchedData = ref([] as any[])

const paginatedEvents = ref([] as any[])

const icons = markRaw({
  DownloadFile,
  Refresh,
  Search
})

const searchLabel = 'Search Events'

const columns = [
  { id: 'time', label: 'Time' },
  { id: 'uei', label: 'UEI' },
  { id: 'Ipaddress', label: 'IP Address' }
]

const emptyListContent = {
  msg: 'No results found.'
}

const sort: Record<string, string>  = reactive({
  time: SORT.NONE,
  uei: SORT.NONE,
  Ipaddress: SORT.NONE
})

const pageInfo = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const eventData = computed(() => {
  const events = nodeStatusStore.fetchedData?.events || []
  pageInfo.total = events.length || 0
  return {
    events
  }
})
const hasEvents = computed(() => eventData.value.events.length > 0)

const updateEvents = () => {
  if (hasEvents.value) {
    eventSearchedData.value = [...eventData.value.events] as any[]
    pageInfo.total = eventData.value.events.length || 0
    updatePaginatedEvents(eventSearchedData.value, pageInfo.page, pageInfo.pageSize)
  }
}

onBeforeMount(() => {
  updateEvents()
})

watch(() => eventData.value, () => {
  updateEvents()
})

const updatePaginatedEvents = (events: Array<any>, pageNumber: number, pageSize: number) => {
  const startIndex = (pageNumber - 1) * pageSize
  const endIndex = startIndex + pageSize
  paginatedEvents.value = events.slice(startIndex, endIndex)
}

const onPageChanged = (v: number) => {
  if (hasEvents) {
    updatePaginatedEvents(eventData.value.events, v, pageInfo.pageSize)
  }
}

const onPageSizeChanged = (v: number) => {
  if (hasEvents) {
    pageInfo.pageSize = v
    updatePaginatedEvents(eventData.value.events, pageInfo.page, v)
  }
}

const sortChanged = (sortObj: Record<string, string>) => {

  let sorted = [...eventData.value.events] as any

  if (sortObj.value === 'asc' || sortObj.value === 'desc') {
    sorted = sortBy(sorted, sortObj.property)

    if (sortObj.value === 'desc') {
      sorted.reverse()
    }
  }

  pageInfo.page = 1
  pageInfo.total = sorted?.length

  updatePaginatedEvents(sorted, pageInfo.page, pageInfo.pageSize)
  for (const prop in sort) {
    sort[prop] = SORT.NONE
  }
  sort[sortObj.property] = sortObj.value
}

const onSearchChanged = (searchTerm: any) => {
  let searchObjects

  if (searchTerm === '') {
    searchObjects = [...eventSearchedData.value]
  } else {
    const searchItem = searchTerm.toLowerCase()
    searchObjects = eventSearchedData.value.filter((item: any) => {
      return searchableAttributes.some((attribute) => {
        const value = String(item[attribute]).toLowerCase()
        return value.includes(searchItem)
      })
    })
  }

  eventData.value.events = [...searchObjects]
  pageInfo.page = 1
  pageInfo.total = searchObjects?.length

  updatePaginatedEvents(searchObjects, pageInfo.page, pageInfo.pageSize)
}

</script>

<style lang="scss" scoped>
@use "@featherds/styles/themes/variables";
@use "@featherds/styles/mixins/typography";
@use "@featherds/table/scss/table";
@use "@/styles/_transitionDataTable";

.main-section {
  padding: 0 var(--spacing-s) var(--spacing-l) var(--spacing-s);
}

.feather-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 1rem 0 0.5rem;

  .title {
    margin: 0 0 2.5rem 1rem;
  }
  .action-container {
    display: flex;
    justify-content: flex-end;
    gap: 5px;
    width: 30%;

    > .search-container {
      width: 80%;
      margin-right: 3px;

      :deep(.label-border) {
        width: 110px !important;
      }
    }
  }
}

.container {
  display: block;
  overflow-x: auto;
  table {
    width: 100%;
    padding: 0 1.5rem 1.5rem 1.5rem;
    @include table.table;
    thead {
      background: var(variables.$background);
      text-transform: uppercase;
    }
    td {
      white-space: nowrap;
      div {
        border-radius: 5px;
        padding: 0px 5px 0px 5px;
      }
    }
  }
}
.feather-pagination {
  border: none;
  padding: 20px 0px;
  :deep(.per-page-text), :deep(.page-size-select) {
    display: block !important;
   }
}
</style>
