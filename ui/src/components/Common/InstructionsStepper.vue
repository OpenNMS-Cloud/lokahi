<template>
  <section class="instructions-stepper">
    <button
      @click="isExpand = !isExpand"
      class="btn-open-toggle"
      data-test="btn-open-toggle"
    >
      <span>{{ textButton }}</span>
      <FeatherIcon
        :icon="isExpand ? icon.ExpandLess : icon.ExpandMore"
        aria-hidden="true"
        focusable="false"
      />
    </button>
    <div
      v-if="isExpand"
      class="steps"
    >
      <div
        class="stepper"
        data-test="stepper"
      >
        <!-- first item -->
        <div
          @click="step = 0"
          :class="{ disabled: step === 0 }"
          class="arrow"
        >
          <FeatherIcon :icon="icon.ChevronLeft" />
        </div>
        <div class="step-line">
          <div class="line" />
          <ul>
            <li
              v-for="(_, index) in stepLists"
              :key="index"
              @click="step = index"
              :class="{ selected: step === index }"
              data-test="step-selector"
            >
              {{ index + 1 }}
            </li>
          </ul>
        </div>
        <!-- last item -->
        <div
          @click="step = stepLists.length - 1"
          :class="{ disabled: step === stepLists.length - 1 }"
          class="arrow"
        >
          <FeatherIcon :icon="icon.ChevronRight" />
        </div>
      </div>

      <div
        v-for="(list, index) in stepLists"
        :key="index"
        :class="{ active: step === index }"
        class="step-content"
      >
        <div
          class="subtitle"
          data-test="step-content-subtitle"
        >
          {{ list.title }}
        </div>
        <ul
          class="type-square"
          data-test="step-content-list"
        >
          <li
            v-for="(item, index) in list.items"
            :key="index"
          >
            {{ item }}
          </li>
        </ul>
      </div>
    </div>
  </section>
</template>

<script lang="ts" setup>
import { PropType } from 'vue'
import ExpandMore from '@featherds/icon/navigation/ExpandMore'
import ExpandLess from '@featherds/icon/navigation/ExpandLess'
import ChevronLeft from '@featherds/icon/navigation/ChevronLeft'
import ChevronRight from '@featherds/icon/navigation/ChevronRight'

interface StepContent {
  title: string
  items: string[]
}

defineProps({
  textButton: {
    type: String,
    required: true
  },
  stepLists: {
    type: Array as PropType<StepContent[]>,
    required: true
  }
})

const step = ref(0)
const isExpand = ref(false)

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

.btn-open-toggle {
  color: var(variables.$primary);
  border: none;
  background-color: transparent;
  padding: 0;
  margin-bottom: var(variables.$spacing-l);
  &:hover {
    cursor: pointer;
  }
  .feather-icon {
    width: var(variables.$spacing-l);
    height: var(variables.$spacing-l);
    vertical-align: middle;
  }
}

.steps {
  padding-bottom: var(variables.$spacing-m);
  margin-bottom: var(variables.$spacing-xl);
  .stepper {
    display: flex;
    margin-bottom: var(variables.$spacing-m);
    .arrow {
      width: 5%;
      display: flex;
      align-items: center;
      justify-content: center;
      &:hover {
        cursor: pointer;
      }
    }
    .step-line {
      width: 90%;
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
          background-color: var(variables.$background);
          &:hover {
            cursor: pointer;
          }
        }
      }
      .selected {
        background-color: var(variables.$surface);
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
    display: none;
    margin-right: var(variables.$spacing-xxl);
    margin-left: var(variables.$spacing-xxl);
    &.active {
      display: block;
    }
    > ul {
      margin-left: var(variables.$spacing-m);
    }
  }
}

// TODO: convert steppers to carousel alike
.instructions-stepper {
  border-left: 1px solid var(variables.$border-on-surface);
  padding-left: var(variables.$spacing-m);
  margin-left: var(variables.$spacing-xl);

  .subtitle {
    @include typography.subtitle2();
    margin-bottom: var(variables.$spacing-m);
  }

  ul.type-square {
    margin-left: var(variables.$spacing-xl);
    > li {
      list-style-type: square;
    }
  }
}
</style>
