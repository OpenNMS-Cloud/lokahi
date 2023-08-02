<template>
  <div class="cards">
    <div v-for="node in tabContent" :key="node?.id" class="card">
      <section class="node-header">
        <h5 data-test="heading" class="node-label">{{ node?.label }}</h5>
        <div class="card-chip-list">
          <div>
            <div v-for="badge, index in metricsAsTextBadges(node?.metrics)" :key="index">
              <TextBadge v-if="badge.label" :type="badge.type">{{ badge.label }}</TextBadge>
            </div>
          </div>
        </div>
      </section>
      <section class="node-content">
        <div>
          <InventoryTextAnchorList :anchor="node?.anchor" data-test="text-anchor-list" />
        </div>
      </section>
      <div class="node-footer">
        <FeatherButton class="tags-count-box" @click="openModalForDeletingTags(node)">
          Tags: <span class="count">{{ node.anchor.tagValue.length }}</span>
        </FeatherButton>
        <InventoryIconActionList :node="node" class="icon-action" data-test="icon-action-list" />
      </div>
      <InventoryNodeTagEditOverlay v-if="tagStore.isTagEditMode" :node="node" />
    </div>
  </div>
  <InventoryTagModal :visible="tagStore.isVisible" :node="tagStore.activeNode" :closeModal="tagStore.closeModal" :state="state"
    :resetState="resetState" />
</template>

<script lang="ts" setup>
import { PropType } from 'vue'
import { InventoryNode } from '@/types'
import { useTagStore } from '@/store/Components/tagStore'
import { useInventoryStore } from '@/store/Views/inventoryStore'
import { BadgeTypes } from '../Common/commonTypes'
import TextBadge from '../Common/TextBadge.vue'


defineProps({
  tabContent: {
    type: Object as PropType<InventoryNode[]>,
    required: true
  },
  state: {
    type: String,
    required: true
  }
})



const tagStore = useTagStore()
const inventoryStore = useInventoryStore()

const isTagManagerReset = computed(() => inventoryStore.isTagManagerReset)
watch(isTagManagerReset, (isReset) => {
  if (isReset) resetState()
})


const resetState = () => {
  tagStore.selectAllTags(false)
  tagStore.setTagEditMode(false)
  inventoryStore.resetSelectedNode()
  inventoryStore.isTagManagerReset = false
}

const openModalForDeletingTags = (node: InventoryNode) => {
  if (!node.anchor.tagValue.length) return
  tagStore.setActiveNode(node)
  tagStore.openModal()
}


const metricsAsTextBadges = (metrics?: [{ type: string, value: string | number, status: string }]) => {
  return metrics?.map((m) => {
    let badge = { type: BadgeTypes.info, label: '' }
    if (m.type === 'latency') {
      if (typeof m.value !== 'undefined') {
        badge = { type: BadgeTypes.success, label: m.value.toString() }
      } else {
        badge = { type: BadgeTypes.error, label: '' }
      }
    } else if (m.type === 'status') {
      if (m.status === 'UP') {
        badge = { type: BadgeTypes.success, label: m.status.toLowerCase() }
      } else {
        badge = { type: BadgeTypes.error, label: m.status.toLowerCase() }
      }
    }
    return badge
  }) || []
}

</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@/styles/vars';
@use '@/styles/mediaQueriesMixins';
@use '@featherds/styles/mixins/typography';

.ctrls {
  display: flex;
  justify-content: end;
  padding: var(variables.$spacing-s) 0;
  min-width: vars.$min-width-smallest-screen;
}

.cards {
  display: flex;
  flex-flow: row wrap;
  gap: 1%;

  .card {
    position: relative;
    padding: var(variables.$spacing-m) var(variables.$spacing-m);
    border: 1px solid rgba(21, 24, 43, 0.12);
    border-radius: 4px;
    width: 100%;
    min-width: vars.$min-width-smallest-screen;
    margin-bottom: var(variables.$spacing-m);
    background: var(variables.$surface);

    @include mediaQueriesMixins.screen-sm {
      width: 100%;
    }

    @include mediaQueriesMixins.screen-md {
      width: auto;
      min-width: 443px;
    }


    .node-header {
      margin-bottom: var(variables.$spacing-s);
      display: flex;
      flex-direction: row;
      gap: 0.5rem;
      align-items: center;
      justify-content: space-between;
    }

    .node-label {
      margin: 0;
      line-height: 20px;
      letter-spacing: 0.28px;
    }
  }


}


.node-content {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  gap: 2rem;

}

.node-footer {
  display: flex;
  justify-content: space-between;
  margin-top: 40px;
}
</style>
