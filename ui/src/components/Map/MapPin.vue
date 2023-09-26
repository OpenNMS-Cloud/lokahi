<template>
  <div
    class="pin"
    id="mapPin"
  >
    <div
      class="severity-circle"
      :class="`${severity.toLowerCase()}`"
    ></div>
    <div
      class="nodes"
      v-if="showNumber"
    >
      {{ numberOfNodes }}
    </div>
  </div>
</template>

<script setup lang="ts">
const props = defineProps({
  severity: { type: String, default: 'indeterminate' },
  numberOfNodes: { type: Number, default: 0 }
})

// icon point positions
const showNumber = computed(() => props.numberOfNodes > 1)
const pinTop = computed(() => (showNumber.value ? '19px' : '18px'))
// margin between severity color and text
const textMargin = computed(() => (showNumber.value ? '5px' : '0px'))
</script>

<style lang="scss" scoped>
@use '@/styles/vars.scss';
@use '@/styles/_severities.scss';
@use '@featherds/styles/mixins/elevation';
@use '@featherds/styles/themes/variables';
.pin {
  filter: drop-shadow(0px 3px 3px var(variables.$shade-2));
  display: flex;
  background: var(variables.$surface);
  width: fit-content;
  padding: 4px;
  border-radius: 16px;
  border: 1px solid var(variables.$border-on-surface);

  .severity-circle {
    width: 20px;
    height: 20px;
    border-radius: vars.$border-radius-round;
    padding: 8px;
    z-index: 1;
  }
  .nodes {
    align-self: center;
    margin-left: v-bind(textMargin);
    margin-right: v-bind(textMargin);
  }
}

.pin:after {
  content: '';
  position: absolute;
  transform: rotate(-45deg);
  margin-top: v-bind(pinTop);
  margin-left: calc(50% - 9px);
  background: var(variables.$surface);
  border: 1px solid;
  border-color: transparent transparent var(variables.$border-on-surface) var(variables.$border-on-surface);
  width: 10px;
  height: 10px;
}
</style>
