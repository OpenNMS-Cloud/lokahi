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
                <h1 data-test="welcome-slide-two-title">Minion<FeatherIcon class="info-icon" :icon="InformationIcon">
                    </FeatherIcon>
                    Installation
                </h1>
                <p>The Minion installation can take up to 10 minutes. You will download an encrypted certificate for the
                    Minion and run a Docker command.</p>
            </div>

            <div class="welcome-slide-step">
                <h2>Step 1: Download Encrypted Minion Certificate</h2>
                <pre
                    class="pre-wrap">Select a permanent location for the download (e.g., /minion/default-certificate.p12). Future Minion restarts require the certificate.</pre>

                <div class="welcome-slide-table">
                    <div class="welcome-slide-table-header">
                        <div>Encrypted Minion Certificate</div>
                        <div>
                            <FeatherButton text @click="localDownloadHandler" v-if="!welcomeStore.downloading"
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
                </div>
            </div>
            <CollapsingWrapper :open="!!welcomeStore.minionCert.password">
                <div class="welcome-slide-step" data-test="welcome-page-two-internal">
                    <h2>Step 2: Copy and Run Docker Install Command in Terminal Window</h2>
                    <pre
                        class="pre-wrap">In the command, replace <strong>PATH_TO_DOWNLOADED_FILE</strong> with the full path to the certificate you downloaded. Remember to store your password, certificate and Docker command securely. You need all three to run your Minion.</pre>
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
                            <textarea :spellcheck="false" ref="textareaRef" @click="highlightStrip"
                                @input="(e) => updateDockerCommand((e.target as HTMLTextAreaElement)?.value || '')"
                                :value="welcomeStore.dockerCmd()" class="styled-like-pre" />
                        </div>
                        <div class="password-right">Password:&nbsp;{{ welcomeStore.minionCert.password }}
                            Save the password somewhere safe.
                            <span v-if="passwordCopyEnabled">
                                <FeatherButton icon="CopyIcon" @click="copyPassword" v-if="!copied">
                                    <FeatherIcon :icon="CopyIcon"></FeatherIcon>
                                </FeatherButton>
                                <FeatherIcon class="copy-icon" :icon="CheckIcon" v-if="copied"></FeatherIcon>
                            </span>
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
import { ref } from 'vue';

const textareaRef = ref();
const copied = ref(false);
const passwordCopyEnabled = ref(false);

defineProps({
    visible: { type: Boolean, default: false }
})

const updateDockerCommand = async (newCommand: string) => {
    welcomeStore.updateDockerCommand(newCommand)
    await nextTick();
    updateScrollHeight();
}
onMounted(() => {
    updateScrollHeight();
    window.addEventListener('resize', updateScrollHeight);
})
onUnmounted(() => {
    window.removeEventListener('resize', updateScrollHeight);
    const welcomeWrapper = document.querySelector('.welcome-wrapper');
    if (welcomeWrapper) {
        welcomeWrapper.removeEventListener('click', highlightStrip);
    }
})

const updateScrollHeight = () => {
    textareaRef.value.style = `height: 0px;`
    textareaRef.value.style = `height: ${Number(textareaRef.value.scrollHeight) + 20 + 'px'};`
}

const localDownloadHandler = () => {
    welcomeStore.downloadClick();
    const welcomeWrapper = document.querySelector('.welcome-wrapper');
    if (welcomeWrapper) {
        welcomeWrapper.addEventListener('click', highlightStrip);
    }
}

const highlightStrip = () => {
    textareaRef.value.classList.add('visible');
    const stringToHighlight = 'PATH_TO_DOWNLOADED_FILE';
    const existingCommand = welcomeStore.dockerCmd();
    const indexOf = existingCommand.indexOf(stringToHighlight);
    if (indexOf >= 0) {
        textareaRef.value.focus();
        textareaRef.value.setSelectionRange(indexOf, indexOf + stringToHighlight.length);
    }
}

/**
 * This is not currently enabled. There is a ref above called passwordCopyEnabled that
 * is set to false (set it to true to enable). I have a feeling this feature will be 
 * requested in the future, and I had a few minutes this morning to code it up and 
 * prep it for that probable request. It looked a little strange to me because of 
 * the duplicated copy icons, which is why I left it disabled. 
 * I imagine UX will have a better way to visualize this in the future.
 */
const copyPassword = () => {
    if (welcomeStore.minionCert.password) {
        navigator.clipboard.writeText(welcomeStore.minionCert.password)
        copied.value = true;
        setTimeout(() => {
            copied.value = false;
        }, 1500)
    }
}
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
    left: 0;
    right: 0;

    h1 {
        @include headline1();
        margin-bottom: 16px;
    }
}

.welcome-slide-two-wrapper.dark {

    .welcome-slide-table-body {
        background-color: #e5f4f9;
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

        .welcome-slide-table-header {
            color: var($disabled-text-on-surface);
            padding: 12px 24px;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .welcome-slide-table-body {
            padding: 12px 24px;
            position: relative;

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

.password-right {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    @include caption();
    color: var(--feather-shade-2);
    padding: var(--feather-spacing-xs) var(--feather-spacing-s);

    span :deep(.btn) {
        font-size: 15px;
        margin-left: var(--feather-spacing-s);
    }

    span :deep(.btn svg) {
        width: 25px;
        height: 25px;
    }
}

.styled-like-pre {
    background-color: transparent;
    border: none;
    resize: none;
    width: 100%;
    outline: 0;
    transition: none;

    &::selection {
        background-color: var(--feather-primary);
        color: var(--feather-state-color-on-color);
    }

    &.visible {
        opacity: 1;
    }

    @media (min-width:500px) and (max-width:547px) {
        padding-right: 50px;
    }

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
}

.info-icon {
    color: var(--feather-shade-2);
    font-size: 22px;
    top: -3px;
    position: relative;
    margin-left: 4px;
    margin-right: 8px;
}
</style>