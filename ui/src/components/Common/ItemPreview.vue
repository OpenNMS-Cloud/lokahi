<template>
  <div class="item-preview-wrapper" data-test="item-preview">
    <div class="item-preview-header" v-if="!loading">
      {{ title }}
    </div>
    <div class="item-preview-body" v-if="!loading">
      <div class="item-preview-card">
        <div class="item-preview-meta">
          <strong data-test="item-preview-meta-id">{{ itemTitle }}</strong>
          <span>{{ itemSubtitle }}</span>
        </div>
        <div class="item-preview-statuses">
          <div class="item-preview-status" v-for="(item, index) in itemStatuses" :key="index">
            <span>{{ item.title }}</span>
            <div data-test="item-preview-status-id"
              :style="{ backgroundColor: item.statusColor, color: item.statusText }">{{ item.status }}</div>
          </div>
        </div>
      </div>
    </div>
    <div v-if="bottomCopy" class="bottom-copy">{{ bottomCopy }}</div>
    <div class="item-preview-loading" v-if="loading">
      <div>
        <FeatherSpinner />
      </div>
      <div data-test="welcome-discovery-status-txt">{{ loadingCopy }}</div>
    </div>
  </div>
</template>
<script lang="ts" setup>
import { ItemPreviewProps } from './commonTypes'

withDefaults(defineProps<ItemPreviewProps>(), {
  loading: false,
  loadingCopy: '',
  title: '',
  itemTitle: '',
  itemSubtitle: '',
  bottomCopy: '',
  itemStatuses: () => []
})
</script>
<style lang="scss" scoped>
@import '@featherds/styles/themes/variables';
@import '@featherds/styles/mixins/typography';

.item-preview-wrapper {
  border: 1px solid var($border-on-surface);
  border-radius: 3px;
  margin-bottom: 24px;
}

.item-preview-header {
  padding: 6px 12px;
  background-color: var($surface);
}

.item-preview-body {
  background-color: #e5f4f9;
  padding: 12px 0;
}

.item-preview-card {
  margin: 0 12px;
  padding: 6px 12px;
  border-radius: 3px;
  border: 1px solid var($border-on-surface);
  background-color: var($surface);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.item-preview-meta {

  strong,
  span {
    display: block;
  }

  span {
    @include caption();
    color: var($disabled-text-on-surface);
  }
}

.item-preview-statuses {
  display: flex;
  align-items: center;
  gap: 24px;

  span {
    @include caption();
    display: block;
    margin-bottom: 4px;
  }

  div {
    border-radius: 8px;
    padding: 6px 8px;
    margin: 0;
    line-height: 1.1em;
    display: inline-block;
  }
}

.item-preview-loading {
  min-height: 132px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var($disabled-text-on-surface);

  :deep(.spinner-container) {
    width: 24px;
    height: 24px;
    margin-right: 12px;
  }
}

.bottom-copy {
  padding: 2px 12px;
  @include caption();
}
</style>
