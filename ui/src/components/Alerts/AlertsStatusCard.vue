<template>
  <div class="card">
    <div class="label-add-icon">
      <PillColor :item="pillColor" />
    </div>
    <div class="count">
      {{ count || 0 }}
    </div>
  </div>
</template>

<script lang="ts" setup>
const props = defineProps<{
  status: string
  count: number
}>()

const pillColor = { style: props.status }
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';
@use '@/styles/mediaQueriesMixins.scss';
@use '@/styles/vars.scss';

.card {
  display: flex;
  flex-direction: column;
  background-color: var(variables.$surface);
  padding: var(variables.$spacing-s);

  &:not(:last-child) {
    border-right: 1px solid var(variables.$shade-4);
  }
}

.label-add-icon {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--feather-spacing-xl);
  > .icon {
    font-size: 24px;
  }
  > *:first-child {
    margin-right: var(variables.$spacing-l);
  }
}

.count {
  font-size: var(variables.$display3-font-size);
  margin-bottom: var(variables.$spacing-l);
  font-family: var(variables.$header-font-family);
  color: var(variables.$primary-text-on-surface);
}
</style>
