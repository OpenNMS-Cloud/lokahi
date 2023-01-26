<template>
  <div class="new-policy">
    <div class="title">New Policy</div>
    <div class="form-new-policy">
      <FeatherInput
        v-model="store.selectedPolicy.name"
        label="New Policy Name"
        class="name-policy"
      />
      <FeatherButton
        class="btn-add-tag"
        secondary
        @click="showTags = !showTags"
      >
        Add Tags
        <FeatherIcon :icon="showTags ? Icons.ExpandLess : Icons.ExpandMore" />
      </FeatherButton>
      <div class="list-selected-tags">
        <div
          class="selected-tag"
          v-for="tag in store.selectedPolicy.tags"
          :key="tag"
        >
          {{ tag }}
          <FeatherIcon
            class="remove-icon"
            :icon="Icons.Cancel"
            @click="store.removeTag(tag)"
          />
        </div>
      </div>
    </div>
  </div>
  <div class="tags-section">
    <div v-if="showTags">Tag manager component</div>
  </div>
</template>

<script lang="ts" setup>
import { useMonitoringPoliciesStore } from '@/store/Views/monitoringPoliciesStore'
import ExpandMore from '@featherds/icon/navigation/ExpandMore'
import ExpandLess from '@featherds/icon/navigation/ExpandLess'
import Cancel from '@featherds/icon/navigation/Cancel'

const store = useMonitoringPoliciesStore()
const Icons = markRaw({
  ExpandMore,
  ExpandLess,
  Cancel
})
const showTags = ref(false)
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';

.new-policy {
  .title {
    @include typography.headline4;
    margin-bottom: var(variables.$spacing-s);
  }

  .name-policy {
    width: 350px;
    margin-right: var(variables.$spacing-l);
  }
  .form-new-policy {
    display: flex;
    .btn-add-tag {
      height: 40px;
    }
  }
  .list-selected-tags {
    display: flex;
    flex-wrap: wrap;
    margin-left: var(variables.$spacing-l);
    width: 70%;

    > .selected-tag {
      background-color: var(variables.$shade-1);
      padding-left: var(variables.$spacing-m);
      padding-right: var(variables.$spacing-s);
      margin-right: var(variables.$spacing-s);
      border-radius: 5px;
      height: 26px;
      margin-bottom: var(variables.$spacing-s);
      display: flex;
      align-items: center;
      color: var(variables.$primary-text-on-color);
    }

    .remove-icon {
      font-size: 18px;
      cursor: pointer;
      margin-left: var(variables.$spacing-xs);
    }
  }
}

.tags-section {
  width: 80%;
}

:deep(.feather-input-sub-text) {
  display: none;
}
</style>
