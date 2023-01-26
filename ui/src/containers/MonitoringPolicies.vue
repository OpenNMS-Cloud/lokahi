<template>
  <div class="container">
    <PageHeader heading="Monitoring Policies" />
    <div class="title-rule">Policies</div>
    <FeatherTabContainer class="tab-container">
      <template v-slot:tabs>
        <FeatherTab>Create New Policy</FeatherTab>
        <FeatherTab>Existing Policies</FeatherTab>
      </template>
      <FeatherTabPanel class="panel"><AddNewPolicy /></FeatherTabPanel>
      <FeatherTabPanel class="panel"> Content for Policy List </FeatherTabPanel>
    </FeatherTabContainer>

    <div class="title-rule">Rules</div>
    <FeatherTabContainer class="tab-container">
      <template v-slot:tabs>
        <FeatherTab>Create New Rule</FeatherTab>
        <FeatherTab>Existing Rules</FeatherTab>
      </template>
      <FeatherTabPanel class="panel"><AddNewRule /><DetectionMethod /><AlertConditions /></FeatherTabPanel>
      <FeatherTabPanel class="panel"> Content for Existing Rule List </FeatherTabPanel>
    </FeatherTabContainer>
  </div>
</template>

<script setup lang="ts">
import { FeatherTab, FeatherTabContainer, FeatherTabPanel } from '@featherds/tabs'
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';

.container {
  display: flex;
  margin: 0 1rem;
  flex-direction: column;

  .panel {
    background-color: var(variables.$surface);
    padding: var(variables.$spacing-l);
    margin-top: var(variables.$spacing-m);
    border: 1px var(variables.$shade-4) solid;
  }
  .tab-container {
    margin-bottom: var(variables.$spacing-xxl);
  }
  .title-rule {
    @include typography.headline3;
    margin-bottom: var(variables.$spacing-s);
  }
}
</style>
