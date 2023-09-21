<template>
  <FeatherDialog
    :hide-close="true"
    :labels="{ title: ' ', close: '' }"
    :modelValue="discoveryStore.newDiscoveryModalActive"
    @update:modelValue="discoveryStore.closeNewModal"
  >
    <div class="new-modal">
      <div class="icon">
        <div :class="['pulse', pulseActive ? 'active' : '']" />
        <div class="icon-inner">
          <div class="icon-bg" />
          <div class="icon-actual">
            <FeatherIcon :icon="CheckCircle" />
          </div>
        </div>
      </div>
      <h2 class="title">{{ discoveryStore.selectedDiscovery.name }} setup successful.</h2>
      <p class="subtitle">View your discovered devices in the inventory screen or perform another discovery.</p>
      <div class="buttons">
        <div class="buttons-inner">
          <FeatherButton
            text
            @click="discoveryStore.backToDiscovery"
            >Back to Discovery</FeatherButton
          >
          <FeatherButton
            primary
            @click="navigateToInventory"
            >Go To Inventory</FeatherButton
          >
        </div>
      </div>
    </div>
  </FeatherDialog>
</template>
<script setup lang="ts">
import CheckCircle from '@featherds/icon/action/CheckCircle'
import { useDiscoveryStore } from '@/store/Views/discoveryStore'

const discoveryStore = useDiscoveryStore()
const router = useRouter()
const navigateToInventory = () => {
  router.push('/inventory')
}
const pulseActive = ref(false)
watchEffect(() => {
  if (discoveryStore.newDiscoveryModalActive) {
    setTimeout(() => {
      pulseActive.value = true
    }, 10)
  }
})
</script>
<style lang="scss" scoped>
@import '@featherds/styles/mixins/elevation';
.new-modal {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: column;
  padding: 32px;
}
.icon {
  position: relative;
  margin-bottom: 62px;
  .icon-inner {
    font-size: 72px;
    border-radius: 50%;
    @include elevation(4);
    padding: 20px;
  }
  .icon-bg {
    background-color: #72d898;
    width: 58px;
    height: 58px;
    border-radius: 50%;
    position: absolute;
    top: 27px;
    left: 27px;
    z-index: 0;
  }
  .icon-actual {
    color: white;
    z-index: 1;
    position: relative;
  }
}
.pulse {
  background: #e9ebf9;
  position: absolute;
  width: 175px;
  height: 175px;
  left: -32px;
  top: -32px;
  z-index: -1;
  border-radius: 50%;
}

@keyframes bounce {
  0% {
    transform: scale(0);
  }
  40% {
    transform: scale(1.2);
  }
  100% {
    transform: scale(1);
  }
}

.pulse.active {
  animation-name: bounce;
  animation-direction: normal;
  animation-duration: 1.9s;
}
.title {
  margin-bottom: 16px;
}
.subtitle {
  margin-bottom: 32px;
  max-width: 380px;
  text-align: center;
}
</style>
