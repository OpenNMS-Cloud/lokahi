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
  managementIp: 'Management IP'
}
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';

.text-anchor-list {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.tag-list {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  > ul {
    margin-left: var(variables.$spacing-xxs);
    > li {
      display: inline;
      &:after {
        content: ', ';
      }
      &:last-child:after {
        content: '';
      }
    }
  }
}
</style>
