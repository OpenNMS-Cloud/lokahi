<template>
  <div class="list" v-for="item in list">
    <div class="card">
      {{ item.name }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { IPolicy, IRule } from '@/types/policies'
defineProps<{
  list: IPolicy[] | IRule[]
}>()
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';

.list {
  display: flex;
  margin-top: var(variables.$spacing-l);
  > .card {
    @include typography.body-small;
    text-align: center;
    padding: var(variables.$spacing-s);
    border: 1px solid var(variables.$shade-4);
    background-color: var(variables.$shade-4);
    margin-right: var(variables.$spacing-m);
    width: 240px;
    border-radius: 5px;
    font-size: 15px;
  }
}
</style>
