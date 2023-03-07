<template>
  <div class="card-my-discoveries">
    <div class="title">
      {{ title }}
      <div class="count">{{ list.length }}</div>
    </div>
    <div v-if="list.length > 0">
      <div
        v-for="item in list"
        :key="item.id"
        class="discovery-name"
      >
        <div @click="$emit('selectDiscovery', item)" class="pointer">
          {{ item.configName?.toUpperCase() || item.name?.toUpperCase() }}
        </div>
        <BasicToggle v-if="passive" :toggle="item.toggle" @toggle="(isToggled) => $emit('toggleDiscovery', item.id, isToggled)" />
      </div>
    </div>
    <div
      v-else
      class="empty"
    >
      <FeatherIcon
        :icon="Warning"
        class="icon"
      />You have no {{ passive? 'passive' : 'active '}} discovery
    </div>
  </div>
</template>

<script lang="ts" setup>
import Warning from '@featherds/icon/notification/Warning'

defineProps<{
  title: string
  list?: any
  passive?: boolean
}>()
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
@use '@/styles/mediaQueriesMixins.scss';
@use '@/styles/vars.scss';
@use '@featherds/styles/mixins/typography';

.card-my-discoveries {
  background-color: var(variables.$surface);
  border: 1px solid var(variables.$border-on-surface);
  border-radius: vars.$border-radius-s;
  padding: var(variables.$spacing-s);
  min-height: 100px;
}

.title {
  @include typography.subtitle1;
  display: flex;
  margin-bottom: var(variables.$spacing-xs);

  .count {
    background-color: #00666d1f;
    padding: 0 var(variables.$spacing-xs);
    margin-left: var(variables.$spacing-m);
    border-radius: 5px;
    @include typography.body-small;
  }
}

.discovery-name {
  display: flex;
  justify-content: space-between;
  @include typography.headline4;
  color: var(variables.$secondary-variant);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.empty {
  display: flex;
  gap: 8px;
  margin-top: var(variables.$spacing-s);

  .icon {
    width: 24px;
    height: 24px;
    color: var(variables.$shade-1);
  }
}
</style>
