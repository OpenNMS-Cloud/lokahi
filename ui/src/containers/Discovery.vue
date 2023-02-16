<template>
  <PageHeadline
    :text="discoveryText.Discovery.pageHeadline"
    class="page-headline"
  />
  <div class="container">
    <section class="my-discovery">
      <div class="add-btn">
        <FeatherButton
          @click="showDiscoveryEditing"
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
          :list="mocksActiveList"
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
      <div class="headline">{{ discoveryText.Discovery.headline1 }}</div>
      <div class="type-selector">
        <DiscoveryTypeSelector @discovery-option-selected="(type: DiscoveryType) => (discoverySelectedType = type)" />
      </div>
      <div v-if="discoverySelectedType === DiscoveryType.ICMP">ICMP/SNMP</div>
      <div v-else-if="discoverySelectedType === DiscoveryType.Azure">
        <DiscoveryAzureForm :successCallback="(name) => successModal.openSuccessModal(name)" />
      </div>
      <DiscoverySyslogSNMPTrapsForm v-else-if="discoverySelectedType === DiscoveryType.SyslogSNMPTraps" />
      <div
        v-else
        class="get-started"
      >
        {{ discoveryText.Discovery.noneDiscoverySelectedMsg }}
      </div>
    </section>
  </div>
  <DiscoverySuccessModal ref="successModal" />
</template>

<script lang="ts" setup>
import { IAutocompleteItemType } from '@featherds/autocomplete'
import PageHeadline from '@/components/Common/PageHeadline.vue'
import AddIcon from '@featherds/icon/action/Add'
import { IIcon } from '@/types'
import { IDiscovery } from '@/types/discovery'
import { DiscoveryType } from '@/components/Discovery/discovery.constants'
import discoveryText from '@/components/Discovery/discovery.text'

const addIcon: IIcon = {
  image: markRaw(AddIcon)
}

const successModal = ref()
const isDiscoveryEditingShown = ref(false)
const discoverySelectedType = ref(DiscoveryType.None)

const showDiscoveryEditing = () => {
  isDiscoveryEditingShown.value = true
}

const discoveriesResults = ref<(IDiscovery & IAutocompleteItemType)[]>([])
const searchLoading = ref(false)
const searchValue = ref(undefined)
const mocksActiveList = [
  { id: 1, name: 'MAD-001' },
  { id: 2, name: 'MAD-002' }
] as IDiscovery[]

const search = (q: string) => {
  searchLoading.value = true
  const results = mocksActiveList
    .filter((x) => x.name.toLowerCase().indexOf(q) > -1)
    .map((x) => ({
      _text: x.name,
      id: x.id,
      name: x.name
    }))
  discoveriesResults.value = results
  searchLoading.value = false
}

const showDiscovery = (id: number) => {
  console.log('show discovery', id)
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
  flex-direction: row;
  flex-flow: wrap;
  justify-content: space-between;
  margin-left: var(variables.$spacing-l);
  margin-right: var(variables.$spacing-l);

  @include mediaQueriesMixins.screen-md {
    > .my-discovery {
      width: 30%;
      min-width: auto;
    }
    > .discovery {
      width: 67%;
      min-width: auto;
    }
  }
}

.my-discovery {
  width: 100%;
  min-width: 400px;
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
    flex-direction: column;
    margin-bottom: 0;
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
    @include typography.headline4();
  }

  .type-selector {
    margin-bottom: var(variables.$spacing-xxl);
  }

  @include mediaQueriesMixins.screen-md {
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
