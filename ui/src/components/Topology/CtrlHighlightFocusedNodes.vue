<template>
  <FeatherButton
    v-if="!focusIsHighlighted"
    icon="Highlight focused nodes."
    @click="highlightFocusedObjects"
  >
    <FeatherIcon :icon="View"></FeatherIcon>
  </FeatherButton>

  <FeatherButton
    v-else
    icon="Remove highlight from focused nodes."
    @click="removeHighlightFromFocusedNodes"
  >
    <FeatherIcon :icon="Hide"></FeatherIcon>
  </FeatherButton>
</template>

<script setup lang="ts">
import { FeatherIcon } from '@featherds/icon'
import { FeatherButton } from '@featherds/button'
import View from '@featherds/icon/action/View'
import Hide from '@featherds/icon/action/Hide'
import { useTopologyStore } from '@/store/Views/topologyStore'

const topologyStore = useTopologyStore()
const focusIsHighlighted = computed<boolean>(() => topologyStore.highlightFocusedObjects)

const highlightFocusedObjects = () => topologyStore.highlightFocusedObjects = true
const removeHighlightFromFocusedNodes = () => topologyStore.highlightFocusedObjects = false
</script>
