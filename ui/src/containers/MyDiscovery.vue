<template>
  <div class="container">
    <HeadlinePage text="My Discovery" />
    <div class="row">
      <div class="discovery-cards">
        <div class="card">
          <div class="title">My Active Discoveries</div>
          <div
            class="list"
            v-if="nodes.length"
          >
            <div
              class="card-item"
              v-for="node in nodes"
              :key="node?.id"
              :class="{ 'dark-card': isDark, selected: itemSelected?.id === node?.id }"
              @click="itemSelected = node"
            >
              <FeatherIcon
                class="icon"
                :icon="Network"
              />{{ node.label }}
            </div>
          </div>
        </div>
        <div class="card">
          <div class="title">My Passive Discoveries</div>
          <div class="list"></div>
        </div>
      </div>
      <div class="details">
        <div v-if="itemSelected">
          <DiscoveryInfo :info="itemSelected" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useInventoryQueries } from '@/store/Queries/inventoryQueries'
import useTheme from '@/composables/useTheme'
import Network from '@featherds/icon/hardware/Network'
import { NodeContent } from '@/types/inventory'
const { isDark } = useTheme()
const inventoryQueries = useInventoryQueries()

const nodes = computed((): NodeContent[] => inventoryQueries.nodes)
const itemSelected = ref()

watchEffect(() => {
  if (nodes) itemSelected.value = nodes.value[0]
})
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';
@use '@featherds/styles/themes/open-dark';

.container {
  display: flex;
  margin: 0 1rem;
  flex-direction: column;

  .row {
    display: flex;
  }

  .discovery-cards {
    display: flex;
    flex-direction: column;
    min-width: 40%;
    > div:first-child {
      margin-bottom: var(variables.$spacing-m);
    }
    > .card {
      > .title {
        height: 60px;
        background-color: var(variables.$shade-4);
        line-height: 60px !important;
        padding-left: 20px;
        @include typography.headline4;
      }

      > .list {
        display: flex;
        min-height: 240px;
        border: 1px solid var(variables.$shade-4);
        background-color: var(variables.$surface);
        padding-top: var(variables.$spacing-l);
        padding-left: var(variables.$spacing-l);
        align-content: baseline;
        flex-wrap: wrap;

        .card-item {
          display: flex;
          padding: var(variables.$spacing-xs);
          border: 1px solid #d8e9f4;
          margin-right: var(variables.$spacing-m);
          border-radius: 10px;
          margin-bottom: var(variables.$spacing-m);
          background-color: #f2f8fc;
          width: 34%;
          height: fit-content;
          align-items: center;
          cursor: pointer;

          &.dark-card {
            background-color: #3f596b;
            border: none;
            &.selected {
              border-color: #beccd7;
            }
          }

          &.selected {
            border: #688eaa 2px solid;
            @include typography.subtitle1;
          }

          .icon {
            border: 1px solid #d8e9f4;
            border-radius: 20px;
            padding: 5px;
            width: 30px;
            height: 30px;
            color: var(variables.$shade-1);
            background-color: var(variables.$surface);
            margin-right: var(variables.$spacing-s);
          }
        }
      }
    }
  }

  .details {
    display: flex;
    border: 1px solid var(variables.$shade-4);
    background-color: var(variables.$surface);
    padding: var(variables.$spacing-l);
    margin-left: var(variables.$spacing-xl);
    flex-grow: 1;
  }
}
</style>
