<template>
  <div class="full-discovery-wrapper">
    <div class="discovery-wrapper">
      <HeadlinePage
        :text="discoveryText.Discovery.pageHeadline"
        class="page-headline"
      />
      <div class="add-btn">
        <FeatherButton
          data-test="addDiscoveryBtn"
          @click="discoveryStore.startNewDiscovery"
          primary
        >
          {{ discoveryText.Discovery.button.add }}
        </FeatherButton>
      </div>
    </div>

    <div class="container">
      <section class="my-discovery">
        <div class="my-discovery-inner">
          <DiscoveryListCard
            title="Active Discoveries"
            :list="discoveryStore.loadedDiscoveries.filter((d) => d.type && activeDiscoveryTypes.includes(d.type as DiscoveryType)).sort(sortDiscoveriesByName)"
            :selectDiscovery="discoveryStore.editDiscovery"
            :selectedId="discoveryStore.selectedDiscovery.id"
            :showInstructions="() => openInstructions(InstructionsType.Active)"
          />
          <DiscoveryListCard
            passive
            title="Passive Discoveries"
            :list="discoveryStore.loadedDiscoveries.filter((d) => d.type && [DiscoveryType.SyslogSNMPTraps].includes(d.type as DiscoveryType)).sort(sortDiscoveriesByName)"
            :toggleDiscovery="discoveryStore.toggleDiscovery"
            :selectDiscovery="discoveryStore.editDiscovery"
            :selectedId="discoveryStore.selectedDiscovery.id"
            :showInstructions="() => openInstructions(InstructionsType.Passive)"
          />
        </div>
      </section>
      <section
        class="discovery"
        v-if="!discoveryStore.discoveryFormActive && discoveryStore.discoveryTypePageActive"
      >
        <DiscoveryTypeSelector
          :updateSelectedDiscovery="discoveryStore.activateForm"
          title="New Discovery"
          :backButtonClick="discoveryStore.cancelUpdate"
        />
      </section>
      <section
        v-if="discoveryStore.discoveryFormActive && !discoveryStore.discoveryTypePageActive"
        class="discovery"
      >
        <div class="flex-title">
          <h2 class="title">{{ discoveryCopy.title }} | {{ selectedTypeOption?._text }}</h2>
          <div
            style="display: flex; justify-content: flex-end"
            v-if="discoveryStore.selectedDiscovery.id"
          >
            <FeatherButton
              v-if="discoveryStore.selectedDiscovery.id"
              @click="discoveryStore.openDeleteModal"
              :disabled="isOverallDisabled"
              secondary
              >Delete</FeatherButton
            >
            <ButtonWithSpinner
              text
              :disabled="isOverallDisabled"
              :click="discoveryStore.saveSelectedDiscovery"
              :isFetching="discoveryStore.loading"
              >Save</ButtonWithSpinner
            >
          </div>
        </div>

        <h3>Basic Information</h3>
        <p class="margin-bottom"></p>
        <FeatherInput
          label="Discovery Name"
          :modelValue="discoveryStore.selectedDiscovery.name"
          :error="discoveryStore.validationErrors.name"
          :disabled="isOverallDisabled"
          @update:model-value="(name: any) => discoveryStore.setSelectedDiscoveryValue('name', name)"
        />

        <div class="auto-with-chips">
          <AtomicAutocomplete
            inputLabel="Choose Location"
            :inputValue="discoveryStore.locationSearch"
            :itemClicked="discoveryStore.locationSelected"
            :loading="discoveryStore.loading"
            :outsideClicked="discoveryStore.clearLocationAuto"
            :resultsVisible="!!discoveryStore.foundLocations.length"
            :results="discoveryStore.foundLocations"
            :textChanged="discoveryStore.searchForLocation"
            :wrapperClicked="() => discoveryStore.searchForLocation('')"
            :errMsg="discoveryStore.validationErrors.locations || discoveryStore.validationErrors.locationId"
            :disabled="isOverallDisabled"
            :allowNew="false"
          />

          <FeatherChipList label="Locations">
            <FeatherChip
              v-for="b in discoveryStore.selectedDiscovery.locations"
              :key="b.id"
            >
              <template
                v-if="!isOverallDisabled"
                v-slot:icon
              >
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

        <FeatherSelect
          v-if="typeVisible"
          label="Type"
          :options="typeOptions"
          :modelValue="selectedTypeOption"
          @update:modelValue="(b: any) => discoveryStore.setSelectedDiscoveryValue('type', b?.value)"
        />
        <h3>Connection Information</h3>
        <p
          class="margin-bottom"
          v-if="isICMPOrPassive"
        >
          Set connection information like IP address ranges, port, and community strings.
        </p>
        <p
          class="margin-bottom"
          v-if="!isICMPOrPassive"
        >
          Set connection information like Azure client and subscription IDs.
        </p>
        <FeatherTabContainer
          :modelValue="selectedTab"
          @update:modelValue="changeSnmpType"
          v-if="discoveryStore.snmpV3Enabled"
        >
          <template v-slot:tabs>
            <FeatherTab>SNMP V1 or V1</FeatherTab>
            <FeatherTab>SNMP V3</FeatherTab>
          </template>
        </FeatherTabContainer>
        <DiscoveryMetaInformation
          :discovery="discoveryStore.selectedDiscovery"
          :discoveryErrors="discoveryStore.validationErrors"
          :updateDiscoveryValue="discoveryStore.setMetaSelectedDiscoveryValue"
        />

        <h3>Tag Discovered Nodes (optional)</h3>
        <p class="margin-bottom">
          A tag is an optional, arbitrary label that you can associate with a discovered node or device and its<br />
          components for easy and flexible filtering.
        </p>
        <div class="auto-with-chips">
          <AtomicAutocomplete
            inputLabel="Tags"
            :inputValue="discoveryStore.tagSearch"
            :itemClicked="discoveryStore.tagSelected"
            :loading="discoveryStore.loading"
            :resultsVisible="!!discoveryStore.foundTags.length || !!discoveryStore.tagSearch"
            :outsideClicked="discoveryStore.clearTagAuto"
            :results="discoveryStore.foundTags"
            :textChanged="discoveryStore.searchForTags"
            :wrapperClicked="() => discoveryStore.searchForTags('')"
            :errMsg="discoveryStore.tagError"
            :disabled="isOverallDisabled"
          />
          <FeatherChipList label="Tags">
            <FeatherChip
              v-for="b in discoveryStore.selectedDiscovery.tags"
              :key="b.id"
            >
              <template
                v-slot:icon
                v-if="!isOverallDisabled"
              >
                <FeatherIcon
                  @click="() => discoveryStore.removeTag(b)"
                  class="icon"
                  :icon="CancelIcon"
                />
              </template>
              {{ b?.name }}
            </FeatherChip>
          </FeatherChipList>
        </div>
        <div
          style="display: flex; justify-content: flex-end; width: 100%"
          v-if="!discoveryStore.selectedDiscovery.id"
        >
          <FeatherButton
            v-if="discoveryStore.selectedDiscovery.id"
            @click="discoveryStore.openDeleteModal"
            :disabled="isOverallDisabled"
            secondary
            >Delete Discovery</FeatherButton
          >
          <div style="margin-left: auto">
            <FeatherButton
              text
              @click="discoveryStore.backToTypePage"
              >Back</FeatherButton
            >

            <ButtonWithSpinner
              primary
              :disabled="isOverallDisabled"
              :click="discoveryStore.saveSelectedDiscovery"
              :isFetching="discoveryStore.loading"
              >{{ discoveryCopy.button }}</ButtonWithSpinner
            >
          </div>
        </div>
      </section>
    </div>
    <DiscoveryDeleteModal />
    <DiscoveryNewModal />
    <DiscoveryInstructions
      :instructionsType="helpType"
      :isOpen="discoveryStore.helpActive"
      @drawerClosed="() => discoveryStore.disableHelp()"
    />
  </div>
