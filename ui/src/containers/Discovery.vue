<template>
  <div class="discovery-wrapper">
    <HeadlinePage
      :text="discoveryText.Discovery.pageHeadline"
      class="page-headline"
    />
    <div class="add-btn">
      <FeatherButton
        data-test="addDiscoveryBtn"
        @click="discoveryStore.startNewDiscovery"
        v-if="!discoveryStore.discoveryFormActive"
        primary
      >
        {{ discoveryText.Discovery.button.add }}
        <template #icon>
          <Icon :icon="addIcon" />
        </template>
      </FeatherButton>
    </div>
  </div>

  <div class="container">
    <section class="my-discovery">
      <div class="my-discovery-inner">
        <DiscoveryListCard
          title="My Active Discoveries"
          :list="discoveryStore.loadedDiscoveries.filter((d) => d.type && activeDiscoveryTypes.includes(d.type as DiscoveryType))"
          :selectDiscovery="discoveryStore.editDiscovery"
          :selectedId="discoveryStore.selectedDiscovery.id"
          :showInstructions="() => openInstructions(InstructionsType.Active)"
        />
        <DiscoveryListCard
          passive
          title="My Passive Discoveries"
          :list="discoveryStore.loadedDiscoveries.filter((d) => d.type && [DiscoveryType.SyslogSNMPTraps].includes(d.type as DiscoveryType))"
          :toggleDiscovery="discoveryStore.toggleDiscovery"
          :selectDiscovery="discoveryStore.editDiscovery"
          :selectedId="discoveryStore.selectedDiscovery.id"
          :showInstructions="() => openInstructions(InstructionsType.Passive)"
        />
      </div>
    </section>
    <section
      class="discovery"
      v-if="!discoveryStore.discoveryFormActive && discoveryStore.soloTypePageActive"
    >
      <DiscoveryTypeSelector
        :updateSelectedDiscovery="discoveryStore.activateForm"
        title="New Discovery"
        :backButtonClick="discoveryStore.cancelUpdate"
      />
    </section>
    <section
      v-if="discoveryStore.discoveryFormActive && !discoveryStore.soloTypePageActive"
      class="discovery"
    >
      <div class="flex-title">
        <FeatherBackButton
          @click="discoveryStore.backToTypePage"
          v-if="discoveryStore.discoveryFormActive"
          >Cancel</FeatherBackButton
        >
        <h2 class="title">{{ discoveryCopy.title }} | {{ selectedTypeOption?._text }}</h2>
      </div>

      <FeatherInput
        label="Discovery Name"
        :modelValue="discoveryStore.selectedDiscovery.name"
        @update:model-value="(name) => discoveryStore.setSelectedDiscoveryValue('name', name)"
      />

      <div class="auto-with-chips">
        <AtomicAutocomplete
          inputLabel="Locations"
          :inputValue="discoveryStore.locationSearch"
          :itemClicked="discoveryStore.locationSelected"
          :loading="discoveryStore.loading"
          :outsideClicked="discoveryStore.clearLocationAuto"
          :resultsVisible="!!discoveryStore.foundLocations.length"
          :results="discoveryStore.foundLocations"
          :textChanged="discoveryStore.searchForLocation"
          :wrapperClicked="() => discoveryStore.searchForLocation('')"
          :errMsg="discoveryStore.locationError"
          :disabled="discoveryStore.loading"
          :allowNew="false"
        />

        <FeatherChipList label="Locations">
          <FeatherChip
            v-for="b in discoveryStore.selectedDiscovery.locations"
            :key="b.id"
          >
            <template v-slot:icon>
              <FeatherIcon
                @click="() => discoveryStore.removeLocation(b)"
                class="icon"
                :icon="CancelIcon"
              />
            </template>
            {{ b.location }}
          </FeatherChip>
        </FeatherChipList>
      </div>
      <div class="auto-with-chips">
        <AtomicAutocomplete
          inputLabel="Tags"
          :inputValue="discoveryStore.tagSearch"
          :itemClicked="discoveryStore.tagSelected"
          :loading="discoveryStore.loading"
          :resultsVisible="!!discoveryStore.foundTags.length"
          :outsideClicked="discoveryStore.clearTagAuto"
          :results="discoveryStore.foundTags"
          :textChanged="discoveryStore.searchForTags"
          :wrapperClicked="() => discoveryStore.searchForTags('')"
          :errMsg="discoveryStore.tagError"
          :disabled="discoveryStore.loading"
          :allowNew="true"
        />
        <FeatherChipList label="Tags">
          <FeatherChip
            v-for="b in discoveryStore.selectedDiscovery.tags"
            :key="b.id"
          >
            <template v-slot:icon>
              <FeatherIcon
                @click="() => discoveryStore.removeTag(b)"
                class="icon"
                :icon="CancelIcon"
              />
            </template>
            {{ b.name }}
          </FeatherChip>
        </FeatherChipList>
      </div>
      <FeatherSelect
        v-if="typeVisible"
        label="Type"
        :options="typeOptions"
        :modelValue="selectedTypeOption"
        @update:modelValue="(b) => discoveryStore.setSelectedDiscoveryValue('type', b?.value)"
      />
      <FeatherTabContainer
        :modelValue="selectedTab"
        @update:modelValue="changeSnmpType"
      >
        <template v-slot:tabs>
          <FeatherTab>SNMP V1 or V1</FeatherTab>
          <FeatherTab>SNMP V3</FeatherTab>
        </template>
      </FeatherTabContainer>
      <DiscoveryMetaInformation
        :discovery="discoveryStore.selectedDiscovery"
        :updateDiscoveryValue="discoveryStore.setSelectedDiscoveryValue"
      />
      <div style="display: flex; justify-content: flex-end; width: 100%">
        <FeatherButton
          v-if="discoveryStore.selectedDiscovery.id"
          @click="discoveryStore.deleteDiscovery"
          >Delete Discovery</FeatherButton
        >
        <FeatherButton
          primary
          @click="discoveryStore.cancelUpdate"
          >{{ discoveryCopy.button }}</FeatherButton
        >
      </div>
    </section>
    <section
      v-if="!discoveryStore.soloTypePageActive && !discoveryStore.discoveryFormActive"
      class="discovery"
    >
      <p>Select a discovery on the left, or click Add Discovery</p>
    </section>
  </div>
  <DiscoverySuccessModal ref="successModal" />
  <DiscoveryInstructions
    :instructionsType="helpType"
    :isOpen="isHelpVisible"
    @drawerClosed="() => (isHelpVisible = false)"
  />
