<template>
    <div :class="[
        'welcome-slide-two-wrapper',
        isDark ? 'dark' : 'light',
        visible ? 'visible' : 'hidden',
        welcomeStore.slide > 2 ? 'down' : '',
        welcomeStore.slide < 2 ? 'up' : ''
    ]">
        <div class="welcome-slide-two-inner">
            <div class="welcome-slide-two-title">
                <h1 data-test="welcome-slide-two-title">Minion Installation</h1>
                <p>An OpenNMS Minion is a virtual or hardware device for distributed network monitoring. Your network must
                    include at least one Minion to use Cloud.</p>
            </div>
            <div class="welcome-slide-two-location-callout">
                <div class="welcome-slide-two-location-callout-left" data-test="welcome-slide-two-callout">
                    <InformationIcon />
                </div>
                <div class="welcome-slide-two-location-callout-right">
                    <span>We have created a default location for your first Minion.</span>
                    <a href="https://docs.opennms.com/cloud/locations.html" target="_blank" rel="noopener noreferrer">Learn
                        More About
                        Locations</a>
                </div>
            </div>
            <div class="welcome-slide-step">
                <h2>Step 1: Download Minion Installation Bundle</h2>
                <div class="welcome-slide-table">
                    <div class="welcome-slide-table-header">
                        <div>Minion Installation Bundle</div>
                        <div>
                            <FeatherButton text @click="welcomeStore.downloadClick" v-if="!welcomeStore.downloading"
                                data-test="welcome-slide-two-download-button">
                                <template #icon>
                                    <FeatherIcon :icon="welcomeStore.downloaded ? CheckIcon : DownloadIcon" />
                                </template>
                                {{ welcomeStore.downloadCopy }}
                            </FeatherButton>
                            <div class="loader-button" v-if="welcomeStore.downloading">
                                <div class="loader-button-inner">
                                    <FeatherSpinner />
                                    {{ welcomeStore.downloadCopy }}
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="welcome-slide-table-body">
                        <strong>File Path: </strong>
                        <span>/tmp/default-certificate.p12</span>
                    </div>
                </div>
            </div>
            <CollapsingWrapper :open="!!welcomeStore.minionCert.password">
                <div class="welcome-slide-step" data-test="welcome-page-two-internal">
                    <h2>Step 2: Copy and Run Docker Install Command</h2>
                    <pre>Replace pathToFile with the path to the certificate. (e.g., /tmp/)</pre>
                    <div class="welcome-slide-table">
                        <div class="welcome-slide-table-header">
                            <span>Command</span>
                            <div>
                                <FeatherButton text @click="welcomeStore.copyDockerClick"
                                    :disabled="!welcomeStore.minionCert.password">
                                    <template #icon>
                                        <FeatherIcon :icon="welcomeStore.copied ? CheckIcon : CopyIcon" />
                                    </template>
                                    {{ welcomeStore.copyButtonCopy }}
                                </FeatherButton>
                            </div>
                        </div>
                        <div class="welcome-slide-table-body">
                            <pre>
                            {{ dockerCmd }}
                        </pre>
                        </div>
                    </div>
                </div>

                <div class="welcome-slide-step">
                    <h2>Step 3: Detect Your Minion</h2>
                    <p>We will automatically detect your Minion once it is set up.</p>

                    <div
                        :class="['welcome-slide-minion-status', welcomeStore.minionStatusSuccess ? 'welcome-slide-minion-success' : '']">
                        <div class="icon-spin" data-test="welcome-slide-two-icon-spin">
                            <FeatherSpinner v-if="welcomeStore.minionStatusLoading && welcomeStore.minionStatusStarted" />
                            <FeatherIcon :icon="CheckIcon"
                                v-if="!welcomeStore.minionStatusLoading && welcomeStore.minionStatusSuccess" />
                        </div>
                        <div class="copy" data-test="welcome-minion-status-txt">
                            {{ welcomeStore.minionStatusCopy }}
                        </div>
                    </div>
                </div>
            </CollapsingWrapper>

            <div class="welcome-slide-footer">
                <FeatherButton text @click="welcomeStore.prevSlide" data-id="welcome-slide-two-back-button">Back
                </FeatherButton>
                <FeatherButton primary :disabled="!welcomeStore.minionStatusSuccess"
                    data-id="welcome-slide-two-continue-button" @click="welcomeStore.nextSlide">
                    Continue
                </FeatherButton>

            </div>
        </div>
    </div>
