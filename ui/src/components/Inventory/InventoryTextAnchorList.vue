<template>
  <ul class="text-anchor-list">
    <li data-test="location">
      <label :for="label.location">{{ label.location }}: </label>
      <a
        v-if="anchor.locationLink"
        @click="goto(anchor.locationLink)"
        :id="label.location"
        >{{ anchor?.locationValue }}</a
      >
      <span
        v-else
        :id="label.location"
        >{{ anchor.locationValue }}</span
      >
    </li>
    <li data-test="management-ip">
      <label :for="label.managementIp">{{ label.managementIp }}: </label>
      <a
        v-if="anchor.managementIpLink"
        @click="goto(anchor.managementIpLink)"
        :id="label.managementIp"
        >{{ anchor.managementIpValue }}</a
      >
      <span
        v-else
        :id="label.managementIp"
        >{{ anchor.managementIpValue }}</span
      >
    </li>
    <li data-test="node-type">
      <label :for="label.nodeType">{{ label.nodeType }}: </label>
      <span
        :id="label.nodeType"
        class="node-type"
        >{{ anchor.nodeType }}
      </span>
    </li>
  </ul>
</template>

<script lang="ts" setup>
import { Anchor } from '@/types/inventory'
import router from '@/router'

defineProps<{
  anchor: Anchor
}>()

const goto = (path: string | undefined) => {
  if (!path) return

  router.push({
    path
  })
}

const label = {
  profile: 'Monitoring Profile',
  location: 'Monitoring Location',
  managementIp: 'Management IP',
  nodeType: 'Node Type'
}
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';

.text-anchor-list {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.node-type {
  display: inline-block;
  text-transform: lowercase;

  &.node-type:first-letter {
    text-transform: capitalize !important;
  }
}
</style>
