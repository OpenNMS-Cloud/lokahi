<template>
  <div class="welcome-wrapper">
    <div :class="['gradiant', welcomeStore.doneGradient ? 'loaded' : '']" data-test="gradiant-bg">
      <GradiantBG />
    </div>
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
import GradiantBG from '../components/Common/GradiantBG.vue'
import useTheme from '@/composables/useTheme'
const welcomeStore = useWelcomeStore()
const { isDark } = useTheme();
</script>
<style lang="scss" scoped>
@import '@featherds/styles/themes/variables';
@import '@featherds/styles/mixins/typography';
@import '@featherds/table/scss/table';

.welcome-wrapper {
  position: fixed;
  z-index: 8001;
  top: 0;
  left: 0;
  width: 100%;
  height: 100vh;
  overflow-y: auto;
  background-color: var($background);
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
  max-width: 660px;
  padding-top: 132px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}

.welcome-logo svg {
  max-width: 160px;
  margin-bottom: 24px;
}

.gradiant {
  position: fixed;
  z-index: 0;
  left: 0;
  top: 0;
  width: 100%;
  height: 100vh;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.4s ease-in-out;
}

.gradiant.loaded {
  opacity: 1;
}
</style>
