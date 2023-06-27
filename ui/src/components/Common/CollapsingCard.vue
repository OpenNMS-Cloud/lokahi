 
<template>
    <div :class="['collapse-box', open ? 'collapse-box-open' : 'collapse-box-closed']" data-test="collapsing-card-wrapper">
        <div class="collapse-box-header" @click="headerClicked" v-bind="$attrs">
            <span class="collapse-box-header-icon-wrapper">
                <span class="collapse-box-header-icon">
                    <slot name="icon"></slot>
                </span>
                <span class="collapse-box-header-title">{{ title }}</span>
            </span>
            <span class="collapse-box-header-chevron">
                <FeatherIcon :icon="ExpandMore" title="ExpandMore" />
            </span>
        </div>
        <div class="collapse-box-contents">
            <slot name="body"></slot>
        </div>
    </div>
</template>
<script lang='ts' setup>
import ExpandMore from '@featherds/icon/navigation/ExpandMore'
import { PropType } from 'vue'
defineOptions({ inheritAttrs: false })
defineProps({
    title: { type: String, default: '' },
    open: { type: Boolean, default: false },
    headerClicked: { type: Function as PropType<() => void>, default: () => ({}) }
})

</script>
<style lang="scss" scoped>
@import '@featherds/styles/mixins/typography';
@import '@featherds/styles/themes/variables';

.collapse-box {
    border: 1px solid var($border-light-on-surface);
    padding: 8px 16px;
    margin-bottom: 16px;
    color: var($secondary-text-on-surface);
    background-color: var($surface);
    border-radius: 5px;
}

.collapse-box-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    max-height: 26px;
    cursor: pointer;
    color: var($secondary-text-on-surface);
}

.collapse-box-contents :deep(a) {
    color: var($clickable-normal);
    display: flex;
    align-items: center;
}

.collapse-box-contents :deep(a) svg {
    font-size: 24px;
}


.collapse-box-header-icon {
    margin-right: 15px;
    height: 24px;
    width: 20px;
    font-size: 24px;
    margin-left: -4px;
}

.collapse-box-header-icon-wrapper {
    display: flex;
    align-items: center;
}

.collapse-box-header-title {
    @include subtitle2();
}

.collapse-box-closed .collapse-box-contents {
    opacity: 0;
    transition: max-height 0.4s ease-out 0s, opacity 0.2s ease-out 0.2s, padding-top 0.4s ease-out 0s, padding-bottom 0.3s ease-out 0s;
    max-height: 0vh;
    padding-top: 0px;
    padding-bottom: 0px;
    padding-left: 32px;
    padding-right: 32px;
}

.collapse-box-open .collapse-box-contents {
    opacity: 1;
    transition: max-height 0.7s ease-out 0s, opacity 0.4s ease-out 0.3s, padding-top 0.5s ease-out 0s, padding-bottom 0.4s ease-out 0s;
    max-height: 100vh;
    padding-top: 16px;
    padding-bottom: 16px;
    padding-left: 32px;
    padding-right: 32px;
}

.collapse-box-header svg {
    fill: var($secondary-text-on-surface);
}

.collapse-box-closed .collapse-box-header-chevron svg {
    transform: rotate(0deg);
    transition: transform 0.3s ease-in-out;
}

.collapse-box-open .collapse-box-header-chevron svg {
    transform: rotate(180deg);
    transition: transform 0.3s ease-in-out;
}

.collapse-box-header-chevron {
    font-size: 24px;
}
</style>
