<template>
  <div class="card-my-discoveries">
    <div class="title">
      {{ title }}&nbsp;
      <FeatherIcon
        class="iconHelp"
        :icon="Icons.Help"
        @click="$emit('showInstructions')"
      />
      <div class="count">({{ list.length }})</div>
    </div>
    <div
      class="list"
      v-if="list.length > 0"
    >
      <div
        v-for="item in list"
        :key="item.id"
        class="discovery-name pointer"
        :class="{ selected: selectedId == item.id }"
        @click="$emit('selectDiscovery', item)"
      >
        <div class="name">
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
            :toggle="(item as PassiveDiscovery).toggle"
            @toggle="(isToggled) => $emit('toggleDiscovery', item.id, isToggled)"
          />
        </FeatherTooltip>
      </div>
    </div>
    <div
      v-else
      class="empty"
    >
      <FeatherIcon
        :icon="Warning"
        class="icon"
      />{{ discoveryText.Discovery.empty }}
    </div>
  </div>
</template>

<script lang="ts" setup>
import Warning from '@featherds/icon/notification/Warning'
import { PassiveDiscovery, AzureActiveDiscovery, IcmpActiveDiscovery } from '@/types/graphql'
import Help from '@featherds/icon/action/Help'
import discoveryText from '@/components/Discovery/discovery.text'

const Icons = markRaw({
  Help
})
defineProps<{
  title: string
  list: (IcmpActiveDiscovery | AzureActiveDiscovery | PassiveDiscovery)[]
  passive?: boolean
  selectedId?: number
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
  padding: var(variables.$spacing-m) var(variables.$spacing-l);
  align-items: center;
  max-height: 55px;
  &.selected {
    color: var(variables.$secondary-variant);
    border-right: 3px var(variables.$secondary-variant) solid;
  }

  .name {
    overflow: hidden;
    text-overflow: ellipsis;
    text-transform: capitalize;
  }
}

.empty {
  display: flex;
  gap: 8px;
  padding: var(variables.$spacing-m);

  .icon {
    width: 24px;
    height: 24px;
    color: var(variables.$shade-1);
  }
}
</style>
