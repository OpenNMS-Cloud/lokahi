<template>
  <HeadlinePage
    :text="discoveryText.Discovery.pageHeadline"
    class="page-headline"
  />
  <div class="container">
    <section class="my-discovery">
      <div class="add-btn">
        <FeatherButton
          @click="handleNewDiscovery"
          primary
        >
          {{ discoveryText.Discovery.button.add }}
          <template #icon>
            <Icon :icon="addIcon" />
          </template>
        </FeatherButton>
      </div>

      <div class="my-discovery-inner">
        <FeatherAutocomplete
          class="search"
          v-model="discoverySearchValue"
          :loading="searchLoading"
          :results="discoveriesResults"
          @search="search"
          label="Search Discovery"
          type="single"
          @update:model-value="showDiscovery"
        />
        <DiscoveryListCard
          title="My Active Discoveries"
          :list="discoveryQueries.activeDiscoveries"
          @select-discovery="showDiscovery"
          :selectedId="discoverySelectedType !== DiscoveryType.SyslogSNMPTraps ? selectedDiscovery?.id : null"
          @show-instructions="isInstructionVisible = true"
        />
        <DiscoveryListCard
          passive
          title="My Passive Discoveries"
          :list="discoveryQueries.passiveDiscoveries"
          @toggle-discovery="toggleDiscovery"
          @select-discovery="showDiscovery"
          :selectedId="discoverySelectedType === DiscoveryType.SyslogSNMPTraps ? selectedDiscovery?.id : null"
          @show-instructions="isInstructionVisible = true"
        />
      </div>
    </section>

    <!-- add/edit a discovery  -->
    <section
      v-if="isDiscoveryEditingShown"
      class="discovery"
    >
      <div
        v-if="showNewDiscovery"
        class="type-selector"
      >
        <div class="headline">{{ discoveryText.Discovery.headline1 }}</div>
        <DiscoveryTypeSelector
          ref="discoveryTypeSelector"
          @discovery-option-selected="(type: DiscoveryType) => (discoverySelectedType = type)"
        />
      </div>
      <div>
        <div v-if="discoverySelectedType === DiscoveryType.ICMP">
          <DiscoverySnmpForm
            :successCallback="
              (name) => {
                successModal.openSuccessModal(name)
                handleClose()
              }
            "
            :cancel="handleClose"
            :discovery="(selectedDiscovery as IcmpActiveDiscovery)"
          />
        </div>
        <div v-else-if="discoverySelectedType === DiscoveryType.Azure">
          <DiscoveryAzureForm
            :successCallback="
              (name) => {
                successModal.openSuccessModal(name)
                handleClose()
              }
            "
            :cancel="handleClose"
            :discovery="(selectedDiscovery as AzureActiveDiscovery)"
          />
        </div>
        <DiscoverySyslogSNMPTrapsForm
          v-else-if="discoverySelectedType === DiscoveryType.SyslogSNMPTraps"
          :successCallback="
            (name) => {
              successModal.openSuccessModal(name)
              handleClose()
            }
          "
          :cancel="handleClose"
          :discovery="(selectedDiscovery as PassiveDiscovery)"
        />
        <div
          v-else
          class="get-started"
        >
          {{ discoveryText.Discovery.noneDiscoverySelectedMsg }}
        </div>
      </div>
    </section>
  </div>
  <DiscoverySuccessModal ref="successModal" />
  <DiscoveryInstructions
    :isOpen="isInstructionVisible"
    @drawer-closed="() => (isInstructionVisible = false)"
  />
</template>

<script lang="ts" setup>
import AddIcon from '@featherds/icon/action/Add'
import { IIcon } from '@/types'
import { DiscoveryInput } from '@/types/discovery'
import { DiscoveryType } from '@/components/Discovery/discovery.constants'
import discoveryText from '@/components/Discovery/discovery.text'
import { useDiscoveryQueries } from '@/store/Queries/discoveryQueries'
import { useDiscoveryMutations } from '@/store/Mutations/discoveryMutations'
import { IAutocompleteItemType } from '@featherds/autocomplete'
import { AzureActiveDiscovery, IcmpActiveDiscovery, PassiveDiscovery } from '@/types/graphql'
import DiscoveryTypeSelector from '@/components/Discovery/DiscoveryTypeSelector.vue'
import { cloneDeep } from 'lodash'
const discoveryQueries = useDiscoveryQueries()
const discoveryMutations = useDiscoveryMutations()

type TDiscoveryAutocomplete = DiscoveryInput & { _text: string }

const addIcon: IIcon = {
  image: markRaw(AddIcon)
}

