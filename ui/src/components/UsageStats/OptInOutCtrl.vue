<template>
  <FeatherIcon
    data-test="settings-btn"
    :icon="Settings"
    class="pointer ctrl-icon"
    @click="openModal()"
  />

  <PrimaryModal title="Help improve your experience" :visible="isVisible">
    <template v-slot:content>
      <p>
        Please opt-in to send anonymous usage statistics. This will help <br />
        us improve your software. You can change this setting at any time.
      </p>

      <FeatherExpansionPanel
        class="expansion"
        v-if="!state.showModalOnLoad" 
        title="Show me what is being sent.">
        <pre>
          <code>
            <!-- {{ usageStatsQueries.usageStatsReport }} -->
          </code>
        </pre>
      </FeatherExpansionPanel>
    </template>

    <template v-slot:footer>
      <FeatherButton 
        data-test="opt-out-btn" 
        secondary 
        @click="opt()">
          Opt-out
      </FeatherButton>
      
      <FeatherButton 
        data-test="opt-in-btn" 
        primary
        @click="opt(true)">
          Opt-in
      </FeatherButton>
    </template>
  </PrimaryModal>
</template>

<script setup lang="ts">
import Settings from '@featherds/icon/action/Settings'
import useModal from '@/composables/useModal'
// import { useUsageStatsMutations } from '@/store/Mutations/usageStatsMutations'
// import { useUsageStatsQueries } from '@/store/Queries/usageStatsQueries'

const { openModal, closeModal, isVisible } = useModal()
// const usageStatsMutations = useUsageStatsMutations()
// const usageStatsQueries = useUsageStatsQueries()

const state = useStorage<{ showModalOnLoad: boolean } >('first-load', {
  showModalOnLoad: true
})

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const opt = (choice = false) => {
  state.value.showModalOnLoad = false

  // usageStatsMutations.toggleUsageStats({ 
  //   toggleDataChoices: {
  //     toggle: choice
  //   }
  // })

  closeModal()
}

onMounted(() => {
  if (state.value.showModalOnLoad) {
    openModal()
  }
})
</script>

<style scoped lang="scss">
.ctrl-icon {
  font-size: 24px;
  margin-top: 2px;
  margin-right: 15px;
}
.expansion {
  margin-top: 20px;
  pre, code {
    white-space: pre-line;
  }
}
</style>
