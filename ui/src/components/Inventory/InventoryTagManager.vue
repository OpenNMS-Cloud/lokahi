<template>
  <div class="tag-manager">
    <section class="select-tags">
      <div class="top">
        <div class="heading-total-selected">
          <h4>Select Tags:</h4>
          <div class="total-selected">
            <div
              class="total"
              data-test="total"
            >
              TOTAL: <span>{{ tags.length }}</span>
              <span class="pipe">|</span>
            </div>
            <div
              class="selected"
              data-test="selected"
            >
              SELECTED: <span>{{ tagsSelected.length }}</span>
            </div>
          </div>
        </div>
        <div class="search-add">
          <FeatherButton
            @click="toggleSelectAll"
            secondary
            class="select-all-btn"
            data-test="select-deselect-all"
            >{{ areAllTagsSelected ? 'Deselect all' : 'Select all' }}</FeatherButton
          >
          <BasicAutocomplete
            @items-selected="tagsSelectedListener"
            :get-items="tagQueries.getTagsSearch"
            :items="tagQueries.tagsSearched"
            :show-list="false"
            label="Search/Add tags (optional)"
            ref="tagsAutocompleteRef"
            class="tags-autocomplete"
            data-test="tags-autocomplete"
          />
        </div>
      </div>
      <FeatherChipList
        v-if="tags.length"
        :key="tagsSelected.toString()"
        condensed
        label="Tags"
        class="tag-chip-list"
        data-test="tag-chip-list"
      >
        <FeatherChip
          v-for="(tag, index) in tags"
          :key="index"
          @click="toggleTagsSelected(tag)"
          :class="{ selected: isTagSelected(tag.name as string) }"
          class="pointer"
        >
          {{ tag.name }}
        </FeatherChip>
      </FeatherChipList>
    </section>
    <section class="tag-nodes">
      <FeatherButton
        @click="setTagEditMode(true)"
        :disabled="!tagsSelected.length"
        primary
        data-test="save-btn"
      >
        Add tags to node
      </FeatherButton>
      <FeatherButton
        @click="resetState"
        :disabled="!tagsSelected.length"
        secondary
        data-test="cancel-btn"
      >
        Cancel
      </FeatherButton>
    </section>
  </div>
</template>

<script setup lang="ts">
import { Tag } from '@/types/graphql'
import { useInventoryStore } from '@/store/Views/inventoryStore'
import { useTagQueries } from '@/store/Queries/tagQueries'
import { useTagStore } from '@/store/Components/tagStore'

const inventoryStore = useInventoryStore()
const tagQueries = useTagQueries()
const tagStore = useTagStore()

const tagsAutocompleteRef = ref()
const tags = computed(() => tagStore.tags)
const tagsSelected = computed(() => tagStore.tagsSelected)
const areAllTagsSelected = ref(false)

const tagsSelectedListener = (selectedTags: Record<string, string>[]) => {
  selectedTags.forEach((newTag) => tagStore.addNewTag(newTag))
}

const setTagEditMode = (isEdit: boolean) => {
  tagStore.setTagEditMode(isEdit)
}

const isTagSelected = (name: string): boolean =>
  tagsSelected.value.some(({ name: selectedTagName }) => selectedTagName === name)

const toggleSelectAll = () => {
  areAllTagsSelected.value = !areAllTagsSelected.value
  tagStore.selectAllTags(areAllTagsSelected.value)
}

const toggleTagsSelected = (tag: Tag) => {
  tagStore.toggleTagsSelected(tag)

  areAllTagsSelected.value = tagsSelected.value.length === tags.value.length
}

const resetState = () => {
  tagQueries.fetchTags()
  areAllTagsSelected.value = false
  inventoryStore.isTagManagerReset = true
  tagsAutocompleteRef.value.reset()
}

watchEffect(() => {
  if (inventoryStore.isTagManagerOpen) tagQueries.fetchTags()
})
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';
@use '@/styles/vars';
@use '@/styles/mediaQueriesMixins';

.tag-manager {
  display: flex;
  flex-direction: row;
  flex-flow: wrap;
  justify-content: space-between;
  border: 1px solid var(variables.$secondary-text-on-surface);
  border-radius: vars.$border-radius-m;
  padding: var(variables.$spacing-m);
  margin-bottom: var(variables.$spacing-xl);
  background-color: var(variables.$disabled-text-on-color);
  min-width: 480px;
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
    align-items: center;
    margin-bottom: var(variables.$spacing-m);
    h4 {
      padding-top: 3px;
      margin-right: var(variables.$spacing-m);
    }
    .heading-total-selected {
      width: 35%;
      .total-selected {
        display: flex;
        flex-direction: column;
        padding-top: 8px;
        margin-bottom: var(variables.$spacing-l);
        .total {
          > span {
            font-weight: bold;
          }
          .pipe {
            color: var(variables.$secondary-text-on-surface);
            margin: 0 var(variables.$spacing-s);
            display: none;
          }
        }
        .selected {
          > span {
            font-weight: bold;
          }
        }
      }

      @include mediaQueriesMixins.screen-lg {
        width: 50%;
      }
    }

    .search-add {
      display: flex;
      flex-direction: column;
      .select-all-btn {
        order: 2;
        margin-top: var(variables.$spacing-xxs);
      }
      .tags-autocomplete {
        display: flex;
        order: 1;
        :deep(.feather-input-wrapper) {
          min-width: 265px;
        }
      }

      @include mediaQueriesMixins.screen-md {
        flex-direction: row;
        .select-all-btn {
          margin-right: var(variables.$spacing-s);
          order: 1;
        }
        .tags-autocomplete {
          order: 2;
        }
      }
    }

    @include mediaQueriesMixins.screen-md {
      margin-bottom: 0;
      .heading-total-selected {
        .total-selected {
          flex-direction: row;
          .total {
            .pipe {
              display: inline;
            }
          }
        }
      }
    }
    @include mediaQueriesMixins.screen-lg {
      .heading-total-selected {
        display: flex;
        flex-direction: row;
      }
    }
  }
  .tag-chip-list {
    max-height: 100px;
    overflow-y: scroll;
  }

  @include mediaQueriesMixins.screen-lg {
    min-width: 0;
  }
}

.tag-nodes {
  display: flex;
  flex-direction: row;
  justify-content: end;
  width: 100%;
  min-width: 445px;
  margin-top: var(variables.$spacing-m);
  padding-top: var(variables.$spacing-m);
}
</style>
