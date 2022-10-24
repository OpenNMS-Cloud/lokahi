<template>
<div class="header">Graphs</div>

<div class="graphs-layout">
  <div class="left-column">
    <Graph
      :label="'Minion Uptime and Minion Response Time'"
      :metricStrings="['minion_uptime_sec', 'minion_response_time_msec']"
    />
    <Graph
      :label="'Minion Uptime'"
      :metricStrings="['minion_uptime_sec']"
    />
    <Graph
      :label="'Minion Response Time'"
      :metricStrings="['minion_response_time_msec']"
    />
    <Graph
      :label="'CPU Seconds Total'"
      :metricStrings="['process_cpu_seconds_total']"
    />
    <Graph
      :label="'Max FDS'"
      :metricStrings="['process_max_fds']"
    />
  </div>
  <div class="right-column">
    <Graph
      :label="'Proess Resident Mem Bytes'"
      :metricStrings="['process_resident_memory_bytes']"
    />
    <Graph
      :label="'Process Start Time'"
      :metricStrings="['process_start_time_seconds']"
    />
    <Graph
      :label="'Process Virtual Mem Bytes'"
      :metricStrings="['process_virtual_memory_bytes']"
    />
    <Graph
      :label="'Process Virtual Mem Max Bytes'"
      :metricStrings="['process_virtual_memory_max_bytes']"
    />
    <Graph
      :label="'Go Threads'"
      :metricStrings="['go_threads']"
    />
  </div>
</div>
</template>
  
<script setup lang="ts"></script>
  
<style scoped lang="scss">
@import "@featherds/styles/mixins/typography";

.header {
  @include headline3();
  text-align: center;
  padding: 6px;
  background: var($shade-4);
}

.graphs-layout {
  display: flex;
  
  .left-column, 
  .right-column {
    display: flex;
    flex-direction: column;
    width: 50%;
  }
}
</style>
  