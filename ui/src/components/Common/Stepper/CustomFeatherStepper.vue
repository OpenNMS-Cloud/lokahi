<!--
  Custom stepper built with feather components.
  Usage:

    <CustomFeatherStepper>
      <CustomFeatherStep>Step 1 Content</CustomFeatherStep>
      <CustomFeatherStep>Step 2 Content</CustomFeatherStep>
      <CustomFeatherStep @slideNext="callApi">Step 3 Content</CustomFeatherStep>
      <CustomFeatherStep nextBtnText="Exit">Step 4 content</CustomFeatherStep>
    </CustomFeatherStepper>

  Optional event hooks for CustomFeatherStep:
  @slideNext
  @slidePrev

  Optional props for CustomFeatherStep:
  nextBtnText (default 'Next')
  prevBtnText (default 'Prev')
  hideNextBtn (default false),
  disableNextBtn (default false)
  title (no default)
 -->
<template>
  <div class="container">
    <div class="stepper-container">
      <template v-for="stepNum of stepNumbers" :key="stepNum">
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
        <FeatherButton primary @click="slidePrev" v-if="currentStep > 1" data-test="prev-btn">
          {{ prevBtnText }}
        </FeatherButton>
      </div>
      <div>
        <FeatherButton v-if="!hideNextBtn" :disabled="disableNextBtn" primary @click="slideNext" data-test="next-btn">
          {{ nextBtnText }}
        </FeatherButton>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { render, VNode } from 'vue'

const slots = useSlots()
const stepNumbers = ref<number[]>([])
const currentStep = ref(1)
const currentContent = ref<VNode>()

const prevBtnText = computed(() => currentContent.value?.props?.prevBtnText || 'Prev')

const nextBtnText = computed(() => {
  let text = 'Next'
  if (currentStep.value === stepNumbers.value.length) text = 'Finish'
  return currentContent.value?.props?.nextBtnText || text
})

const hideNextBtn = computed(() => {
  if (slots.default) {
    // must call default slot to keep hideNextBtn prop reactive
    return slots.default()[currentStep.value - 1].props?.hideNextBtn
  }
})

const disableNextBtn = computed(() => {
  if (slots.default) {
    // must call default slot to keep disableNextBtn prop reactive
    return slots.default()[currentStep.value - 1].props?.disableNextBtn
  }
})

// exposed for explicit manual use
const next = () => {
  if (currentStep.value === stepNumbers.value.length) return
  currentStep.value++
  updateContent()
}

// exposed for explicit manual use
const prev = () => {
  if (currentStep.value === 1) return
  currentStep.value--
  updateContent()
}

const slideNext = () => {
  // check and use slideNext event passed from the step
  if (currentContent.value?.props && currentContent.value?.props.onSlideNext) {
    currentContent.value.props.onSlideNext()
    return
  }

  // if last step, don't increase step count
  if (currentStep.value === stepNumbers.value.length) return

  currentStep.value++
  updateContent()
}

const slidePrev = () => {
  // check and use slidePrev event passed from the step
  if (currentContent.value?.props && currentContent.value?.props.onSlidePrev) {
    currentContent.value.props.onSlidePrev()
    return
  }

  currentStep.value--
  updateContent()
}

const updateContent = () => {
  if (!slots.default) return
  const contentDiv = document.getElementById('content') as HTMLElement
  render(null, contentDiv) // unmount any previous VNode content
  currentContent.value = slots.default()[currentStep.value - 1] // get content as VNode
  render(currentContent.value, contentDiv) // render VNode
}

onMounted(() => {
  if (!slots.default) return
  // spread number of steps into array (using .length), start with 1 instead of 0
  stepNumbers.value = Array.from({ length: slots.default().length }, (_, i) => i + 1)
  updateContent()
})

defineExpose({
  next,
  prev
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
