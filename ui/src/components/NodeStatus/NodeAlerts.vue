<template>
  <div>
    <section class="node-component-header">
      <h3 data-test="heading" class="node-label">Recent Alerts</h3>
    </section>
    <section class="node-component-content">
      <TableCard>
        <div class="header">
          <div class="title-container">
            <span class="title"></span>
          </div>
        </div>
        <div class="container">
          <table class="data-table" aria-label="Recent Alerts Table" data-test="data-table">
            <thead>
              <tr>
                <th scope="col">Alert Type</th>
                <th scope="col">Severity</th>
                <th scope="col">Date</th>
                <th scope="col">Time</th>
              </tr>
            </thead>
            <TransitionGroup name="data-table" tag="tbody">
              <tr v-for="alert in alertData" :key="alert.id as number" data-test="data-item">
                <td>{{ alert.type }}</td>
                <td>{{ alert.severity }}</td>
                <td>{{ alert.date }}</td>
                <td>{{ alert.time }}</td>
              </tr>
            </TransitionGroup>
          </table>
        </div>
        <div>
          <!-- <FeatherPagination
            v-model="page"
            :pageSize="pageSize"
            :total="total"
            @update:pageSize="updatePageSize"
          /> -->
        </div>
      </TableCard>
    </section>
  </div>
</template>

<script lang="ts" setup>

const alertData = ref<any[]>([
  { id: 1, type: 'Custom Severity 90', severity: 'Critical', date: '2/28/2023', time: '04:15.10 am est' },
  { id: 2, type: 'Custom Severity 90', severity: 'Critical', date: '2/28/2023', time: '04:15.10 am est' },
  { id: 3, type: 'Custom Severity 90', severity: 'Critical', date: '2/28/2023', time: '04:15.10 am est' },
  { id: 4, type: 'Custom Severity 80', severity: 'Major', date: '2/28/2023', time: '04:15.10 am est' }
])
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@/styles/vars';
@use '@/styles/mediaQueriesMixins';
@use '@featherds/styles/mixins/typography';
@use '@featherds/table/scss/table';
@use '@/styles/_transitionDataTable';

.node-component-header {
  margin-bottom: var(variables.$spacing-s);
  display: flex;
  flex-direction: row;
  gap: 0.5rem;
  align-items: center;
  justify-content: space-between;
}

.node-component-label {
  margin: 0;
  line-height: 20px;
  letter-spacing: 0.28px;
}

.node-component-content {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  gap: 2rem;
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
    td {
      white-space: nowrap;
      div {
        border-radius: 5px;
        padding: 0px 5px 0px 5px;
      }
    }
  }
}
</style>
