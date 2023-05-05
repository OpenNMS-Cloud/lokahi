<template>
  <div class="minions-list-wrapper">
    <HeadlineSection
      text="Minions"
      data-test="headline"
    >
      <template #left>
        <CountColor
          :count="minions?.length"
          data-test="count"
        />
      </template>
      <template #middle>
        <FeatherInput
          @update:model-value="searchMinionsListener"
          label="Search Minion"
          type="search"
          class="search-minions-input"
          data-test="search-input"
        >
          <template #pre>
            <FeatherIcon :icon="icons.Search" />
          </template>
        </FeatherInput>
      </template>
      <template #right>
        <FeatherIcon
          :icon="icons.Help"
          class="icon-help"
          data-test="icon-help"
        />
      </template>
    </HeadlineSection>
    <ul
      v-if="minions?.length"
      class="minions-list"
    >
      <li
        v-for="minion in minionsList"
        :key="minion.id"
      >
        <LocationsMinionsCard :item="minion" />
      </li>
    </ul>
    <EmptyList
      v-else
      :content="emptyListContent"
      data-test="empty-list"
    />
  </div>
</template>

<script setup lang="ts">
import HeadlineSection from '@/components/Common/HeadlineSection.vue'
import Help from '@featherds/icon/action/Help'
import Search from '@featherds/icon/action/Search'
import { useLocationStore } from '@/store/Views/locationStore'

const props = defineProps({
  minions: {
    type: Array,
    required: true
  }
})

const minionsList = computed(() => props.minions)

const locationStore = useLocationStore()

const searchMinionsListener = (val: string | number | undefined) => {
  locationStore.searchMinions(val as string)
}

const emptyListContent = {
  msg: 'No minions found.'
}

const icons = markRaw({
  Help,
  Search
})
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@/styles/vars.scss';
@use '@/styles/mixins.scss';

.minions-list-wrapper {
  padding: var(variables.$spacing-m) var(variables.$spacing-s);
  background: var(variables.$surface);
  border-radius: vars.$border-radius-s;

  .search-minions-input {
    width: 100%;
    :deep(.feather-input-sub-text) {
      display: none;
    }
  }

  .minions-list {
    > li {
      margin-bottom: var(variables.$spacing-s);
      &:last-child {
        margin-bottom: 0;
      }
    }
  }
}
</style>
