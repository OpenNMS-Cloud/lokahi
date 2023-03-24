<template>
  <div class="policy-form-container">
    <div>
      <FeatherButton
        primary
        @click="store.displayPolicyForm()"
      >
        <FeatherIcon :icon="addIcon" />
        New Policy
      </FeatherButton>
      <MonitoringPoliciesExistingItems
        title="Existing Policies"
        :list="store.monitoringPolicies"
        :selectedItemId="store.selectedPolicy?.id"
        @selectExistingItem="populateForm"
      />
    </div>
    <transition name="fade-instant">
      <div
        class="policy-form"
        v-if="store.selectedPolicy"
      >
        <div class="form-title">Policy Name</div>
        <FeatherInput
          v-model="store.selectedPolicy.name"
          label="New Policy Name"
        />
        <FeatherTextarea
          v-model="store.selectedPolicy.memo"
          label="Memo"
          :maxlength="100"
        />
        <FeatherCheckboxGroup
          label="Notifications (Optional)"
          vertical
        >
          <FeatherCheckbox v-model="store.selectedPolicy.notifications.email">Email</FeatherCheckbox>
          <FeatherCheckbox v-model="store.selectedPolicy.notifications.pagerDuty">Pager Duty</FeatherCheckbox>
          <FeatherCheckbox v-model="store.selectedPolicy.notifications.webhooks">Webhooks</FeatherCheckbox>
        </FeatherCheckboxGroup>
        <div class="subtitle">Tags</div>
        <BasicAutocomplete
          @itemsSelected="selectTags"
          :getItems="tagQueries.getTagsSearch"
          :items="tagQueries.tagsSearched"
          :label="'Tag name'"
        />
      </div>

      <div
        v-else
        class="mp-card-container"
      >
        <MonitoringPoliciesCard
          v-for="(policy, index) in store.monitoringPolicies"
          :policy="policy"
          :index="index"
          @selectPolicy="(policy: IPolicy) => store.displayPolicyForm(policy)"
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
import { IPolicy } from '@/types/policies'

const store = useMonitoringPoliciesStore()
const tagQueries = useTagQueries()
const addIcon = markRaw(Add)

const selectTags = (tags: TagSelectItem[]) => (store.selectedPolicy!.tags = tags.map((tag) => ({ name: tag.name })))
const populateForm = (item: IPolicy) => {
  store.selectedPolicy = item
  store.selectedRule = undefined
}
</script>

<style scoped lang="scss">
@use '@featherds/styles/mixins/elevation';
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';
@use '@/styles/mediaQueriesMixins';
@use '@/styles/_transitionFade';
@use '@/styles/vars.scss';

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
    border-radius: vars.$border-radius-s;

    .form-title {
      @include typography.headline3;
      margin-bottom: var(variables.$spacing-m);
    }
    .subtitle {
      @include typography.subtitle1;
    }
  }

  .mp-card-container {
    display: flex;
    flex: 1;
    flex-direction: column;
    gap: var(variables.$spacing-xxs);
    margin-bottom: var(variables.$spacing-xl);;
  }

  @include mediaQueriesMixins.screen-lg {
    flex-direction: row;
  }
}
</style>
