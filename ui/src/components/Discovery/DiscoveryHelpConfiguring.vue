<template>
  <section class="help-configuring">
    <div class="subtitle1">{{ DiscoverySyslogSNMPTrapsForm.help.top.heading }}</div>
    <ul class="type-square">
      <li>{{ DiscoverySyslogSNMPTrapsForm.help.top.list.text1 }}</li>
      <li>{{ DiscoverySyslogSNMPTrapsForm.help.top.list.text2 }}</li>
      <!-- Hidden until after EAR -->
      <!-- <li
        @click="isHelpOpen = !isHelpOpen"
        class="step-opener"
      >
        <span>
          {{ DiscoverySyslogSNMPTrapsForm.help.top.list.text3 }}
          <FeatherIcon :icon="isHelpOpen ? icon.ExpandLess : icon.ExpandMore" />
        </span>
      </li> -->
    </ul>
    <div
      v-if="isHelpOpen"
      class="steps"
    >
      <div class="stepper">
        <div
          @click="stepHandler(0)"
          class="arrow"
          :class="step === 1 ? 'disabled' : ''"
        >
          <FeatherIcon :icon="icon.ChevronLeft" />
        </div>
        <div class="step-line">
          <div class="line"></div>
          <ul>
            <li
              @click="stepHandler(1)"
              :class="step === 1 ? 'selected' : ''"
            >
              1
            </li>
            <li
              @click="stepHandler(2)"
              :class="step === 2 ? 'selected' : ''"
            >
              2
            </li>
            <li
              @click="stepHandler(3)"
              :class="step === 3 ? 'selected' : ''"
            >
              3
            </li>
            <li
              @click="stepHandler(4)"
              :class="step === 4 ? 'selected' : ''"
            >
              4
            </li>
          </ul>
        </div>
        <div
          @click="stepHandler(5)"
          class="arrow"
          :class="step === 4 ? 'disabled' : ''"
        >
          <FeatherIcon :icon="icon.ChevronRight" />
        </div>
      </div>
      <div
        v-if="step === 1"
        class="step-content"
      >
        <div class="subtitle2">{{ DiscoverySyslogSNMPTrapsForm.help.step1.heading }}</div>
        <ul class="type-square">
          <li>{{ DiscoverySyslogSNMPTrapsForm.help.step1.list.text1 }}</li>
          <li>{{ DiscoverySyslogSNMPTrapsForm.help.step1.list.text2 }}</li>
          <li>{{ DiscoverySyslogSNMPTrapsForm.help.step1.list.text3 }}</li>
        </ul>
      </div>
      <div
        v-if="step === 2"
        class="step-content"
      >
        <div class="subtitle2">{{ DiscoverySyslogSNMPTrapsForm.help.step2.heading }}</div>
        <ul class="type-square">
          <li>{{ DiscoverySyslogSNMPTrapsForm.help.step2.list.text1 }}</li>
          <li>{{ DiscoverySyslogSNMPTrapsForm.help.step2.list.text2 }}</li>
          <li>{{ DiscoverySyslogSNMPTrapsForm.help.step2.list.text3 }}</li>
        </ul>
      </div>
      <div
        v-if="step === 3"
        class="step-content"
      >
        <div class="subtitle2">{{ DiscoverySyslogSNMPTrapsForm.help.step3.heading }}</div>
        <ul class="type-square">
          <li>{{ DiscoverySyslogSNMPTrapsForm.help.step3.list.text1 }}</li>
          <li>{{ DiscoverySyslogSNMPTrapsForm.help.step3.list.text2 }}</li>
          <li>{{ DiscoverySyslogSNMPTrapsForm.help.step3.list.text3 }}</li>
        </ul>
      </div>
      <div
        v-if="step === 4"
        class="step-content"
      >
        <div class="subtitle2">{{ DiscoverySyslogSNMPTrapsForm.help.step4.heading }}</div>
        <ul class="type-square">
          <li>{{ DiscoverySyslogSNMPTrapsForm.help.step4.list.text1 }}</li>
          <li>{{ DiscoverySyslogSNMPTrapsForm.help.step4.list.text2 }}</li>
          <li>{{ DiscoverySyslogSNMPTrapsForm.help.step4.list.text3 }}</li>
        </ul>
      </div>
    </div>
  </section>
