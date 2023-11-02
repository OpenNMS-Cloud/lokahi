<template>
  <div class="card-my-discoveries">
    <div class="title">{{ title }}&nbsp;</div>
    <div
      class="list"
      v-if="list.length > 0"
    >
      <div
        v-for="(item, index) in list"
        :key="index"
        class="discovery-name pointer"
        :class="{ selected: selectedId == item.id && item.type === selectedType }"
      >
        <div
          class="name pointer"
          @click="() => selectDiscovery(item)"
        >
          {{ item.name }}
        </div>
        <FeatherTooltip
          :title="`Toggle ${item.name} on/off`"
          v-slot="{ attrs, on }"
        >
          <BasicToggle
            v-bind="attrs"
            v-on="on"
            v-if="passive"
            :toggle="(item?.meta as DiscoveryTrapMeta).toggle?.toggle"
            @toggle="
              () => {
                toggleDiscovery && toggleDiscovery(item)
              }
            "
          />
        </FeatherTooltip>
      </div>
    </div>
    <div
      v-else
      class="empty"
    >
      {{ discoveryText.Discovery.empty }}
    </div>
  </div>
</template>

<script lang="ts" setup>
import discoveryText from '@/components/Discovery/discovery.text'
import { DiscoveryTrapMeta, NewOrUpdatedDiscovery } from '@/types/discovery'

defineProps<{
  title: string
  list: NewOrUpdatedDiscovery[]
  selectDiscovery: (discovery: NewOrUpdatedDiscovery) => void
  showInstructions: () => void
  toggleDiscovery?: (discovery: NewOrUpdatedDiscovery) => void
  passive?: boolean
  selectedId?: number
  selectedType?: string
}>()
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';
@use '@/styles/vars.scss';
@import '@/styles/mediaQueriesMixins.scss';

.card-my-discoveries {
  background-color: var(variables.$surface);
  border: 1px solid var(variables.$border-on-surface);
  border-radius: vars.$border-radius-surface;
  min-height: 100px;
  max-width: 288px;
}

.title {
  @include typography.headline4;
  display: flex;
  padding: var(variables.$spacing-l);
  border-bottom: 1px solid var(variables.$border-on-surface);
  align-items: center;
  .count {
    margin-left: var(variables.$spacing-xs);
    @include typography.body-large;
    color: var(variables.$shade-1);
  }
  > .iconHelp {
    font-size: 22px;
    cursor: pointer;
    color: var(variables.$shade-2);
  }
}

.list {
  > div {
    border-bottom: 1px solid var(variables.$border-on-surface);
  }
  > div:last-child {
    border-bottom: none;
  }
}

.discovery-name {
  display: flex;
  justify-content: space-between;
  @include typography.subtitle1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  align-items: center;
  max-height: 55px;
  &.selected {
    color: var(variables.$secondary-variant);
    border-right: 3px var(variables.$secondary-variant) solid;
    background-color: #e7e9f8;
  }

  .name {
    overflow: hidden;
    text-overflow: ellipsis;
    padding: var(variables.$spacing-m) var(variables.$spacing-l);
  }
}

.empty {
  display: flex;
  gap: 8px;
  padding: var(variables.$spacing-m);
  font-weight: 700;
  font-family: var(--feather-header-font-family);
  color: var(--feather-disabled-text-on-surface);
  opacity: 0.4;
  font-size: 13px;
  .icon {
    width: 24px;
    height: 24px;
    color: var(variables.$shade-1);
  }
}
</style>