const successModal = ref()
const isDiscoveryEditingShown = ref(false)
const showNewDiscovery = ref(false)
const selectedDiscovery = ref<PassiveDiscovery | AzureActiveDiscovery | IcmpActiveDiscovery | null>(null)
const discoverySelectedType = ref(DiscoveryType.None)
const discoveryTypeSelector = ref<InstanceType<typeof DiscoveryTypeSelector>>()
const discoveriesResults = ref<TDiscoveryAutocomplete[]>([])
const searchLoading = ref(false)
const discoverySearchValue = ref(undefined)
const isInstructionVisible = ref(false)

const handleNewDiscovery = () => {
  isDiscoveryEditingShown.value = true
  showNewDiscovery.value = true
  selectedDiscovery.value = null
  discoverySelectedType.value = DiscoveryType.None
  discoveryTypeSelector.value?.reset()
}

const search = (q: string) => {
  if (!q) return
  searchLoading.value = true
  const results = [...discoveryQueries.activeDiscoveries, ...discoveryQueries.passiveDiscoveries]
    .filter((x: any) => x.name?.toLowerCase().indexOf(q) > -1)
    .map((x: any) => ({
      _text: x.name,
      ...x
    }))
  discoveriesResults.value = results as TDiscoveryAutocomplete[]
  searchLoading.value = false
}

const showDiscovery = (selected: IAutocompleteItemType | IAutocompleteItemType[] | undefined) => {
  const discovery = selected as IAutocompleteItemType
  discoverySearchValue.value = undefined

  if (discovery) {
    isDiscoveryEditingShown.value = true
    showNewDiscovery.value = false
    selectedDiscovery.value = cloneDeep(discovery)
    //replace with type guard
    if (discovery.discoveryType === DiscoveryType.ICMP) {
      discoverySelectedType.value = DiscoveryType.ICMP
    } else if (discovery.discoveryType === DiscoveryType.Azure) {
      discoverySelectedType.value = DiscoveryType.Azure
    } else {
      discoverySelectedType.value = DiscoveryType.SyslogSNMPTraps
    }
  }
}

const toggleDiscovery = (id: string, toggle: boolean) => {
  discoveryMutations.togglePassiveDiscovery({
    toggle: { id, toggle }
  })
}

const handleClose = () => {
  isDiscoveryEditingShown.value = false
  discoverySelectedType.value = DiscoveryType.None
}
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@/styles/mediaQueriesMixins.scss';
@use '@/styles/vars.scss';
@use '@featherds/styles/mixins/typography';

.page-headline {
  margin-left: var(variables.$spacing-l);
  margin-right: var(variables.$spacing-l);
}

.container {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  margin-left: var(variables.$spacing-l);
  margin-right: var(variables.$spacing-l);
  @include mediaQueriesMixins.screen-md {
    flex-direction: row;
  }
}

.my-discovery {
  width: 100%;
  min-width: 400px;
  margin-bottom: var(variables.$spacing-m);
  border-bottom: 1px solid var(variables.$border-on-surface);
  .add-btn {
    width: 100%;
    margin-bottom: var(variables.$spacing-l);
  }
  > .my-discovery-inner {
    width: 100%;
    display: flex;
    flex-direction: column;
    margin-bottom: var(variables.$spacing-l);
    margin-top: var(variables.$spacing-l);
    border-top: 1px solid var(variables.$border-on-surface);
    padding-top: var(variables.$spacing-l);

    > * {
      margin-bottom: var(variables.$spacing-m);
      &:last-child {
        margin-bottom: 0;
      }
    }

    @include mediaQueriesMixins.screen-md {
      max-width: 350px;
      margin-bottom: 0;
    }

    .search {
      background-color: var(variables.$surface);
      margin-bottom: var(variables.$spacing-m);

      :deep(.feather-input-sub-text) {
        display: none !important;
      }
    }
  }

  @include mediaQueriesMixins.screen-md {
    width: 27%;
    border-bottom: none;
    min-width: auto;
    flex-direction: column;
    margin-bottom: 0;
    margin-right: var(variables.$spacing-m);
    > * {
      width: 100%;
      margin-bottom: var(variables.$spacing-m);
    }
  }
}

.discovery {
  width: 100%;
  min-width: 400px;
  border: 1px solid var(variables.$border-on-surface);
  border-radius: vars.$border-radius-s;
  padding: var(variables.$spacing-m);
  background-color: var(variables.$surface);

  .headline {
    @include typography.header();
  }

  .type-selector {
    margin-bottom: var(variables.$spacing-l);
  }

  @include mediaQueriesMixins.screen-md {
    padding: var(variables.$spacing-l);
    height: fit-content;
    flex-grow: 1;
    min-width: auto;
    margin-bottom: 0;
  }
}
.get-started {
  width: 100%;
  height: 200px;
  display: flex;
  justify-content: center;
  align-items: center;
}
</style>
