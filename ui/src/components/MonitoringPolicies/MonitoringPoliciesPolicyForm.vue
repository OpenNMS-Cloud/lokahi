<template>
  <div class="policy-form-container">
    <div>
      <FeatherButton
        primary
        @click="store.displayPolicyForm()"
        data-test="new-policy-btn"
      >
        <FeatherIcon :icon="icons.Add" />
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
        <div class="policy-form-title-container">
          <div class="form-title">Policy Name</div>
          <FeatherButton
            icon="Copy"
            @click="store.copyPolicy(store.selectedPolicy!)"
          >
            <FeatherIcon :icon="ContentCopy" />
          </FeatherButton>
        </div>

        <FeatherInput
          v-model="store.selectedPolicy.name"
          label="New Policy Name"
          v-focus
          data-test="policy-name-input"
          :readonly="store.selectedPolicy.isDefault"
        />
        <FeatherTextarea
          v-model="store.selectedPolicy.memo"
          label="Memo"
          :maxlength="100"
          :disabled="store.selectedPolicy.isDefault"
        />
        <FeatherCheckboxGroup
          label="Notifications (Optional)"
          vertical
        >
          <FeatherCheckbox
            :disabled="store.selectedPolicy.isDefault"
            v-model="store.selectedPolicy.notifyByEmail"
            >Email</FeatherCheckbox
          >
          <FeatherCheckbox
            :disabled="store.selectedPolicy.isDefault"
            v-model="store.selectedPolicy.notifyByPagerDuty"
            >PagerDuty</FeatherCheckbox
          >
          <FeatherCheckbox
            :disabled="store.selectedPolicy.isDefault"
            v-model="store.selectedPolicy.notifyByWebhooks"
            >Webhooks</FeatherCheckbox
          >
        </FeatherCheckboxGroup>
        <div class="subtitle">Tags</div>
        <BasicAutocomplete
          @itemsSelected="selectTags"
          :getItems="tagQueries.getTagsSearch"
          :items="tagQueries.tagsSearched"
          :label="'Tag name'"
          :preselectedItems="formattedTags"
          :disabled="store.selectedPolicy.isDefault"
        />
      </div>

      <div
        v-else
        class="mp-card-container"
      >
        <MonitoringPoliciesCard
          v-for="(policy, index) in store.monitoringPolicies"
          :key="policy.id"
          :policy="(policy as Policy)"
          :index="index"
          @selectPolicy="(policy: Policy) => store.displayPolicyForm(policy)"
          @copyPolicy="(policy: Policy) => store.copyPolicy(policy)"
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
import { Policy } from '@/types/policies'
import { TagSelectItem } from '@/types'
import ContentCopy from '@featherds/icon/action/ContentCopy'

const store = useMonitoringPoliciesStore()
const tagQueries = useTagQueries()
const icons = markRaw({
  Add,
  ContentCopy
})

const selectTags = (tags: TagSelectItem[]) => (store.selectedPolicy!.tags = tags.map((tag) => tag.name))
const populateForm = (policy: Policy) => store.displayPolicyForm(policy)
const formattedTags = computed(() => store.selectedPolicy!.tags!.map((tag: string) => ({ name: tag, id: tag })))
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

    .policy-form-title-container {
      display: flex;
      justify-content: space-between;
      .form-title {
        @include typography.headline3;
        margin-bottom: var(variables.$spacing-m);
      }
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
    margin-bottom: var(variables.$spacing-xl);
  }

  @include mediaQueriesMixins.screen-lg {
    flex-direction: row;
  }
}
</style>
