<template>
  <ul class="anchor-list">
    <li>
      <label>{{ label.profile }}: </label>
      <a :href="anchor.profileLink">{{ anchor.profileValue }}</a>
    </li>
    <li>
      <label>{{ label.location }}: </label>
      <a :href="anchor.locationLink">{{ anchor.locationValue }}</a>
    </li>
    <li>
      <label>{{ label.ipInterface }}: </label>
      <a :href="anchor.ipInterfaceLink">{{ anchor.ipInterfaceValue }}</a>
    </li>
    <li>
      <label>{{ label.tag }}: </label>
      <a :href="anchor.tagLink">{{ anchor.tagValue }}</a>
    </li>
  </ul>
</template>

<script lang="ts" setup>
import { PropType } from 'vue'
import { Anchor } from '@/types/inventory'


defineProps({
  anchor: {
    type: Object as PropType<Anchor>,
    required: true
  }
})

const label = {
  profile: 'Monitoring Profile',
  location: 'Monitoring Location',
  ipInterface: 'IP Interface',
  tag: 'Tag'
}
</script>

<style lang="scss" scoped>
.anchor-list {
  list-style: none;
  padding: 0;
}
</style>