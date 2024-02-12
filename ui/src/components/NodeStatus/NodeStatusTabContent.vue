<template>
  <div class="cards">
    <div class="column-left">
      <div class="card">
        <NodeSystemInformation />
      </div> 
      <div class="card">
        <NodeTags />
      </div> 
      <div class="card">
        <NodeMonitoringPolicies />
      </div> 
      <div class="card">
        <NodeDiscoveries />
      </div> 
    </div>
    <div class="column-right">
      <div class="card">
        <NodeAlerts />
      </div> 
      <div class="card">
        <NodeGraph
          title="CPU Utilization"
        />
      </div> 
      <div class="card">
        <NodeGraph
          title="Memory Utilization"
        />
      </div> 
    </div>
  </div>
</template>

<script lang="ts" setup>


</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@/styles/vars';
@use '@/styles/mediaQueriesMixins';
@use '@featherds/styles/mixins/typography';

/*
.ctrls {
  display: flex;
  justify-content: end;
  padding: var(variables.$spacing-s) 0;
  min-width: vars.$min-width-smallest-screen;
}
*/

.cards {
  display: flex;
  //flex-flow: row wrap;
  gap: 1%;
  margin-top: 1em;

  .column-left {
    width: 35%;
  }

  .column-right {
    width: 65%;
  }

  .card {
    position: relative;
    padding: var(variables.$spacing-m) var(variables.$spacing-m);
    border: 1px solid rgba(21, 24, 43, 0.12);
    border-radius: 4px;
    width: 100%;
    min-width: vars.$min-width-smallest-screen;
    margin-bottom: var(variables.$spacing-m);
    background: var(variables.$surface);

    @include mediaQueriesMixins.screen-sm {
      width: 100%;
    }

    @include mediaQueriesMixins.screen-md {
      width: auto;
      min-width: 438px;
    }

    .node-header {
      margin-bottom: var(variables.$spacing-s);
      display: flex;
      flex-direction: row;
      gap: 0.5rem;
      align-items: center;
      justify-content: space-between;
    }

    .node-label {
      margin: 0;
      line-height: 20px;
      letter-spacing: 0.28px;
    }
  }
}

.node-content {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  gap: 2rem;
}

.node-footer {
  display: flex;
  justify-content: space-between;
  margin-top: 40px;
}

.text-badge-row {
  display:flex;
}
</style>