</template>

<script lang="ts" setup>
import { DiscoveryType, InstructionsType } from '@/components/Discovery/discovery.constants'
import discoveryText from '@/components/Discovery/discovery.text'
import { useDiscoveryStore } from '@/store/Views/discoveryStore'
import CancelIcon from '@featherds/icon/navigation/Cancel'
import { FeatherTabContainer } from '@featherds/tabs'
import { sortDiscoveriesByName } from '@/dtos/discovery.dto'
const selectedTab = ref()
const changeSnmpType = (type: any) => {
  if (type === 0) {
    discoveryStore.setSelectedDiscoveryValue('type', DiscoveryType.ICMP)
  } else if (type === 1) {
    discoveryStore.setSelectedDiscoveryValue('type', DiscoveryType.ICMPV3NoAuth)
  }
}
const discoveryStore = useDiscoveryStore()
const isOverallDisabled = computed(
  () =>
    !!(
      (discoveryStore.selectedDiscovery.type === DiscoveryType.Azure && discoveryStore.selectedDiscovery.id) ||
      discoveryStore.loading
    )
)
const discoveryCopy = computed(() => {
  const copy = { title: '', button: 'Save Discovery' }
  let title = discoveryStore.selectedDiscovery.id ? 'Edit' : 'New'

  if (discoveryStore.selectedDiscovery.type === DiscoveryType.Azure) {
    title += ' Azure'
    if (discoveryStore.selectedDiscovery.id) {
      title = 'View Azure'
    }
  } else if (discoveryStore.selectedDiscovery.type === DiscoveryType.SyslogSNMPTraps) {
    title += ' Passive'
  } else if (discoveryStore.selectedDiscovery.type === DiscoveryType.ICMP) {
    title += ' Active'
  }
  copy.title = title + ' Discovery'
  return copy
})
const isICMPOrPassive = computed(
  () =>
    discoveryStore.selectedDiscovery.type === DiscoveryType.ICMP ||
    discoveryStore.selectedDiscovery.type === DiscoveryType.SyslogSNMPTraps
)
const typeVisible = false
onMounted(() => {
  discoveryStore.init()
})
onUnmounted(() => {
  discoveryStore.$reset()
})
const typeOptions = ref([
  { value: DiscoveryType.ICMP, _text: 'ICMP/SNMP' },
  { value: DiscoveryType.Azure, _text: 'Azure' },
  { value: DiscoveryType.SyslogSNMPTraps, _text: 'Passive Syslog Traps' }
])
if (discoveryStore.snmpV3Enabled) {
  typeOptions.value = typeOptions.value.concat([
    { value: DiscoveryType.ICMPV3NoAuth, _text: 'ICMP V3 No Auth' },
    { value: DiscoveryType.ICMPV3Auth, _text: 'ICMP V3 Auth' },
    { value: DiscoveryType.ICMPV3AuthPrivacy, _text: 'ICMP V3 Auth + Privacy' }
  ])
}
const selectedTypeOption = computed(() => {
  return typeOptions.value.find((d) => d.value === discoveryStore.selectedDiscovery.type)
})

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
<style lang="scss">
.app-layout {
  overflow-y: scroll;
}
</style>
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
      max-width: 266px;
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
    max-width: 266px;

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
  border-radius: vars.$border-radius-surface;
  padding: var(variables.$spacing-m);
  background-color: var(variables.$surface);
  max-width: 900px;
  margin-bottom: 20px;
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
    margin-bottom: 20px;
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

  .title {
    width: 100%;
  }
  h2 {
    margin-bottom: 0;
  }
}
.margin-bottom {
  margin-bottom: 18px;
}
.full-discovery-wrapper {
  max-width: 1222px;
  margin: 0 auto;
}
</style>
