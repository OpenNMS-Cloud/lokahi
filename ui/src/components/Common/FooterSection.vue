<template>
  <div class="footer-section-wrapper">
    <hr />
    <div class="actions">
      <slot name="buttons" />
    </div>
  </div>
</template>

<script setup lang="ts"></script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';

.footer-section-wrapper {
  margin-top: var(variables.$spacing-l);
}

.actions {
  display: flex;
  justify-content: flex-end;
  margin-top: var(variables.$spacing-l);
}
</style>