</template>
<script lang="ts" setup>
import { useWelcomeStore } from '@/store/Views/welcomeStore'
import CopyIcon from '@featherds/icon/action/ContentCopy'
import CheckIcon from '@featherds/icon/action/CheckCircle'
import DownloadIcon from '@featherds/icon/action/DownloadFile'
import InformationIcon from '@featherds/icon/action/Info'
import useTheme from '@/composables/useTheme'
import CollapsingWrapper from '../Common/CollapsingWrapper.vue'
import { FeatherSpinner } from '@featherds/progress'
defineProps({
    visible: { type: Boolean, default: false }
})
const dockerCmd = computed(() => welcomeStore.dockerCmd());
const welcomeStore = useWelcomeStore()
const { isDark } = useTheme();
</script>
<style lang="scss" scoped>
@import '@featherds/styles/themes/variables';
@import '@featherds/styles/mixins/typography';

.welcome-slide-two-wrapper {
    border: 1px solid var($border-on-surface);
    background-color: var($surface);
    border-radius: 3px;
    padding: 35px 40px 40px 40px;
    max-width: 660px;
    margin-bottom: 40px;
    margin-top: -4px;
    width: 100%;
    position: absolute;

    h1 {
        @include headline1();
        margin-bottom: 16px;
    }
}

.welcome-slide-two-wrapper.dark {

    .welcome-slide-two-location-callout,
    .welcome-slide-table-body {
        background-color: #e5f4f9;
        color: var($primary-text-on-color);
    }
}

.welcome-slide-two-wrapper.light {

    .welcome-slide-two-location-callout,
    .welcome-slide-table-body {
        background-color: #e5f4f9;
    }
}


.welcome-slide-two-location-callout {
    display: flex;
    align-items: center;
    border-radius: 3px;
    gap: 16px;
    padding: 12px 16px;
    margin: 28px 0;

    .welcome-slide-two-location-callout-left {
        width: 30px;
        font-size: 24px;

        svg {
            fill: #5cb9db;
        }
    }

    a {
        color: #273180;
        display: block;
    }
}

.welcome-slide-step {
    margin-bottom: 32px;

    h2 {
        @include headline4();
        margin-bottom: 12px;
    }

    .welcome-slide-table {
        border: 1px solid var($border-on-surface);
        border-radius: 3px;

        .welcome-slide-table-header {
            color: var($disabled-text-on-surface);
            padding: 12px 24px;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .welcome-slide-table-body {
            padding: 12px 24px;

            pre {
                margin: 0;
                white-space: normal;
                word-wrap: break-word;
            }
        }
    }
}

.welcome-slide-minion-status {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 24px 0;
    border: 1px solid var($border-on-surface);
    border-radius: 3px;
    margin-top: 24px;
    color: var($disabled-text-on-surface);

    :deep(.spinner-container) {
        width: 24px;
        height: 24px;
        margin-right: 24px;
    }
}

.welcome-slide-footer {
    display: flex;
    justify-content: flex-end;
}

.visible {
    opacity: 1;
    pointer-events: all;
    transform: translateY(0px);
    transition: opacity 0.1s ease-in-out 0.2s, transform 0.2s ease-out 0.2s;
}

.hidden {
    opacity: 0;
    transition: opacity 0.2s ease-in-out 0s, transform 0.3s ease-in 0s;
    pointer-events: none;
}

.down.hidden {
    transform: translateY(20px);
}

.up.hidden {
    transform: translateY(-20px);
}

.welcome-slide-minion-success {
    background-color: rgba(11, 114, 12, 0.2);
}

.welcome-slide-minion-success .icon-spin {
    color: var($success);
    font-size: 26px;
    margin-right: 12px;
}

.welcome-slide-minion-success .copy {
    color: var($primary-text-on-surface);
    font-weight: 700;
}

.loader-button {
    position: relative;
    left: -10px;
}

.loader-button-inner {
    display: flex;
    align-items: center;
    font-weight: 700;
    text-transform: uppercase;
    max-height: 36px;

    :deep(svg) {
        max-width: 15px;
        margin-right: 12px;
    }
}
</style>