<template>
  <div class="geo-map">
    <HeadlinePage text="Geo Map" class="title"/>
    <LeafletMap style="height: calc(100vh - 173px)" />
  </div>
</template>

<script setup lang="ts">
import LeafletMap from '../components/Map/LeafletMap.vue'
</script>

<style setup lang="scss">
.title {
  margin-left: 40px;
}
</style>
