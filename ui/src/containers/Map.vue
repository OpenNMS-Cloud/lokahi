<template>
  <div class="geo-map">
    <HeadlinePage text="Geo Map"/>
    <LeafletMap style="height: calc(100vh - 145px)" />
  </div>
</template>

<script setup lang="ts">
import LeafletMap from '../components/Map/LeafletMap.vue'
</script>

<style setup lang="scss">
@use '@featherds/styles/themes/variables';
</style>
