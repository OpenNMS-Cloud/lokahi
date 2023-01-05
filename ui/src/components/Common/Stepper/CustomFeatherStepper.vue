<!-- 
  Custom stepper built with feather components.
  Usage:

    <CustomFeatherStepper>
      <CustomFeatherStep @slideNext="someFn">Step 1 Content</CustomFeatherStep>
      <CustomFeatherStep @slidePrev="someOtherFn">Step 2 content</CustomFeatherStep>
      <CustomFeatherStep>Step 3 content</CustomFeatherStep>
    </CustomFeatherStepper>
 -->
<template>
  <div class="container">
    <div class="stepper-container">
      <template v-for="stepNum of stepNumbers">
        <!-- circular step element with number -->
        <div class="step" 
          :class="{ 
            'step-active': currentStep === stepNum, 
            'step-complete': currentStep > stepNum 
          }"
        >
          {{ stepNum }}
        </div>
        <!-- line between steps -->
        <hr v-if="stepNum !== stepNumbers.length" />
      </template>
    </div>

    <!-- CustomFeatherStep content rendered here -->
    <div id="content"></div>

    <div class="btns">
      <div>
        <FeatherButton primary @click="slidePrev" v-if="currentStep > 1">
          Prev
        </FeatherButton>
      </div>
      <div>
        <FeatherButton primary @click="slideNext" v-if="currentStep !== stepNumbers.length">
          Next
        </FeatherButton>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { render, VNode } from "vue"
const slots = useSlots()
const stepNumbers = ref<number[]>([])
const currentStep = ref(1)
let currentContent: VNode

const slideNext = () => {
  // check and use slideNext event passed from the step
  if (currentContent.props && currentContent.props.onSlideNext) {
    currentContent.props.onSlideNext()
  }

  currentStep.value++
  updateContent()
}

const slidePrev = () => {
  // check and use slidePrev event passed from the step
  if (currentContent.props && currentContent.props.onSlidePrev) {
    currentContent.props.onSlidePrev()
  }

  currentStep.value--
  updateContent()
}

const updateContent = () => {
  if (!slots.default) return
  const contentDiv = document.getElementById('content') as HTMLElement
  render(null, contentDiv) // unmount any previous VNode content
  currentContent = slots.default()[currentStep.value - 1] // get content as VNode
  render(currentContent, contentDiv) // render VNode
}

onMounted(() => {
  if (!slots.default) return
  // spread number of steps into array (using .length), start with 1 instead of 0
  stepNumbers.value = Array.from({ length: slots.default().length }, (_, i) => i + 1)
  updateContent()
})
</script>

<style scoped lang="scss">
@use "@featherds/styles/mixins/typography";
@use "@featherds/styles/themes/variables";
.container {
  display: flex;
  flex-direction: column;

  .stepper-container {
    display: flex;

    .step {
      @include typography.subtitle1;
      display: flex;
      border-radius: 50%;
      width: 35px;
      height: 35px;
      background: var(variables.$shade-3);
      color: var(variables.$primary-text-on-color);
      align-items: center;
      justify-content: center;
    }

    .step-active {
      background: var(variables.$primary)
    }

    .step-complete {
      background: var(variables.$success)
    }

    hr {
      flex-grow: 1;
      align-self: center;
      margin: 0px 15px;
      border: 1px solid var(variables.$shade-4);
    }
  }

  .btns {
    display: flex;
    justify-content: space-between;
  }
}
</style>
