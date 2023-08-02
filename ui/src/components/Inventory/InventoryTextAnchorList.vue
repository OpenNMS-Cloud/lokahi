<template>
  <ul class="text-anchor-list">
    <li data-test="location">
      <label :for="label.location">{{ label.location }}: </label>
      <a v-if="anchor.locationLink" @click="goto(anchor.locationLink)" :id="label.location">{{ anchor?.locationValue
      }}</a>
      <span v-else :id="label.location">{{ anchor.locationValue }}</span>
    </li>
    <li data-test="management-ip">
      <label :for="label.managementIp">{{ label.managementIp }}: </label>
      <a v-if="anchor.managementIpLink" @click="goto(anchor.managementIpLink)" :id="label.managementIp">{{
        anchor.managementIpValue }}</a>
      <span v-else :id="label.managementIp">{{ anchor.managementIpValue }}</span>
    </li>
  </ul>
</template>

<script lang="ts" setup>
import { Anchor } from '@/types/inventory'

const router = useRouter()

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
@import '@featherds/styles/themes/variables';
@import '@featherds/styles/mixins/typography';

.text-anchor-list {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  @include caption();
  font-family: Inter;
}

.tag-list {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;

  ul {
    margin-left: var($spacing-xxs);
  }
}
</style>
