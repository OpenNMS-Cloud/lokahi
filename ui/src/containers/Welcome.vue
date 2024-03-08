<template>
  <div class="welcome-wrapper">
    <div :class="['gradient', welcomeStore.doneGradient ? 'loaded' : '']" data-test="gradient-bg"></div>
    <div :class="['welcome-contain', welcomeStore.doneLoading ? 'loaded' : '']">
      <div class="welcome-inner">
        <div class="welcome-logo" data-test="welcome-logo">
          <LogoIcon v-if="!isDark" />
          <LogoDarkIcon v-if="isDark" />
        </div>
        <WelcomeSlideOne :visible="welcomeStore.slide === 1" />
        <WelcomeSlideTwo :visible="welcomeStore.slide === 2" />
        <WelcomeSlideThree :visible="welcomeStore.slide === 3" />
      </div>
    </div>
  </div>
</template>
<script lang="ts" setup>
import LogoIcon from '@/assets/OpenNMS_Horizontal-Logo_Light-BG.svg'
import LogoDarkIcon from '@/components/Common/LogoDarkIcon.vue'
import WelcomeSlideOne from '../components/Welcome/WelcomeSlideOne.vue'
import WelcomeSlideTwo from '../components/Welcome/WelcomeSlideTwo.vue'
import WelcomeSlideThree from '../components/Welcome/WelcomeSlideThree.vue'
import { useWelcomeStore } from '@/store/Views/welcomeStore'
import useTheme from '@/composables/useTheme'

const welcomeStore = useWelcomeStore()
const { isDark } = useTheme()

const fullSequence = ['ArrowLeft', 'ArrowRight', 'ArrowLeft', 'ArrowLeft', 'ArrowLeft', 'ArrowLeft', 'ArrowRight', 'ArrowRight', 'ArrowRight']
const inputtedSequence = ref<string[]>([])
const router = useRouter()

/**
 * Used to verify if we should be overriding the Welcome Guide
 * How to use:
 * 1. After loading the welcome guide, give the window/document focus.
 * 2. Enter they keypress sequence listed above (Left,Right,Left,Left,Left,Left,Right,Right,Right)
 * 3. You should now have a session variable set to override the welcome guide.
 * 4. You should be redirected now to the dashboard, and the Welcome Guide Closed.
 * 5. Page reloads will not cause the welcome guide to load.
 * 6. To disable the flag set by this sequence, navigate back to the welcome guide (/welcome) and enter the sequence again.
 * @param keyValue A keyboard event from 'keyup' on addEventListener
 */
const overrideChecker = (keyValue: KeyboardEvent) => {
  let clear = false
  let totalExact = 0

  // Store the keypress key
  inputtedSequence.value.push(keyValue.key)
  for (let i = 0; i < inputtedSequence.value.length; i++) {
    // If we have something that doesn't match, we clear the sequence so the user can restart.
    if (inputtedSequence.value[i] !== fullSequence[i]) {
      clear = true
    } else {
      totalExact += 1
    }
  }

  // We found an incorrect value in the sequence, reset.
  if (clear) {
    inputtedSequence.value = []
  }

  // The sequence is identical. Toggle the override value.
  if (totalExact === fullSequence.length) {
    if (sessionStorage.getItem('welcomeOverride') === 'true') {
      sessionStorage.setItem('welcomeOverride', 'false')
    } else {
      sessionStorage.setItem('welcomeOverride', 'true')
      router.push('/')
    }
  }
}

onMounted(() => {
  window.addEventListener('keyup', overrideChecker)
  if (sessionStorage.getItem('welcomeOverride') === 'true') {
    welcomeStore.init()
  }
})

onUnmounted(() => {
  window.removeEventListener('keyup', overrideChecker)
})
</script>

<style lang="scss" scoped>
@import '@featherds/styles/themes/variables';
@import '@featherds/styles/mixins/typography';
@import '@featherds/table/scss/table';

.welcome-wrapper {
  position: fixed;
  z-index: 1070;
  top: 0;
  left: 0;
  width: 100%;
  height: 100vh;
  overflow-y: auto;
  background-color: var($surface);
}

.welcome-contain {
  opacity: 0;
  transform: translateY(4px);
  transition: opacity 0.9s ease-in-out, transform 0.5s ease-in-out 0s;
}

.welcome-contain.loaded {
  opacity: 1;
  transform: translateY(0px);
}

.welcome-inner {
  padding-top: 132px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
  max-width: calc(100% - 20px);
  padding-left: 10px;
  padding-right: 10px;

  @media (min-width:700px) {
    max-width: 660px;
    padding-left: 0;
    padding-right: 0;
  }
}

.welcome-logo svg {
  max-width: 160px;
  margin-bottom: 24px;
}

.gradient {
  position: fixed;
  width: 100%;
  height: 100vh;
  pointer-events: none;
  background-image: url( '@/assets/WelcomeScreen.svg' );
  background-size: 100% 100%;
  opacity: 0;
  transition: opacity 0.4s ease-in-out;
}

.gradient.loaded {
  opacity: 1;
}
</style>
