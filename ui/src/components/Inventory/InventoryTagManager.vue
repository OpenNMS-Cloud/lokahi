<template>
  <div
    class="tag-manager-box"
    v-if="isTaggingBoxOpen"
  >
    <section class="select-tags">
      <div class="top">
        <div class="heading-total-selected">
          <h4>Select Tags:</h4>
          <div class="total-selected">
            <div class="total">
              <!-- TOTAL: <span>{{ tags.length ? tags.length : '' }}</span> -->
              TOTAL: <span>{{ tags.length }}</span>
              <span class="pipe">|</span>
            </div>
            <div class="selected">
              <!-- SELECTED: <span>{{ selectedTags.length ? selectedTags.length : '' }}</span> -->
              SELECTED: <span>{{ selectedTags.length }}</span>
            </div>
          </div>
        </div>
        <div class="search-add">
          <!-- Add tag -->
          <!-- <InputButtonPopover
            :icon="inputIcon"
            :handler="addTag"
            label="Add Tag"
          /> -->
          <!-- Search tags input -->
          <!-- <FeatherInput
            :modelValue="searchValue"
            @update:model-value="tagsFiltering"
            clear="clear"
            label="Search Tags"
            class="search"
          /> -->
          <!-- <FeatherButton text>Deselect all</FeatherButton> -->
          <FeatherButton
            @click="selectAllToggle"
            secondary
            class="select-all-btn"
            >{{ isSelectingAll ? 'Deselect all' : 'Select all' }}</FeatherButton
          >
          <DiscoveryAutocomplete
            @items-selected="tagsSelectedListener"
            :get-items="tagQueries.getTagsSearch"
            :items="tagQueries.tagsSearched"
            label="Search/Add tags (optional)"
            ref="tagsAutocompleteRef"
            class="tags-autocomplete"
            data-test="tags-autocomplete"
          />
        </div>
      </div>
      <FeatherChipList
        v-if="tags.length"
        :key="selectedTags.toString()"
        condensed
        label="Tags"
        class="tag-chip-list"
      >
        <FeatherChip
          v-for="(tag, index) in tags"
          :key="index"
          @click="tagStore.toggleTag(tag.name)"
          :class="{ selected: isTagSelected(tag.name as string) }"
          class="pointer"
        >
          {{ tag.name }}
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
        v-model="tagStore.tagNodesSelected"
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
import { useTagQueries } from '@/store/Queries/tagQueries'
import { useTagStore } from '@/store/Components/tagStore'
import { useTagMutations } from '@/store/Mutations/tagMutations'
import { TagNodesType } from '@/types/tags'
// import Add from '@featherds/icon/action/Add'

const inventoryStore = useInventoryStore()
const tagQueries = useTagQueries()
const tagStore = useTagStore()
const tagMutations = useTagMutations()

const searchValue = ref()
const tags = computed(() => tagStore.tags)
const selectedTags = computed(() => tagStore.selectedTags)
const isSelectingAll = ref(false)

watchEffect(() => {
  // console.log('tagQueries.tags', tagQueries.tags)
  tags.value = tagQueries.tags
})
const isTaggingBoxOpen = computed(() => {
  inventoryStore.isTaggingBoxOpen ? tagStore.fetchTags() : tagStore.resetTags()

  return inventoryStore.isTaggingBoxOpen
})

const tagsAutocompleteRef = ref()
let tagsSelected: Record<string, string>[] = []
const tagsSelectedListener = (tags: Record<string, string>[]) => {
  tagsSelected = tags.map((tag) => {
    delete tag._text
    return tag
  })
}

const isTagSelected = (name: string): boolean => selectedTags.value.some((t) => t.name?.includes(name))

const selectAllToggle = () => {
  // select all
  /* if (isSelectingAll.value) {

  } else {
    // deselect all
  } */
  isSelectingAll.value = !isSelectingAll.value
  tagStore.selectAllTags(isSelectingAll.value)
}
const addTag = (val: string) => {
  tagMutations.editTag(val, true)
  // cannot be added if already exists
  // tag list should be updated
  // input popover should be auto-closed once added?
  // if adding fail, show snackabr
  // reset: on close
  // acts as list filter
  // when pop
}
const tagsFiltering = (val: string) => {
  console.log('val', val)
  console.log('tagQueries.tags', tagQueries.tags)
  if (!val?.length) {
    tags.value = tagQueries.tags
  } else {
    tags.value = tagQueries.tags.filter((tag) => tag.name?.includes(val))
  }
  console.log('tags.value', tags.value)
}

/* const inputIcon = {
  image: markRaw(Add),
  size: 2,
  cursorHover: 'pointer'
} */
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
    }

    .search-add {
      width: 50%;
      display: flex;
      flex-direction: row;
      flex-wrap: wrap;
      justify-content: flex-end;
      .select-all-btn {
        order: 2;
      }
      .tags-autocomplete {
        min-width: 265px;
        order: 1;
      }
    }
    @include mediaQueries.screen-md {
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
      .search-add {
        width: 70%;
        .select-all-btn {
          order: 1;
          margin-top: 2px;
          margin-right: var(variables.$spacing-m);
        }
        .tags-autocomplete {
          order: 2;
        }
      }
    }

    @include mediaQueries.screen-xl {
      .heading-total-selected {
        width: 50%;
        display: flex;
        flex-direction: row;
        .total-selected {
          .total {
            margin-left: var(variables.$spacing-m);
          }
        }
      }
    }
  }
  .tag-chip-list {
    max-height: 100px;
    overflow-y: scroll;
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
