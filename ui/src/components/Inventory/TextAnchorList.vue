<template>
  <ul class="text-anchor-list">
    <!-- <li data-test="profile">
      <label :for="label.profile">{{ label.profile }}: </label>
      <a v-if="anchor.profileLink" @click="goto(anchor.profileLink)" :id="label.profile">{{ anchor?.profileValue }}</a>
      <span v-else :id="label.profile">{{ anchor.profileValue }}</span>
    </li> -->
    <li data-test="location">
      <label :for="label.location">{{ label.location }}: </label>
      <a v-if="anchor.locationLink" @click="goto(anchor.locationLink)" :id="label.location">{{ anchor?.locationValue }}</a>
      <span v-else :id="label.location">{{ anchor.locationValue }}</span>
    </li>
    <li data-test="management-ip">
      <label :for="label.managementIp">{{ label.managementIp }}: </label>
      <a v-if="anchor.managementIpLink" @click="goto(anchor.managementIpLink)" :id="label.managementIp">{{ anchor.managementIpValue }}</a>
      <span v-else :id="(label.managementIp)">{{ anchor.managementIpValue }}</span>
    </li>
    <!-- <li data-test="tag">
      <label :for="label.tag">{{ label.tag }}: </label>
      <a v-if="anchor.tagLink" @click="goto(anchor.tagLink)" :id="label.tag">{{ anchor.tagValue }}</a>
      <span v-else :id="label.tag">{{ anchor.tagValue }}</span>
    </li> -->
  </ul>
</template>

<script lang="ts" setup>
import { Anchor } from '@/types/inventory'
import router from '@/router'

defineProps<{
  anchor: Anchor
}>()

const goto = (path: string | undefined) => {
  if(!path) return
  
  router.push({
    path
  })
}

const label = {
  profile: 'Monitoring Profile',
  location: 'Monitoring Location',
  managementIp: 'Management IP',
  tag: 'Tag'
}
</script>

<style lang="scss" scoped>
ul.text-anchor-list {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}
</style>