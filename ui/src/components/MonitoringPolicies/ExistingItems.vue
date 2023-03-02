<template>
  <div class="existing-items-container">
    <div class="title">
      <div>{{ title }}</div>
      <FeatherButton icon="info">
        <FeatherIcon :icon="infoIcon" />
      </FeatherButton>
    </div>
    <div class="list" v-for="item in list">
      <div class="card">
        {{ item.name }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import Info from '@featherds/icon/action/Info'
import { IPolicy, IRule } from '@/types/policies'

const infoIcon = markRaw(Info)

defineProps<{
  title: String,
  list: Partial<IPolicy>[] | Partial<IRule>[]
}>()
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';
@use "@featherds/styles/mixins/elevation";

.existing-items-container {
  @include elevation.elevation(2);
  display: flex;
  flex-direction: column;
  background: var(variables.$surface);
  margin-top: var(variables.$spacing-l);
  padding: var(variables.$spacing-l);
  width: 350px;
  border-radius: 5px;
  border-left: 5px solid var(variables.$success);

  .title {
    @include typography.headline3;
    display: flex;
    justify-content: space-between;
  }

  .list {
    display: flex;
    margin-top: var(variables.$spacing-xs);

    .card {
      @include typography.headline4;
      width: 100%;
      border: 1px solid var(variables.$shade-3);
      border-radius: 10px;
      padding: var(variables.$spacing-xs) var(variables.$spacing-m);
    }
  }
}
</style>
