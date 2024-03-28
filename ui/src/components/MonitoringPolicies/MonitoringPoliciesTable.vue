<template>
  <div class="monitoring-policies-table-wrapper">
    <TableCard class="card border">
      <div class="header">
        <div class="title-container">
          <div class="title-time">
            <span class="title"></span>
          </div>
          <div class="btns">
            <FeatherButton
              primary
              icon="Download"
              @click="onDownload"
            >
              <FeatherIcon :icon="icons.DownloadFile"> </FeatherIcon>
            </FeatherButton>
            <FeatherButton
              primary
              icon="Refresh"
              @click="onRefresh"
            >
              <FeatherIcon :icon="icons.Refresh"> </FeatherIcon>
            </FeatherButton>
          </div>
        </div>
      </div>
      <div class="container">
        <table
          class="data-table"
          aria-label="Monitoring Policies Table"
        >
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
          <TransitionGroup
            name="data-table"
            tag="tbody"
          >
            <tr
              v-for="policy in store.monitoringPolicies"
              :key="policy.id"
              class="policies-table-row"
            >
              <td>
                <div class="action">
                 <span class="check-circle"  @click="() => onSelectPolicy(policy.id)">
                  <FeatherTooltip
                    :title="policy?.enabled ? 'Enabled':'Disabled'"
                    v-slot="{ attrs, on }"
                  >
                    <FeatherIcon
                      v-bind="attrs"
                      v-on="on"
                      :icon="policy?.enabled ? CheckCircle : Circle"
                      :class="{ 'enabled': policy?.enabled }"
                      class="enabled-icon"
                      data-test="check-icon"
                    />
                  </FeatherTooltip>
                 </span>
                  <span @click.stop class="policy-name">
                    {{ policy.name }}
                  </span>
               </div>
              </td>
              <td>{{ policy.memo }}</td>
              <td>{{ policy.rules?.length || 0 }}</td>
              <td>{{ '--' }}</td>
            </tr>
          </TransitionGroup>
        </table>
        <!-- <FeatherPagination
          v-model=""
          :pageSize=""
          :total=""
          @update:model-value=""
          v-if="store.monitoringPolicies.length > 0"
        >
        </FeatherPagination> -->
      </div>
    </TableCard>
  </div>
</template>

<script setup lang="ts">
import { SORT } from '@featherds/table'
import CheckCircle from '@featherds/icon/action/CheckCircle'
import Circle from '@/assets/circle.svg'
import DownloadFile from '@featherds/icon/action/DownloadFile'
import Refresh from '@featherds/icon/navigation/Refresh'
import { useMonitoringPoliciesStore } from '@/store/Views/monitoringPoliciesStore'
import { Policy } from '@/types/policies'

const store = useMonitoringPoliciesStore()

const icons = markRaw({
  CheckCircle,
  DownloadFile,
  Refresh
})

const emit = defineEmits<{
  (e: 'policySelected', id: number): void
}>()

const columns = [
  { id: 'name', label: 'Name' },
  { id: 'description', label: 'Description' },
  { id: 'alertRules', label: 'Alert Rules' },
  { id: 'affectedNodes', label: 'Affected Nodes' }
]

const sort = reactive({
  name: SORT.NONE,
  description: SORT.NONE,
  alertRules: SORT.NONE,
  affectedNodes: SORT.ASCENDING
}) as any

const sortChanged = (sortObj: Record<string, string>) => {
  // store.setTopNNodesTableSort(sortObj)

  for (const prop in sort) {
    sort[prop] = SORT.NONE
  }

  sort[sortObj.property] = sortObj.value
}

const onSelectPolicy = (id: string) => {
  console.log(`onSelectPolicy, id: ${id}`)

  const selectedPolicy = store.monitoringPolicies.find((item: Policy) => item.id === Number(id))

  if (selectedPolicy) {
    store.displayPolicyForm(selectedPolicy)
    emit('policySelected', Number(selectedPolicy.id))
  }
}

const onDownload = () => {
  console.log('download clicked')
}

const onRefresh = () => {
  console.log('refresh clicked')
  store.getMonitoringPolicies()
}

</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';
@use '@featherds/table/scss/table';
@use '@/styles/_transitionDataTable';
@use '@/styles/vars.scss';

.monitoring-policies-table-wrapper {
  margin-bottom: 20px;
  width: 100%;
  overflow-x: hidden;

  .card {
    height: 442px;

    .header {
      display: flex;
      justify-content: space-between;

      .title-container {
        display: flex;
        justify-content: space-between;
        width: 100%;
        margin-bottom: 20px;

        .title-time {
          display: flex;
          flex-direction: column;
          .title {
            @include typography.headline3;
            margin-left: 20px;
            margin-top: 2px;
          }
        }
        .btns {
          margin-right: 15px;
        }
      }
    }
  }

  .container {
    display: block;
    overflow-x: auto;
    table {
      width: 100%;
      @include table.table;
      thead {
        background: var(variables.$background);
        text-transform: uppercase;
      }
      .action{
        display: flex;
        justify-content: flex-start;
        align-items: center;
        gap: 20px;
      }
      td {
        white-space: nowrap;
        div {
          border-radius: 5px;
          padding: 0px 5px 0px 5px;
        }
      }

      tr.policies-table-row {
        cursor: pointer;
        transition: background-color 0.3s ease;
        &:hover{
          background-color: var(variables.$shade-4)
        }
      }

    }
  }
}

.check-circle {
  margin-top: 5px;
  .enabled-icon {
    width: 1.5rem;
    height: 1.5rem;
    color: var(variables.$shade-3);

    &.enabled {
      color: var(variables.$success);
    }
  }
}
</style>
