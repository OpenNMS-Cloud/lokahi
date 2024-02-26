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
                <h1 data-test="welcome-slide-two-title">
                    Install Secure Minion
                    <FeatherTooltip
                        title="A Minion is a lightweight, secure collector that monitors and communicates with your network devices."
                        v-slot="{ attrs, on }"
                    >
                        <FeatherIcon
                            v-bind="attrs"
                            v-on="on"
                            class="info-icon"
                            :icon="icons.InformationIcon"
                        />
                    </FeatherTooltip>
                </h1>
                <p>
                    To install our secure Minion, you must download our runtime bundle and run it in your desired location.
                    For optimal monitoring, the Minion needs to be available at all times.
                </p>
                <br>
                <p>Our runtime bundle contains the following:</p>
                <br>
                <ul>
                    <li>
                        An encrypted Minion certificate with a decryption password
                    </li>
                    <li>
                        A Docker Compose file with a Minion container
                    </li>
                </ul>
                <br>
                <p>
                    Installation can take up to 10 minutes.
                </p>
            </div>

            <div class="welcome-slide-step">
                <h2>Step 1: Download Secure Minion Runtime Bundle</h2>
                <pre
                    class="pre-wrap">Select a permanent location for the download (e.g., /minion/minion-default.zip). Future Minion restarts require the certificate.</pre>

                <div class="welcome-slide-table">
                    <div class="welcome-slide-table-header">
                        <div>Secure Minion Runtime Bundle</div>
                        <div>
                            <FeatherButton text @click="localDownloadHandler" v-if="!welcomeStore.downloading"
                                data-test="welcome-slide-two-download-button">
                                <template #icon>
                                    <FeatherIcon :icon="welcomeStore.downloaded ? icons.CheckIcon : icons.DownloadIcon" />
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
                </div>
            </div>

            <CollapsingWrapper :open="!!welcomeStore.minionCert.password">
                <div class="welcome-slide-step" data-test="welcome-page-two-internal">
                    <h2>Step 2: Copy the Bundle to a Permanent Location</h2>
                    <pre class="pre-wrap">Unzip the bundle in a permanent location. <br>You will need to access this information if you restart your network. This location is also where logs are saved.</pre>
                </div>
                <div class="welcome-slide-step" data-test="welcome-page-two-internal">
                    <h2>Step 3: Run Minion in a Terminal Window</h2>
                    <pre
                        class="pre-wrap">To install the Minion, open a terminal window, navigate to the bundle location, and type the following command:</pre>
                    <div class="welcome-slide-table docker-cmd">
                        <div class="welcome-slide-table-header docker-cmd">
                            <div class="welcome-slide-table-body">
                                <span class="cmd-text">docker compose up -d</span>
                                <FeatherButton class="dl-btn" text @click="welcomeStore.copyDockerClick">
                                    <template #icon>
                                        <FeatherIcon :icon="welcomeStore.copied ? icons.CheckIcon : icons.CopyIcon" />
                                    </template>
                                    {{ welcomeStore.copyButtonCopy }}
                                </FeatherButton>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="welcome-slide-step">
                    <h2>Step 4: Detect Your Minion</h2>
                    <p>We will automatically detect your Minion once it is set up.</p>

                    <div
                        :class="['welcome-slide-minion-status', welcomeStore.minionStatusSuccess ? 'welcome-slide-minion-success' : '']">
                        <div class="icon-spin" data-test="welcome-slide-two-icon-spin">
                            <FeatherSpinner v-if="welcomeStore.minionStatusLoading && welcomeStore.minionStatusStarted" />
                            <FeatherIcon :icon="icons.CheckIcon"
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

const icons = markRaw({
  DownloadIcon,
  InformationIcon,
  CopyIcon,
  CheckIcon
})

const localDownloadHandler = () => {
  welcomeStore.downloadClick()
}

const welcomeStore = useWelcomeStore()
const { isDark } = useTheme()
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
    left: 0;
    right: 0;

    h1 {
        @include headline1();
        margin-bottom: 16px;
    }
}

.welcome-slide-two-wrapper.dark {

    .welcome-slide-table-body {
        background-color: var($background);
        color: var($primary-text-on-color);
    }
}

.welcome-slide-two-wrapper.light {
    .welcome-slide-table-body {
        background-color: var(--feather-background);
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

        &.docker-cmd {
            border: none;
        }

        .welcome-slide-table-header {
            color: var($primary-text-on-surface);
            padding: 12px 24px;
            display: flex;
            justify-content: space-between;
            align-items: center;

            &.docker-cmd {
                padding: 0px;
            }
        }

        .welcome-slide-table-body {
            width: 100%;
            padding: 12px 24px;
            position: relative;
            display: flex;
            align-items: center;

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
    padding: 24px;
    border: 1px solid var($border-on-surface);
    border-radius: 3px;
    margin-top: 24px;
    color: var($disabled-text-on-surface);
    font-size: 0.9em;

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
    font-size: 0.9em;
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

.cmd-text {
    width: 100%;
    color: var($primary-text-on-surface);
}
.dl-btn {
    width: 110px;
}

.pre-wrap {
    white-space: pre-wrap;
    @include body-small();
}

.copy-icon {
    font-size: 36px;
    margin-left: var(--feather-spacing-s);
}

.welcome-slide-two-title {
    margin-bottom: 16px;

    :deep(svg) {
        cursor: pointer;
    }

    li {
        list-style-type: disc;
        list-style-position: inside;
        margin-left: 10px;
    }
}

.info-icon {
    color: var(--feather-shade-2);
    font-size: 22px;
    top: -3px;
    left: -3px;
    position: relative;
}
</style>
