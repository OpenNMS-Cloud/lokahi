<template>
    <div :class="['welcome-slide-three-wrapper', visible ? 'visible' : 'hidden',
    ]">
        <div class="welcome-slide-three-inner">
            <div class="welcome-slide-three-title">
                <h2 data-test="welcome-slide-three-title">Set Up Your First Device</h2>
                <p>Let's use node discovery to identify one of your network devices. </p>
            </div>
            <div class="welcome-slide-three-form">
                <FeatherInput label="Node Discovery Name" data-test="welcome-slide-three-name-input"
                    :modelValue="welcomeStore.firstDiscovery.name" :error="welcomeStore.firstDiscoveryErrors.name"
                    @update:modelValue="(e: any) => welcomeStore.updateFirstDiscovery('name', e)" />
                <FeatherInput label="Management IPV4" data-test="welcome-slide-three-ip-input"
                    :modelValue="welcomeStore.firstDiscovery.ip" :error="welcomeStore.firstDiscoveryErrors.ip"
                    @update:modelValue="(e: any) => welcomeStore.updateFirstDiscovery('ip', e)" />
                <FeatherInput label="Community String (optional)" data-test="welcome-slide-three-community-string-input"
                    :modelValue="welcomeStore.firstDiscovery.communityString"
                    :error="welcomeStore.firstDiscoveryErrors.communityString"
                    @update:modelValue="(e: any) => welcomeStore.updateFirstDiscovery('communityString', e)" />
                <FeatherInput label="Port (optional)" data-test="welcome-slide-three-port-input"
                    :modelValue="welcomeStore.firstDiscovery.port"
                    :error="welcomeStore.firstDiscoveryErrors.communityString"
                    @update:modelValue="(e: any) => welcomeStore.updateFirstDiscovery('port', e)" />
            </div>

            <ItemPreview v-if="welcomeStore.discoverySubmitted" :loading="welcomeStore.devicePreview.loading"
                :title="welcomeStore.devicePreview.title" :loadingCopy="welcomeStore.devicePreview.loadingCopy"
                :itemTitle="welcomeStore.devicePreview.itemTitle" :itemSubtitle="welcomeStore.devicePreview.itemSubtitle"
                :bottomCopy="welcomeStore.devicePreview.bottomCopy"
                :itemStatuses="welcomeStore.devicePreview.itemStatuses" />
            <div class="welcome-slide-footer">
                <FeatherButton data-test="welcome-store-slide-three-skip-button" text @click="welcomeStore.skipSlideThree">
                    Skip
                </FeatherButton>
                <FeatherButton data-test="welcome-store-slide-three-continue-button" v-if="welcomeStore.discoverySubmitted"
                    primary :disabled="welcomeStore.slideThreeDisabled" @click="welcomeStore.skipSlideThree">
                    Continue
                </FeatherButton>
                <FeatherButton v-if="!welcomeStore.discoverySubmitted"
                    data-test="welcome-store-page-three-start-discovery-button" primary
                    @click="welcomeStore.startDiscovery">
                    Start Monitoring
                </FeatherButton>
            </div>
        </div>
    </div>
</template>
<script lang="ts" setup>
import { useWelcomeStore } from '@/store/Views/welcomeStore'
import ItemPreview from '../Common/ItemPreview.vue'
defineProps({
    visible: { type: Boolean, default: false }
})
const welcomeStore = useWelcomeStore()
</script>
<style lang="scss" scoped>
@import '@featherds/styles/themes/variables';
@import '@featherds/styles/mixins/typography';

.welcome-slide-three-wrapper {
    border: 1px solid var($border-on-surface);
    border-radius: 3px;
    padding: 40px 24px;
    max-width: 660px;
    width: 100%;
    margin-bottom: 40px;
    background-color: var($surface);
    position: absolute;

    left: 0;
    right: 0;

    h2 {
        @include headline2();
        margin-bottom: 12px;
    }
}

.welcome-slide-three-title {
    margin-bottom: 32px;
}

.welcome-slide-three-form {
    :deep(.feather-input-container) {
        margin-bottom: 4px;
    }
}

.welcome-slide-footer {
    display: flex;
    justify-content: flex-end;
}

.visible {
    opacity: 1;
    transition: opacity 0.1s ease-in-out 0.2s, transform 0.2s ease-out 0.2s;
    transform: translateY(0px);
    pointer-events: all;
}

.hidden {
    opacity: 0;
    transition: opacity 0.2s ease-in-out 0s, transform 0.3s ease-in 0s;
    transform: translateY(-20px);
    pointer-events: none;
}
</style>