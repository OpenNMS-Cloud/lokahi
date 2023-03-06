<template>
  <ul class="text-anchor-list">
    <!-- <li data-test="profile">
      <label :for="label.profile">{{ label.profile }}: </label>
      <a v-if="anchor.profileLink" @click="goto(anchor.profileLink)" :id="label.profile">{{ anchor?.profileValue }}</a>
      <span v-else :id="label.profile">{{ anchor.profileValue }}</span>
    </li> -->
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
    <li
      data-test="tag"
      class="tag-list"
    >
      <label :for="label.tag">{{ label.tag }}: </label>
      <ul
        v-if="anchor.tagValue.length"
        :id="label.tag"
      >
        <li
          v-for="tag in anchor.tagValue"
          :key="tag.id"
        >
          <span>{{ tag.name }}</span>
        </li>
      </ul>
      <span v-else>--</span>
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
  tag: 'Tag'
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
