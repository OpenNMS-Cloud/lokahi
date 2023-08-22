<template>
  <PrimaryModal
    @close="closeModalAndEmit()"
    :visible="isVisible"
    hideTitle
  >
    <template #content>
      <div class="header">
        <FeatherIcon
          class="header-icon"
          :icon="Icons.CheckCircle"
          aria-hidden="true"
          focusable="false"
        />
        <span class="successName">{{ successName }}</span> {{ SuccessModalOptions.successMsg }}
      </div>

      <div class="column">
        <FeatherButton
          text
          @click="closeModalAndStartNew"
          data-test="addAnotherDiscoveryButton"
        >
          <template v-slot:icon>
            <FeatherIcon
              :icon="Icons.Discovery"
              aria-hidden="true"
              focusable="false"
            />
            {{ SuccessModalOptions.addDiscovery }}
          </template>
        </FeatherButton>

        <FeatherButton
          text
          @click="router.push('Inventory')"
          data-test="viewDetectedNodesButton"
        >
          <template v-slot:icon>
            <FeatherIcon
              :icon="Icons.Nodes"
              aria-hidden="true"
              focusable="false"
            />
            {{ SuccessModalOptions.viewNodes }}
          </template>
        </FeatherButton>
      </div>
    </template>
  </PrimaryModal>
</template>

<script setup lang="ts">
import useModal from '@/composables/useModal'
import { useRouter } from 'vue-router'
import CheckCircle from '@featherds/icon/action/CheckCircle'
import Discovery from '@featherds/icon/action/Search'
import Nodes from '@featherds/icon/hardware/Appliances'
import Synthetic from '@featherds/icon/action/Cycle'
import Monitoring from '@featherds/icon/hardware/MinionProfiles'
import { SuccessModalOptions } from './discovery.text'
import { PropType } from 'vue'

const Icons = markRaw({
  CheckCircle,
  Discovery,
  Nodes,
  Synthetic,
  Monitoring
})

const router = useRouter()

const props = defineProps({
  startNewDiscovery: { type: Function as PropType<() => void>, default: () => ({}) }
})
const emit = defineEmits(['close'])

const closeModalAndStartNew = () => {
  props.startNewDiscovery()
  emit('close')
  closeModal()
}
const { openModal, closeModal, isVisible } = useModal()

const successName = ref()

const closeModalAndEmit = () => {
  closeModal()
  emit('close')
}
const openSuccessModal = (name: string) => {
  successName.value = name
  openModal()
}

defineExpose({ openSuccessModal })
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';

.header {
  @include typography.body-large;
  height: 100px;
  border-radius: 5px;
  background: var(variables.$background);
  margin: 0px;
  text-align: center;

  .header-icon {
    color: var(variables.$shade-1);
    font-size: 23px;
    vertical-align: middle;
    margin-right: var(variables.$spacing-s);
  }
  .successName {
    font-weight: bold;
    line-height: 100px;
    vertical-align: middle;
    text-transform: uppercase;
  }
}

.title {
  @include typography.body-small;
  margin: var(variables.$spacing-l) 0;
  text-align: center;
}

.column {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: var(variables.$spacing-xl);
  .btn {
    width: 300px;
    svg {
      color: var(variables.$shade-1);
      margin-right: var(variables.$spacing-l);
    }
  }
}
</style>
