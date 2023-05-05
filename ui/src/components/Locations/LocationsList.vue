<template>
  <div class="locations-list-wrapper">
    <HeadlineSection
      text="locations"
      data-test="headline"
    >
      <template #left>
        <CountColor
          :count="locationsList?.length"
          data-test="count"
        />
      </template>
      <template #right>
        <FeatherIcon
          :icon="icons.Help"
          data-test="icon-help"
        />
      </template>
    </HeadlineSection>
    <div class="locations-list">
      <div class="header">
        <div
          class="name"
          data-test="name"
        >
          Name
        </div>
        <div
          class="status"
          data-test="status"
        >
          Status
        </div>
      </div>
      <ul>
        <li
          v-for="(item, index) in locationsList"
          :key="index"
          data-test="card"
        >
          <LocationsCard :item="item" />
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import HeadlineSection from '@/components/Common/HeadlineSection.vue'
import Help from '@featherds/icon/action/Help'
import { LocationTemp } from '@/types/locations.d'

const props = defineProps<{
  items: LocationTemp[]
}>()

const locationsList = computed(() => props.items)

const icons = markRaw({
  Help
})
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@/styles/vars.scss';

.locations-list-wrapper {
  padding: var(variables.$spacing-m) var(variables.$spacing-s);
  background: var(variables.$surface);
  border-radius: vars.$border-radius-s;

  .header {
    display: flex;
    align-items: center;
    gap: var(variables.$spacing-s);
    padding: var(variables.$spacing-xs) var(variables.$spacing-s);
    background-color: var(variables.$background);
    > * {
      &:nth-child(1) {
        width: 40%;
      }
      &:nth-child(2) {
        width: 30%;
        display: flex;
        justify-content: center;
      }
    }
  }
}
</style>
