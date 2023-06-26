<template>
    <div :class="['collapse-box', open ? 'collapse-box-open' : 'collapse-box-closed']"
        data-test="collapsing-wrapper-wrapper">
        <div class="collapse-box-contents">
            <slot></slot>
        </div>
    </div>
</template>
<script setup lang="ts">

defineProps({
    open: { type: Boolean, default: false },
})

</script>

<style lang="scss" scoped>
.collapse-box-closed .collapse-box-contents {
    opacity: 0;
    transition: max-height 0.4s ease-out 0s, opacity 0.2s ease-out 0.2s, padding-top 0.4s ease-out 0s, padding-bottom 0.3s ease-out 0s;
    max-height: 0vh;
}

.collapse-box-open .collapse-box-contents {
    opacity: 1;
    transition: max-height 0.7s ease-out 0s, opacity 0.4s ease-out 0.3s, padding-top 0.5s ease-out 0s, padding-bottom 0.4s ease-out 0s;
    max-height: 1000vh;
}
</style>