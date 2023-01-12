<template>
  <div class="type">
    <FeatherCheckbox v-model="isSelected">
      {{label}}
    </FeatherCheckbox>
    <div>
      <FeatherIcon
        class="icon"
        :class="{selected: isSelected}"
        :icon="Settings"
        @click="isSelected && emit('show-settings')"
      />
      <FeatherIcon
        class="icon"
        :class="{selected: isSelected}"
        :icon="SupportCenter"
        @click="isSelected && emit('show-instructions')"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import Settings from '@featherds/icon/action/Settings'
import SupportCenter from '@featherds/icon/action/SupportCenter'

defineProps<{
	label: string
}>()
const emit = defineEmits(['show-settings', 'show-instructions'])
const isSelected = ref(false)
</script>
<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
.type {
        display: flex;
        align-items: center;
        justify-content: space-between;
    }
    .type:last-child {
        margin-top: 22px;
    }

    .layout-container {
        margin: 0;
    }

    .icon {
        width: 30px;
        height: 30px;
        margin-left: 15px;
        color: #e8e8e8;
        &.selected {
            color: #737373;
            cursor: pointer;
        }
    }
</style>
