<template>
  <div class="geo-map">
    <HeadlinePage text="Geo Map" class="title"/>
    <LeafletMap class="map" style="height: calc(100vh - 175px)" />
  </div>
</template>

<script setup lang="ts">
import LeafletMap from '../components/Map/LeafletMap.vue'
</script>

<style setup lang="scss">
@use '@featherds/styles/themes/variables';
.map {
  border-top: 2px solid var(variables.$border-on-surface);
}
.title {
  margin-left: 40px;
}
</style>
