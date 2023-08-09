<template>
  <ul class="text-anchor-list">
    <li data-test="location">
      <label :for="label.location">{{ label.location }}: </label>
     
      <span  :id="label.location">{{ location }}</span>
    </li>
    <li data-test="management-ip">
      <label :for="label.managementIp">{{ label.managementIp }}: </label>
      <span  :id="label.managementIp">{{ ipAddress }}</span>
    </li>
  </ul>
</template>

<script lang="ts" setup>

defineProps<{
  location: string,
  ipAddress: string
}>()

const label = {
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
  font-family: var(--feather-header-font-family);
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
