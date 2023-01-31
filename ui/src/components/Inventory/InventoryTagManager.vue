<template>
  <div
    v-if="isTaggingBoxOpen"
    class="tag-manager-box"
    data-test="tag-manager-box"
  >
    <section
      class="select-tags"
      data-test="select-tags"
    >
      <div class="top">
        <div class="heading-total-selected">
          <h4>Select Tags:</h4>
          <div class="total-selected">
            <div>
              TOTAL: <span>{{ tags.length }}</span
              ><span class="pipe">|</span>
            </div>
            <div>
              SELECTED: <span>{{ selectedTags.length }}</span>
            </div>
          </div>
        </div>
        <div class="search-add">
          <!-- Add tag -->
          <InputButtonPopover
            :handler="addTag"
            label="Add Tag"
          />
          <!-- Search tags input -->
          <FeatherInput
            :modelValue="searchValue"
            @update:model-value="tagsFiltering"
            clear="clear"
            label="Search Tags"
            class="search"
          />
        </div>
      </div>
      <FeatherChipList
        condensed
        label="Tags"
        :key="selectedTags.toString()"
      >
        <FeatherChip
          v-for="tag of tags"
          :key="tag"
          class="pointer"
          :class="{ selected: selectedTags.includes(tag) }"
          @click="taggingStore.toggleTag(tag)"
        >
          {{ tag }}
        </FeatherChip>
      </FeatherChipList>
    </section>
    <section
      class="tag-nodes"
      data-test="tag-nodes"
    >
      <h4>Tag Nodes:</h4>
      <FeatherRadioGroup
        label=""
        v-model="taggingStore.tagNodesSelected"
        class="select-tag-nodes"
      >
        <FeatherRadio
          :value="TagNodesType.All"
          :disabled="selectedTags.length === 0"
          >All</FeatherRadio
        >
        <FeatherRadio
          :value="TagNodesType.Individual"
          :disabled="selectedTags.length === 0"
          >Individual</FeatherRadio
        >
        <FeatherRadio :value="TagNodesType.Clear">Clear</FeatherRadio>
      </FeatherRadioGroup>
    </section>
  </div>
</template>

<script setup lang="ts">
import { useInventoryStore } from '@/store/Views/inventoryStore'
import { useTaggingQueries } from '@/store/Queries/taggingQueries'
import { useTaggingStore } from '@/store/Components/taggingStore'
import { useTaggingMutations } from '@/store/Mutations/taggingMutations'
import { TagNodesType } from '@/types/tags'

const inventoryStore = useInventoryStore()
const taggingQueries = useTaggingQueries()
const taggingStore = useTaggingStore()
const taggingMutations = useTaggingMutations()

const searchValue = ref()
const tags = ref()
const selectedTags = computed(() => taggingStore.selectedTags)

watchEffect(() => {
  tags.value = taggingQueries.tags
})

const isTaggingBoxOpen = computed(() => {
  if (!inventoryStore.isTaggingBoxOpen) {
    taggingQueries.resetTags()
  } else {
    taggingQueries.fetchTags()
  }

  return inventoryStore.isTaggingBoxOpen
})

const addTag = (val: string) => {
  taggingMutations.editTag(val, true)
}

const tagsFiltering = (val: string) => {
  if (!val?.length) {
    tags.value = taggingQueries.tags
  } else {
    tags.value = taggingQueries.tags.filter((tag: string) => tag.includes(val))
  }
}
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';
@use '@/styles/vars';
@use '@/styles/mediaQueries';

.tag-manager-box {
  display: flex;
  flex-direction: row;
  flex-flow: wrap;
  justify-content: space-between;
  border: 1px solid var(variables.$secondary-text-on-surface);
  border-radius: vars.$border-radius-m;
  padding: var(variables.$spacing-m);
  margin-bottom: var(variables.$spacing-xxl);
  background-color: var(variables.$disabled-text-on-color);
  min-width: 480px;
  h4 {
    padding-top: 3px;
  }
}

.select-tags {
  display: flex;
  flex-direction: column;
  width: 100%;
  min-width: 445px;
  .top {
    display: flex;
    flex-direction: row;
    justify-content: space-between;
    margin-bottom: var(variables.$spacing-m);
    .heading-total-selected {
      .total-selected {
        display: flex;
        flex-direction: row;
        padding-top: 8px;
        > * {
          > span {
            font-weight: bold;
            &.pipe {
              color: var(variables.$secondary-text-on-surface);
              margin: 0 var(variables.$spacing-s);
            }
          }
        }
      }

      @include mediaQueries.screen-md {
        display: flex;
        flex-direction: row;
        width: 50%;
        justify-content: space-between;
      }
    }
    .search-add {
      display: flex;
      flex-direction: row;
      :deep(.add-btn) {
        margin: 5px 10px 0 0;
      }
      :deep(.search) {
        width: 200px;
        input {
          margin-right: var(variables.$spacing-m);
        }
      }
    }

    @include mediaQueries.screen-md {
      margin-bottom: 0;
    }
  }
  .tag-chip-list {
    margin-top: 0;
  }

  @include mediaQueries.screen-lg {
    width: 75%;
    min-width: 0;
  }
  @include mediaQueries.screen-xxl {
    width: 80%;
  }
}

.tag-nodes {
  display: flex;
  flex-direction: row;
  width: 100%;
  min-width: 445px;
  margin-top: var(variables.$spacing-m);
  padding-top: var(variables.$spacing-m);
  border-top: 1px solid var(variables.$secondary-text-on-surface);

  :deep(.select-tag-nodes) {
    margin-left: var(variables.$spacing-m);
    > label {
      display: none;
    }
    .feather-input-sub-text {
      display: none;
    }
  }

  @include mediaQueries.screen-lg {
    width: 20%;
    min-width: 0;
    margin-top: 0;
    padding-top: 0;
    border-top: 0;
    border-left: 1px solid var(variables.$secondary-text-on-surface);
    padding-left: var(variables.$spacing-l);
    display: block;
  }

  @include mediaQueries.screen-xxl {
    width: 15%;
  }
}
</style>
