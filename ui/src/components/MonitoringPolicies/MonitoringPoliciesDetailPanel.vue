<template>
  <div
    class="policy-form"
    v-if="store.selectedPolicy"
  >
    <div class="policy-form-title-container">
      <div class="form-title">{{ store.selectedPolicy.name }}</div>
      <div>
        <FeatherButton
          icon="Edit"
          @click="onEdit"
        >
          <FeatherIcon :icon="icons.Edit" />
        </FeatherButton>
         <FeatherButton
          icon="ContentCopy"
          @click="onCopy"
        >
          <FeatherIcon :icon="icons.ContentCopy" />
        </FeatherButton>
        <FeatherButton
          icon="Cancel"
          @click="onClose"
        >
          <FeatherIcon :icon="icons.Cancel" />
        </FeatherButton>
      </div>
    </div>
    <hr style="width: 100%" />
    <div class="policy-details-card-wrapper">
      <div class="card">
        <div class="sub-title">STATUS</div>
        <div class="toggle-wrapper">
          <span>Enabled</span>
          <FeatherListSwitch
            class="basic-switch"
            v-model="isEnabled"
          />
        </div>
      </div>
      <div class="card">
        <div class="sub-title">TAGS</div>
        <FeatherChipList condensed label="Tags" class="tag-chip-list" data-test="tag-chip-list">
          <FeatherChip v-for="(tag, index) in tags" :key="index" class="pointer">
            <template v-slot:icon>
              <FeatherIcon :icon="Cancel" />
            </template>
            {{ tag.name }}
          </FeatherChip>
        </FeatherChipList>
      </div>
      <div class="card">
        <div class="sub-title">ALERT RULES</div>
        <div class="alert-rules-wrapper">
          <div class="alert-rule" v-for="rule in store.selectedPolicy?.rules" :key="rule.id">
            <div class="headline">
              <span>{{ rule.name }}</span>
            </div>
            <div>
              <span>Rule Description</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useMonitoringPoliciesStore } from '@/store/Views/monitoringPoliciesStore'
import ContentCopy from '@featherds/icon/action/ContentCopy'
import Cancel from '@featherds/icon/navigation/Cancel'
import Edit from '@featherds/icon/action/Edit'
import { TagSelectItem } from '@/types'

const store = useMonitoringPoliciesStore()
const isEnabled = ref(false)
const tags = ref<TagSelectItem[]>([
  { id: '1', name: 'default' },
  { id: '2', name: 'tag1' },
  { id: '3', name: 'tag2' }
])

const emit = defineEmits<{
  (e: 'onClose'): void
}>()

const icons = markRaw({
  Cancel,
  ContentCopy,
  Edit
})

const onEdit = () => {
  console.log('onEdit clicked')
}

const onCopy = () => {
  console.log('copy clicked')
}

const onClose = () => {
  console.log('close clicked')
  emit('onClose')
}
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';
@use '@/styles/vars.scss';

.policy-form {
  display: flex;
  flex: 1;
  flex-direction: column;
  background: var(variables.$surface);
  padding: var(variables.$spacing-l);

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

.policy-details-card-wrapper {
  display: flex;
  flex-direction: column;

  .card {
  }
}

.sub-title {
  @include typography.button;
  color: var(variables.$secondary-variant);
}

.toggle-wrapper {
  text-align: left;
}

.headline {
  font-size: 1rem;
  font-weight: 600;
}
</style>
