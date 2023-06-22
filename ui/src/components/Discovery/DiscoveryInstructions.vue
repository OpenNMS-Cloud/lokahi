<template>
  <FeatherDrawer
    :modelValue="isOpen"
    :labels="{ close: 'close', title: 'Discovery' }"
    @update:modelValue="$emit('drawer-closed')"
  >
    <div class="drawerContent">
      <div class="section">
        <div class="title">{{ Instructions.title }}</div>
        {{ Instructions.subtitle }}
      </div>
      <br />
      <div class="section" v-if="instructionsType === InstructionsType.Active">
        <div class="title">{{ Instructions.activeDiscoveryTitle }}</div>
        {{ Instructions.activeDiscoverySubtitle }}
        <ul class="list">
          <li>
            <strong>{{ Instructions.activeListTool.tool1 }}</strong> {{ Instructions.activeListTool.toolDescription1 }}
          </li>
          <li>
            <strong>{{ Instructions.activeListTool.tool2 }}</strong> {{ Instructions.activeListTool.toolDescription2 }}
          </li>
        </ul>
        {{ Instructions.activeNote }}
        <ul class="list">
          <li>
            <strong>{{ Instructions.activeListCharacteristics.benefits }}</strong>
            {{ Instructions.activeListCharacteristics.benefitsDescription }}
          </li>
          <li>
            <strong>{{ Instructions.activeListCharacteristics.disadvantages }}</strong>
            {{ Instructions.activeListCharacteristics.disadvantagesDescription }}
          </li>
        </ul>
      </div>
      <div class="section" v-if="instructionsType === InstructionsType.Passive">
        <div class="title">{{ Instructions.passiveDiscoveryTitle }}</div>

        {{ Instructions.passiveDiscoverySubtitle }}

        <p>{{ Instructions.passiveNote }}</p>

        <ul class="list">
          <li>
            <strong>{{ Instructions.passiveListCharacteristics.benefits }}</strong>
            {{ Instructions.passiveListCharacteristics.benefitsDescription }}
          </li>
          <li>
            <strong>{{ Instructions.passiveListCharacteristics.disadvantages }}</strong>
            {{ Instructions.passiveListCharacteristics.disadvantagesDescription }}
          </li>
        </ul>
      </div>
      <div class="section">
        <a
          :href="Instructions.learnMoreLink.link"
          target="_blank"
          class="link"
          >{{ Instructions.learnMoreLink.label }}</a
        >
      </div>
    </div>
  </FeatherDrawer>
</template>

<script setup lang="ts">
import { FeatherDrawer } from '@featherds/drawer'
import { Instructions } from './discovery.text'
import { InstructionsType } from './discovery.constants';
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
  padding: var(variables.$spacing-m);
  width: 100%;
  @include typography.caption;

  @include mediaQueriesMixins.screen-md {
    width: 600px;
    padding: var(variables.$spacing-xl);
  }
  > .section {
    padding-bottom: var(variables.$spacing-m);
    > .title {
      @include typography.headline4;
    }
    > .list {
      padding-top: var(variables.$spacing-xl);
      padding-left: var(variables.$spacing-m);
      margin-left: var(variables.$spacing-xs);
      padding-bottom: var(variables.$spacing-xl);
      @include mediaQueriesMixins.screen-md {
        padding-left: var(variables.$spacing-xl);
      }
    }
    li {
      list-style: disc;
    }
    .link {
      @include typography.button;
      display: flex;
      justify-content: flex-end;
      cursor: pointer;
      color: var(variables.$primary);
      text-decoration: none;
    }
    p {
      @include typography.caption;
      padding-top: var(variables.$spacing-s);
    }
  }

  strong {
    @include typography.header;
  }
}
</style>
