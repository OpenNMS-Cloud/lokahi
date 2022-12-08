<template>
  <div class="tag-manager-box" v-if="store.isTagsOpen">
    <div class="left">
      <div class="top">
        <div class="subtitle">Selected Tags: </div>

        <div class="total-selected">
          <div class="subtitle">Total: {{ tags.length }}</div>
          <div class="subtitle pipe">|</div>
          <div class="subtitle">Selected: {{ selectedTags.length }}</div>
        </div>

        <div class="search-add">
          <!-- Add tag -->
          <div>
            <FeatherDropdown ref="newTagDropdown">
              <template v-slot:trigger="{ attrs, on }">
                <FeatherButton link href="#" icon="Add Tag" v-bind="attrs" v-on="on">
                  <FeatherIcon :icon="addIcon" />
                </FeatherButton>
              </template>
              <FeatherInput v-model="newTag" label="New tag" class="new-tag-input" />
              <FeatherButton primary class="new-tag-btn" @click="addTag">
                Add Tag
              </FeatherButton>
            </FeatherDropdown>
          </div>

          <!-- Search tags input -->
          <FeatherInput
            class="search"
            v-model="searchValue"
            label="Search Tags">
            <template v-slot:pre>
              <FeatherIcon :icon="searchIcon" />
            </template>
          </FeatherInput>
        </div>
      </div>

      <div>
        <FeatherChipList condensed label="Tags" :key="selectedTags.toString()">
          <FeatherChip 
            v-for="tag of tags" 
            :key="tag" 
            class="pointer"
            :class="{ 'selected' : selectedTags.includes(tag) }"
            @click="selectTag(tag)"
          >
            {{ tag }}
          </FeatherChip>
        </FeatherChipList>
      </div>
    </div>

    <div class="right">
      <div class="vl"></div>
      <FeatherRadioGroup vertical :label="'Tag Nodes:'" v-model="tagNodes">
        <FeatherRadio :value="1">All</FeatherRadio>
        <FeatherRadio :value="2">Individual</FeatherRadio>
        <FeatherRadio :value="3">Clear</FeatherRadio>
      </FeatherRadioGroup>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useInventoryStore } from '@/store/Views/inventoryStore'
import Search from '@featherds/icon/action/Search'
import AddIcon from '@featherds/icon/action/AddCircleAlt'

const store = useInventoryStore()

const searchIcon = markRaw(Search)
const addIcon = markRaw(AddIcon)

const newTag = ref()
const newTagDropdown = ref()
const tagNodes = ref()
const searchValue = ref()
const selectedTags = ref<string[]>([])

const tags = computed(() => ['tag1', 'tag2'])

const selectTag = (tag: string) => {
  if (selectedTags.value.includes(tag)) {
    selectedTags.value = selectedTags.value.filter(t => t !== tag)
  } else {
    selectedTags.value.push(tag)
  }
}

const addTag = () => {
  // send newtag.value
  newTag.value = '' // clear input
  newTagDropdown.value.handleClose() // clost dropdown
}
</script>

<style scoped lang="scss">
@use "@featherds/styles/themes/variables";
@use "@featherds/styles/mixins/typography";
.subtitle {
  @include typography.subtitle2();
}
.tag-manager-box {
  display: flex;
  height: 175px;
  background: var(variables.$shade-4);
  margin: 10px 20px 10px 20px;
  border-radius: 3px;
  padding: var(variables.$spacing-m);

  .left {
    width: 80%;
    display: flex;
    flex-direction: column;
    padding-right: var(variables.$spacing-m);

    .top {
      display: flex;
      justify-content: space-between;

      .total-selected {
        display: flex;
        gap: var(variables.$spacing-m);

        .pipe {
          color: var(variables.$shade-4);
        }
      }

      .search-add {
        display: flex;
        gap: 5px;
        .search {
          width: 200px;
        }
        .new-tag-input {
          margin: 0px 10px 0px 10px;
          width: 175px;
        }
        .new-tag-btn {
          margin-left: var(variables.$spacing-s);
        }
      }
    }
  }
  .right {
    display: flex;

    .vl {
      border-left: 1px solid var(variables.$shade-3);
      height: 135px;
      margin-top: 6px;
      margin-right: var(variables.$spacing-m);
    }
  }
}
</style>


<style lang="scss">
.tag-manager-box {
  .feather-radio-group-container.vertical .layout-container {
    margin-bottom: 0px !important;
  }
}
</style>