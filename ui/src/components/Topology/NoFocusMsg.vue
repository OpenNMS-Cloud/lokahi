<template>
  <div class="no-focus-container">
    <div class="title">No Focus Defined</div>

    <div class="content">
      <p>This means</p>
      <ul>
        <li>the last vertex was removed from focus or</li>
        <li>no default focus is available.</li>
      </ul>

      <p>To add a vertex to focus</p>
      <ul>
        <li>manually add a vertex to focus via the search box</li>
        <li>use the default focus</li>
      </ul>

      <FeatherButton
        @click="useDefaultFocus"
        primary
        class="btn"
        :disabled="!defaultObjects.length"
      >
        <span>{{ defaultObjects.length ? 'Use Default Focus' : 'No Nodes Available' }}</span>
      </FeatherButton>
    </div>
  </div>
</template>

<script
  setup
  lang="ts"
>
import { FeatherButton } from '@featherds/button'
import { useTopologyStore } from '@/store/Views/topologyStore'
import { useTopologyFocus } from './topology.composables'

const topologyStore = useTopologyStore()
const { useDefaultFocus } = useTopologyFocus()
const defaultObjects = computed(() => topologyStore.defaultObjects)
</script>

<style
  scoped
  lang="scss"
>
@use "@featherds/styles/themes/variables";
@use "@featherds/styles/mixins/elevation";
@use "@featherds/styles/mixins/typography";

.no-focus-container {
  @include typography.body-small;
  @include elevation.elevation(1);
  background: var(variables.$surface);
  color: var(variables.$primary-text-on-surface);
  height: 400px;
  width: 600px;
  position: absolute;
  margin-left: auto;
  margin-right: auto;
  left: 0;
  right: 0;
  z-index: 99999;
  top: 7rem;
  .title {
    @include typography.headline3;
    height: auto;
    background: var(variables.$secondary);
    color: var(variables.$primary-text-on-color);
    padding: 15px;
  }

  .content {
    margin-left: 2rem;
    margin-top: 20px;

    .btn {
      margin-left: 20px;
      margin-top: 20px;
    }
  }
}
</style>
