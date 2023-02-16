<template>
  <PrimaryModal :visible="isVisible" hideTitle>
    <template #content>
      <div class="header">
        <FeatherIcon class="header-icon" :icon="Icons.CheckCircle" aria-hidden="true" focusable="false" />
        <span class="successName">{{ successName }}</span> {{ SuccessModalOptions.successMsg }}
      </div>

      <div class="column">
        <div class="title">
          {{ SuccessModalOptions.title }}
        </div>

        <FeatherButton text @click="closeModal">
          <template v-slot:icon>
            <FeatherIcon :icon="Icons.Discovery" aria-hidden="true" focusable="false" />
            {{ SuccessModalOptions.addDiscovery }}
          </template>
        </FeatherButton>

        <FeatherButton text @click="router.push('Inventory')">
          <template v-slot:icon>
            <FeatherIcon :icon="Icons.Nodes" aria-hidden="true" focusable="false" />
            {{ SuccessModalOptions.viewNodes }}
          </template>
        </FeatherButton>

        <FeatherButton text @click="router.push('Synthetic Transactions')">
          <template v-slot:icon>
            <FeatherIcon :icon="Icons.Synthetic" aria-hidden="true" focusable="false" />
            {{ SuccessModalOptions.addTransaction }}
          </template>
        </FeatherButton>

        <FeatherButton text @click="router.push('Monitoring Policies')">
          <template v-slot:icon>
            <FeatherIcon :icon="Icons.Monitoring" aria-hidden="true" focusable="false" />
            {{ SuccessModalOptions.addMonitoring }}
          </template>
        </FeatherButton>
      </div>
    </template>
    <template #footer>
      <feather-checkbox v-model="state.preventModal">
        {{ SuccessModalOptions.checkboxText }}
      </feather-checkbox>
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

const Icons = markRaw({
  CheckCircle,
  Discovery,
  Nodes,
  Synthetic,
  Monitoring
})

const router = useRouter()
const { openModal, closeModal, isVisible } = useModal()

const successName = ref()

const openSuccessModal = (name: string) => {
  if (state.value.preventModal) return
  successName.value = name
  openModal()
}

const state = useStorage<{ preventModal: boolean } >('prevent-discovery-modal', {
  preventModal: false
})

defineExpose({ openSuccessModal })
</script>

<style scoped lang="scss">
@use "@featherds/styles/themes/variables";
@use "@featherds/styles/mixins/typography";

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

  .btn {
    width: 300px;
    svg {
      color: var(variables.$shade-1);
      margin-right: var(variables.$spacing-l);
    }
  }
}
</style>
