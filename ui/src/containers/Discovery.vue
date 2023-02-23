<template>
  <PageHeadline
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
          v-model="searchValue"
          :loading="searchLoading"
          :results="discoveriesResults"
          @search="search"
          label="Search Discovery"
          type="single"
        />
        <DiscoveryListCard
          title=" My Active Discoveries"
          :list="activeDiscoveries"
          @select-discovery="showDiscovery"
        />
        <DiscoveryListCard
          title=" My Passive Discoveries"
          :list="[]"
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
        <DiscoveryTypeSelector @discovery-option-selected="(type: DiscoveryType) => (discoverySelectedType = type)" />
      </div>
      <div>
        <div v-if="discoverySelectedType === DiscoveryType.ICMP">
          <DiscoverySnmpForm
            @close-form="handleCancel"
            :successCallback="(name) => successModal.openSuccessModal(name)"
            :cancel="handleCancel"
          />
        </div>
        <div v-else-if="discoverySelectedType === DiscoveryType.Azure">
          <DiscoveryAzureForm
            :successCallback="(name) => successModal.openSuccessModal(name)"
            :cancel="handleCancel"
          />
        </div>
        <DiscoverySyslogSNMPTrapsForm
          v-else-if="discoverySelectedType === DiscoveryType.SyslogSNMPTraps"
          :successCallback="(name) => successModal.openSuccessModal(name)"
          :cancel="handleCancel"
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
</template>

<script lang="ts" setup>
import PageHeadline from '@/components/Common/PageHeadline.vue'
import AddIcon from '@featherds/icon/action/Add'
import { IIcon } from '@/types'
import { DiscoveryInput } from '@/types/discovery'
import { DiscoveryType } from '@/components/Discovery/discovery.constants'
import discoveryText from '@/components/Discovery/discovery.text'
import { useDiscoveryStore } from '@/store/Views/discoveryStore'
import { useDiscoveryQueries } from '@/store/Queries/discoveryQueries'
import { DiscoveryConfig } from '@/types/graphql'

const discoveryQueries = useDiscoveryQueries()
onMounted(() => discoveryQueries.getDiscoveries())
const activeDiscoveries = computed(() => discoveryQueries.discoveries)

type TDiscoveryAutocomplete = DiscoveryInput & { _text: string }

const store = useDiscoveryStore()

const addIcon: IIcon = {
  image: markRaw(AddIcon)
}

const successModal = ref()
const isDiscoveryEditingShown = ref(false)
const showNewDiscovery = ref(false)
const selectedDiscovery = ref<DiscoveryInput | null>(null)
const discoverySelectedType = ref(DiscoveryType.None)

const handleNewDiscovery = () => {
  isDiscoveryEditingShown.value = true
  showNewDiscovery.value = true
  store.setSelectedDiscovery(null)
}

const discoveriesResults = ref<TDiscoveryAutocomplete[]>([])
const searchLoading = ref(false)
const searchValue = ref(undefined)

const search = (q: string) => {
  searchLoading.value = true
  const results = store.activeDiscoveries
    .filter((x) => x.name.toLowerCase().indexOf(q) > -1)
    .map((x) => ({
      _text: x.name,
      id: x.id,
      name: x.name
    }))
  discoveriesResults.value = results as TDiscoveryAutocomplete[]
  searchLoading.value = false
}

const showDiscovery = (discovery: DiscoveryConfig) => {
  isDiscoveryEditingShown.value = true
  showNewDiscovery.value = false
  //type hardocoded for now
  discoverySelectedType.value = DiscoveryType.ICMP
  store.setSelectedDiscovery(discovery)
}

const handleCancel = () => {
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
    border-bottom: 1px solid var(variables.$border-on-surface);
    > button {
      margin-bottom: var(variables.$spacing-l);
    }
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
      margin-bottom: 0;
    }

    .search {
      background-color: var(variables.$surface);
      margin-bottom: var(variables.$spacing-m);
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