</template>

<script lang="ts" setup>
import AddIcon from '@featherds/icon/action/Add'
import { IIcon } from '@/types'
import { DiscoveryType, InstructionsType } from '@/components/Discovery/discovery.constants'
import discoveryText from '@/components/Discovery/discovery.text'
import { useDiscoveryStore } from '@/store/Views/discoveryStore'
import CancelIcon from '@featherds/icon/navigation/Cancel'
import { FeatherTabContainer } from '@featherds/tabs'
const selectedTab = ref()
const changeSnmpType = (type: any) => {
  if (type === 0) {
    discoveryStore.setSelectedDiscoveryValue('type', DiscoveryType.ICMP)
  } else if (type === 1) {
    discoveryStore.setSelectedDiscoveryValue('type', DiscoveryType.ICMPV3NoAuth)
  }
}
const discoveryStore = useDiscoveryStore()
const discoveryCopy = computed(() =>
  discoveryStore.selectedDiscovery.id
    ? { title: 'Edit Discovery', button: 'Update Discovery' }
    : { title: 'Add Discovery', button: 'Start New Discovery' }
)

const typeVisible = false
onMounted(() => {
  discoveryStore.init()
})
const typeOptions = [
  { value: DiscoveryType.ICMP, _text: 'ICMP' },
  { value: DiscoveryType.ICMPV3NoAuth, _text: 'ICMP V3 No Auth' },
  { value: DiscoveryType.ICMPV3Auth, _text: 'ICMP V3 Auth' },
  { value: DiscoveryType.ICMPV3AuthPrivacy, _text: 'ICMP V3 Auth + Privacy' },
  { value: DiscoveryType.Azure, _text: 'Azure' },
  { value: DiscoveryType.SyslogSNMPTraps, _text: 'Syslogs' }
]
const selectedTypeOption = computed(() => {
  return typeOptions.find((d) => d.value === discoveryStore.selectedDiscovery.type)
})
const addIcon: IIcon = {
  image: markRaw(AddIcon)
}

const isHelpVisible = ref(false)
const helpType = ref(InstructionsType.Active)

const openInstructions = (type: InstructionsType) => {
  isHelpVisible.value = true
  helpType.value = type
}

const activeDiscoveryTypes = [
  DiscoveryType.Azure,
  DiscoveryType.ICMP,
  DiscoveryType.ICMPV3Auth,
  DiscoveryType.ICMPV3AuthPrivacy,
  DiscoveryType.ICMPV3NoAuth
]
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@/styles/mediaQueriesMixins.scss';
@use '@/styles/vars.scss';
@use '@featherds/styles/mixins/typography';

.title {
  margin-bottom: 24px;
}
.discovery-wrapper {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  padding-right: 20px;
}
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

.icon {
  cursor: pointer;
}

.auto-with-chips {
  margin-bottom: 12px;
}
.flex-title {
  display: flex;
  align-items: center;
  margin-bottom: 32px;

  h2 {
    margin-bottom: 0;
    margin-left: 18px;
  }
}
</style>
