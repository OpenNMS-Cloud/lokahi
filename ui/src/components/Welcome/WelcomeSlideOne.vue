<template>
    <div :class="['welcome-slide-one-wrapper', visible ? 'visible' : 'hidden',
    ]">
        <div class="welcome-text">
            <h1 data-test="welcome-slide-one-title">Welcome to <br />OpenNMS&nbsp;Cloud!</h1>
            <p class="margin-bottom">Our mission is to make monitoring just&nbsp;happen.</p>
            <p class="margin-bottom">To start monitoring, your network must include at least one OpenNMS Minion for
                distributed network monitoring. We'll walk you through that setup, which takes only a
                few&nbsp;minutes.</p>
        </div>
        <CollapsingCard title="Requirements: Before You Begin" data-test="welcome-slide-one-toggler"
            :open="welcomeStore.slideOneCollapseVisible" :headerClicked="welcomeStore.toggleSlideOneCollapse">
            <template #icon>
                <div>
                    <FeatherIcon :icon="Lightbulb" title="Lightbulb" :class="isDark ? 'icon-dark' : 'icon-light'" />
                </div>
            </template>
            <template #body>
                <div>
                    <h4>Minimum System Requirements:</h4>
                </div>
                <table aria-describedby="Minimum System Requirements" data-test="welcome-system-requirements-table">
                    <thead>
                        <tr>
                            <th>Requiment Type</th>
                            <th>Requiment Specifications</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td>CPU</td>
                            <td>3GHz quad core x86_64 and above</td>
                        </tr>
                        <tr>
                            <td>
                                RAM
                            </td>
                            <td>
                                8GB (physical) and above
                            </td>
                        </tr>
                        <tr>
                            <td>
                                Storage (disk space)
                            </td>
                            <td>
                                100GB with SSD and above
                            </td>
                        </tr>
                    </tbody>
                </table>
                <h4>Make sure you have the following:</h4>
                <ul>
                    <li>Docker environment with Docker Compose</li>
                    <li>Network Time Protocol (NTP) installed and configured</li>
                </ul>
                <a href="https://docs.docker.com/desktop/install/mac-install/" target="_blank" rel="noopener noreferrer">How
                    to set up
                    Docker
                    <FeatherIcon :icon="ChevronRight" />
                </a>
            </template>
        </CollapsingCard>
        <FeatherButton primary @click="welcomeStore.nextSlide" data-test="welcome-slide-one-setup-button">Start Setup
        </FeatherButton>
    </div>
</template>
<script lang="ts" setup>
import CollapsingCard from '../Common/CollapsingCard.vue'
import Lightbulb from '../Common/LightbulbIcon.vue'
import ChevronRight from '@featherds/icon/navigation/ChevronRight'
import { useWelcomeStore } from '@/store/Views/welcomeStore'
import useTheme from '@/composables/useTheme';
const welcomeStore = useWelcomeStore()
const { isDark } = useTheme();
defineProps({
    visible: { type: Boolean, default: false }
})
</script>
<style lang="scss" scoped>
@import '@featherds/styles/themes/variables';
@import '@featherds/styles/mixins/typography';
@import "@featherds/table/scss/table";

h1 {
    @include headline1();
    font-size: 2.25rem;
    font-weight: 700;
    line-height: 2.5rem;
    margin-bottom: var($spacing-xl);
    color: var($primary-text-on-surface);
}

h4 {
    @include subtitle2();
    color: var($secondary-text-on-surface);
    margin-bottom: 16px;
}

.margin-bottom {
    margin-bottom: 16px;
}

table {
    @include table();
    color: var($secondary-text-on-surface);
    border: 1px solid var($border-light-on-surface);
    border-radius: 3px;
    border-bottom: 0;
    margin-bottom: 24px;

    thead {
        display: none;
    }
}

ul {
    margin-bottom: 24px;
    padding-inline-start: 16px;
}

ul li {
    @include caption();
    color: var($secondary-text-on-surface);
    list-style-type: disc;
}

.welcome-text {
    margin-top: 24px;
    margin-bottom: 24px;
}

.visible {
    opacity: 1;
    pointer-events: all;
    transform: translateY(0px);
    transition: opacity 0.1s ease-in-out 0.2s, transform 0.2s ease-out 0.2s;
}

.hidden {
    opacity: 0;
    pointer-events: none;
    transform: translateY(20px);
    transition: opacity 0.2s ease-in-out 0s, transform 0.3s ease-in 0s;
}

.welcome-slide-one-wrapper {
    position: absolute;
    padding-bottom: 40px;

    a {
        color: var($clickable-normal);
    }
}

.icon-dark {
    color: var($primary-text-on-surface);
}

.icon-light {
    color: blue;
}
</style>