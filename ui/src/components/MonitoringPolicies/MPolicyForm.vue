<template>
  <div class="policy-form-container">
    <div>
      <FeatherButton primary @click="store.displayPolicyForm()">
        <FeatherIcon :icon="addIcon" />
        Create New Policy
      </FeatherButton>
      <MPolicyExistingItems
        title="Existing Policies"
        :list="[{ name: 'Test'}, { name: 'Testlongername'}]" 
      />
    </div>
    <transition name="fade">
      <div class="policy-form" v-if="store.selectedPolicy">
        <div class="form-title">
          Create New Policy
        </div>
        <div class="form-subtitle">
          New Policy Name
        </div>
        <FeatherInput
          v-model="store.selectedPolicy.name"
          label="Policy Name"
        />
        <FeatherTextarea
          v-model="store.selectedPolicy.memo"
          label="Memo"
          :maxlength="100"/>
        <FeatherCheckboxGroup
          label="Notifications (Optional)"
          vertical>
          <FeatherCheckbox v-model="store.selectedPolicy.notifications.email">Email</FeatherCheckbox>
          <FeatherCheckbox v-model="store.selectedPolicy.notifications.pagerDuty">Pager Duty</FeatherCheckbox>
          <FeatherCheckbox v-model="store.selectedPolicy.notifications.webhooks">Webhooks</FeatherCheckbox>
        </FeatherCheckboxGroup>
        <div class="form-subtitle">
          Tags
        </div>
        <BasicAutocomplete
          @items-selected="selectTags"
          :get-items="tagQueries.getTagsSearch"
          :items="tagQueries.tagsSearched"
          :label="'Tag name'"
        />
      </div>
    </transition>
  </div>
  <hr v-if="store.selectedPolicy" />
</template>

<script setup lang="ts">
import { useMonitoringPoliciesStore } from '@/store/Views/monitoringPoliciesStore'
import { useTagQueries } from '@/store/Queries/tagQueries'
import Add from '@featherds/icon/action/Add'
import { TagSelectItem } from '@/types'

const store = useMonitoringPoliciesStore()
const tagQueries = useTagQueries()
const addIcon = markRaw(Add)

const selectTags = (tags: TagSelectItem[]) => store.selectedPolicy!.tags = tags.map((tag) => ({ name: tag.name }))
</script>

<style scoped lang="scss">
@use "@featherds/styles/mixins/elevation";
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';
@use '@/styles/mediaQueriesMixins';
@use "@/styles/_transitionFade";

.policy-form-container {
  display: flex;
  flex-direction: column;
  gap: var(variables.$spacing-l);

  .policy-form {
    @include elevation.elevation(2);
    display: flex;
    flex: 1;
    flex-direction: column;
    background: var(variables.$surface);
    padding: var(variables.$spacing-l);
    border-radius: 5px;

    .form-title {
      @include typography.headline3;
      margin-bottom: var(variables.$spacing-m);
    }
    .form-subtitle {
      @include typography.subtitle1;
      margin-bottom: var(variables.$spacing-m);
    }
  }

  @include mediaQueriesMixins.screen-md {
    flex-direction: row;
  }
}
</style>