<template>
  <FeatherDrawer
    :modelValue="isOpen"
    :labels="{ close: 'close', title: 'Discovery' }"
    @update:modelValue="$emit('drawer-closed')"
  >
    <div class="drawerContent">
      <div class="section" v-if="instructionsType === InstructionsType.Location">
        <div class="title">{{ Instructions.locTitle }}</div>
        {{ Instructions.locInfo }}
      </div>
      <div class="section" v-if="instructionsType === InstructionsType.Minion">
        <div class="title">{{ Instructions.minTitle }}</div>
        {{ Instructions.minInfo }}
      </div>
    </div>
  </FeatherDrawer>
</template>

<script setup lang="ts">
import { FeatherDrawer } from '@featherds/drawer'
import { Instructions } from './locations.text'
import { InstructionsType } from './locations.constants'
const props = defineProps<{
  isOpen: boolean
  instructionsType: string
}>()

const isOpen = computed<boolean>(() => props.isOpen)
</script>

<style lang="scss">
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';
@use '@/styles/mediaQueriesMixins.scss';

.drawerContent {
  padding: var(variables.$spacing-xl);
  white-space: pre-wrap;
  @include typography.caption;

  @include mediaQueriesMixins.screen-md {
    width: 575px;
  }
  > .section {
    padding-bottom: var(variables.$spacing-m);
    > .title {
      @include typography.headline4;
    }
  }
}
</style>
