<template>
    <span class="feather-text-badge" :class="type">
        <slot />
    </span>
</template>
<script setup lang="ts">
import { PropType } from 'vue'
import { BadgeTypes } from './commonTypes'

defineProps({
  type: { type: String as PropType<keyof typeof BadgeTypes>, required: true }
})


</script>
<style scoped lang="scss">
@import "@featherds/styles/themes/variables";
@import "@featherds/styles/themes/utils";
@import "@featherds/styles/mixins/typography";

.feather-text-badge {
    height: 1.25rem;
    padding: 0.125rem 0.5rem;
    display: inline-flex;
    align-items: center;
    border-radius: 6px;
    @include caption;
    font-weight: var($font-bold);
    margin-left: 0.5rem;
    text-transform: capitalize;

    &.error {
        color: var(--feather-error);
        background-color: color-mix(in srgb, var(--feather-error) 12%, white);
    }

    &.info {
        color: var(--feather-primary);
        background-color: color-mix(in srgb, var(--feather-primary) 12%, white);
    }

    &.success {
        color: #0B720C;
        background-color: color-mix(in srgb, #0B720C 12%, white);
    }

}
</style>