</template>

<script lang="ts" setup>
import ExpandMore from '@featherds/icon/navigation/ExpandMore'
import ExpandLess from '@featherds/icon/navigation/ExpandLess'
import ChevronLeft from '@featherds/icon/navigation/ChevronLeft'
import ChevronRight from '@featherds/icon/navigation/ChevronRight'
import { DiscoverySyslogSNMPTrapsForm } from './discovery.text'

const step = ref(1)
const isHelpOpen = ref(false)
const stepHandler = (s: number) => {
  switch (s) {
    case 0: // previous
      if (step.value !== 1) step.value = step.value - 1
      break
    case 5: // next
      if (step.value !== 4) step.value = step.value + 1
      break
    default:
      step.value = s
  }
}

const icon = markRaw({
  ExpandMore,
  ExpandLess,
  ChevronLeft,
  ChevronRight
})
</script>

<style lang="scss" scoped>
@use '@featherds/styles/mixins/typography';
@use '@featherds/styles/themes/variables';
@use '@/styles/mediaQueriesMixins.scss';

// TODO: convert steppers to carousel alike
.help-configuring {
  border-left: 1px solid var(variables.$border-on-surface);
  padding-left: var(variables.$spacing-m);
  margin-left: var(variables.$spacing-xl);

  .subtitle1 {
    @include typography.subtitle1();
    margin-bottom: var(variables.$spacing-m);
  }
  .subtitle2 {
    @include typography.subtitle2();
    margin-bottom: var(variables.$spacing-m);
  }
  ul.type-square > li {
    list-style-type: square;
  }
  > ul.type-square {
    margin-left: var(variables.$spacing-xl);
    margin-bottom: var(variables.$spacing-xxl);
  }
  .step-opener {
    > span {
      color: var(variables.$secondary-variant);
      &:hover {
        cursor: pointer;
      }
      .feather-icon {
        width: var(variables.$spacing-l);
        height: var(variables.$spacing-l);
        vertical-align: bottom;
      }
    }
  }
  .steps {
    margin-left: var(variables.$spacing-xl);
    padding-bottom: var(variables.$spacing-m);
    margin-bottom: var(variables.$spacing-xl);
    .stepper {
      display: flex;
      margin-bottom: var(variables.$spacing-m);
      .arrow {
        width: 10%;
        display: flex;
        align-items: center;
        justify-content: center;
        &:hover {
          cursor: pointer;
        }
      }
      .step-line {
        width: 80%;
        position: relative;
        .line {
          border: 1px solid var(variables.$border-on-surface);
          background-color: var(variables.$background);
          width: 100%;
          position: absolute;
          top: var(variables.$spacing-m);
          left: 0;
          z-index: 1000;
        }
        > ul {
          display: flex;
          justify-content: space-between;
          position: relative;
          z-index: 1001;
          > li {
            border: 1px solid var(variables.$border-on-surface);
            border-radius: 50%;
            width: 2rem;
            height: 2rem;
            text-align: center;
            padding-top: 2px;
            color: var(variables.$secondary-text-on-surface);
            background-color: var(variables.$surface);
            &:hover {
              cursor: pointer;
            }
          }
        }
        .selected {
          background-color: var(variables.$background);
        }
      }
      .disabled {
        color: var(variables.$disabled-text-on-surface);
        &:hover {
          cursor: default;
        }
      }
      .feather-icon {
        width: var(variables.$spacing-l);
        height: var(variables.$spacing-l);
        vertical-align: bottom;
      }
    }
    .step-content {
      margin-right: var(variables.$spacing-xl);
      margin-left: var(variables.$spacing-xl);
      > ul {
        margin-left: var(variables.$spacing-m);
      }
    }
  }
}
</style>
