<template>
  <CollapsingWrapper :open="visible">
    <div class="tag-manager">
      <div class="instructions">
        <h4>To add tags to a node:</h4>
        <ol>
          <li>Using the search, find an existing tag or create a new one.</li>
          <li>After selecting your tag(s), click the checkbox field on the node(s) to which you want to add the tag(s).
          </li>
          <li>Click <strong>Save Tags to Node</strong>.</li>
        </ol>
      </div>
      <section class="select-tags">
        <div class="top">
          <div class="search-add">
            <AtomicAutocomplete class="tag-manager-complete" data-test="search-by-tags-inner" inputLabel="Search Tags"
              :loading="tagQueries.tagsSearchIsFetching" :outsideClicked="closeAutocomplete" :itemClicked="itemClicked"
              :resultsVisible="isAutoCompleteOpen" :focusLost="onFocusLost" :wrapperClicked="wrapperClicked"
              :results="tagQueries.tagsSearched.map((d) => d.name)" :inputValue="inputValue" :textChanged="textChanged">
            </AtomicAutocomplete>
            <FeatherTooltip :title="tagManagerTip" v-slot="{ attrs, on }">
              <FeatherButton v-bind="attrs" v-on="tagManagerTip ? on : null"
                @click="() => tagStore.saveTagsToSelectedNodes(state)" :disabled="!inventoryStore.nodesSelected.length"
                primary data-test="save-tags-button">
                {{ `Save tags to node${inventoryStore.nodesSelected.length > 1 ? 's' : ''}` }}
              </FeatherButton>
            </FeatherTooltip>
          </div>
        </div>
        <FeatherChipList v-if="tagStore.tagsSelected.length" condensed label="Tags" class="tag-chip-list"
          data-test="tag-chip-list">
          <FeatherChip v-for="(tag, index) in tagStore.tagsSelected" :key="index" class="pointer">
            <template v-slot:icon>
              <FeatherIcon @click="tagStore.deactivateTag(index)" :icon="CancelIcon" />
            </template>
            {{ tag.name }}
          </FeatherChip>
        </FeatherChipList>
      </section>
      <section class="tag-nodes">
        <div class="ctrls">

        </div>
      </section>
    </div>
  </CollapsingWrapper>
</template>

<script setup lang="ts">
import { useInventoryStore } from '@/store/Views/inventoryStore'
import { useTagQueries } from '@/store/Queries/tagQueries'
import { useTagStore } from '@/store/Components/tagStore'
import CancelIcon from '@featherds/icon/navigation/Cancel'
import { FeatherTooltip } from '@featherds/tooltip';
import AtomicAutocomplete from '../Common/AtomicAutocomplete.vue';

const autoCompleteOpen = ref(false);
const inputValue = ref('');
const inventoryStore = useInventoryStore()
const tagQueries = useTagQueries()
const tagStore = useTagStore()

const isAutoCompleteOpen = computed(() => {
  let open = false;
  if (autoCompleteOpen.value) {
    if (inputValue.value) {
      open = true;
    }
    if (tagQueries.tagsSearched.length > 0) {
      open = true;
    }
  }
  return open;
})

defineProps({
  visible: { type: Boolean, default: false },
  state: { type: String, default: '' }
})

const closeAutocomplete = () => {
  autoCompleteOpen.value = false;
  document.removeEventListener('click', closeChecker);
}

const closeChecker = (e: MouseEvent) => {
  if (!(e?.target as HTMLInputElement).closest('.atomic-input-wrapper')) {
    closeAutocomplete();
  }
}
const wrapperClicked = () => {
  autoCompleteOpen.value = true;
  tagQueries.getTagsSearch(inputValue.value);
  document.addEventListener('click', closeChecker);
}

const onFocusLost = () => {
  closeAutocomplete();
}

const tagManagerTip = computed(() => {
  let val = 'Find or create a tag to get started'
  if (tagStore.tagsSelected.length > 0) {
    val = 'Select an inventory item below in which to attach these tags.'
  }
  if (inventoryStore.nodesSelected.length > 0) {
    val = ''
  }
  return val
})

const itemClicked = (item: unknown, index: number) => {
  if (item) {
    tagStore.addNewTag({ name: item as string, id: index.toString() });
  }
}

const textChanged = (newVal: string) => {
  if (newVal) {
    autoCompleteOpen.value = true;
  }
  inputValue.value = newVal;
  tagQueries.getTagsSearch(newVal)
}
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
  border-radius: vars.$border-radius-s;
  padding: var(variables.$spacing-m);
  margin-bottom: var(variables.$spacing-s);
  background-color: var(variables.$disabled-text-on-color);
  min-width: 480px;
}

.select-tags {
  display: flex;
  flex-direction: column;
  min-width: 445px;

  .top {
    display: flex;
    justify-content: flex-end;
    align-items: flex-start;
    flex-direction: flex-end;
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
          >span {
            font-weight: bold;
          }

          .pipe {
            color: var(variables.$secondary-text-on-surface);
            margin: 0 var(variables.$spacing-s);
            display: none;
          }
        }

        .selected {
          >span {
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
      flex-wrap: wrap;
      align-items: center;

      .tags-autocomplete {
        margin-right: 12px;

        :deep(.feather-input-wrapper) {
          min-width: 265px;
        }

        :deep(.feather-input-sub-text) {
          display: none;
        }
      }

      .wrapper {
        @media (max-width:702px) {
          margin-bottom: var(variables.$spacing-m);
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
    display: flex;
    justify-content: flex-end;
    flex-wrap: wrap;
  }

  @include mediaQueriesMixins.screen-lg {
    min-width: 0;
  }

  .tag-manager-complete {
    margin-right: 12px;
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

.instructions {
  margin-bottom: var(variables.$spacing-m);
}
</style>
