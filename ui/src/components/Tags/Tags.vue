<template>
  <div class="tags-container">

    <!-- Add tag -->
    <div class="add-tag-container">
      <!-- Add tag input -->
      <FeatherInput
        data-test="add-tag-input"
        class="add-tag-input"
        label="Tag"
        v-model="tag"
        @keyup.enter="addTag"
        v-focus
        :error="duplicateTagNameError"
      />

      <!-- Add tag btn -->
      <FeatherButton 
        primary
        data-test="add-tag-btn"
        class="add-tag-btn"
        :disabled="!tag || Boolean(duplicateTagNameError)"
        @click="addTag"
      > 
        <template v-slot:icon>
          <FeatherIcon 
            :icon="Add" 
            aria-hidden="true" 
            focusable="false" 
          />
          Add Tag
        </template>
      </FeatherButton>
    </div>

    <!-- Tag list -->
    <FeatherList>
      <FeatherListItem v-for="item of state.tags">
        {{ item }}

        <template v-slot:post>
          <FeatherButton 
            data-test="remove-tag-btn" 
            icon="Remove tag" 
            @click="removeTag(item)">
            <FeatherIcon :icon="Remove"></FeatherIcon>
          </FeatherButton>
        </template>

      </FeatherListItem>
    </FeatherList>
  </div>
</template>

<script setup lang="ts">
import Add from '@featherds/icon/action/Add'
import Remove from '@featherds/icon/action/Remove'

const tag = ref('')
const state = useStorage<{ tags: string[]} >('local-tags', {
  tags: []
})

const addTag = () => {
  if (tag.value) {
    state.value.tags.push(tag.value)
  }
  tag.value = ''
}

const removeTag = (tagToRemove: string) => {
  state.value.tags = state.value.tags.filter((tag) => tag !== tagToRemove)
}

const duplicateTagNameError = computed(() => {
  return (tag.value && state.value.tags.includes(tag.value)) ? 'Cannot add duplicate tag name. ' : ''
})
</script>

<style lang="scss" scoped>
@import "@featherds/styles/themes/variables";

.tags-container {
  display: flex;
  flex-direction: column;
  background: var($surface);
  padding: 10px;
  border-radius: 5px;
  max-height: 300px;
  overflow-y: auto;
  .add-tag-container {
    display: flex;
    flex-direction: row;
    gap: 10px;

    .add-tag-input {
      width: 250px;
    }

    .add-tag-btn {
      margin-top: 2px;
    }
  }
}
</style>