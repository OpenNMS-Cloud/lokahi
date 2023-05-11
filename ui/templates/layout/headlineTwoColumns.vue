<template>
  <div class="wrapper">
    <div class="header">
      <PageHeadline
        text="Headline"
        class="page-headline"
      />
      <div>buttons, links,...</div>
    </div>
    <div class="content">
      <div class="content-left">content left</div>
      <div class="content-right">content right</div>
    </div>
    <div class="footer">footer - if needed</div>
  </div>
</template>

<script lang="ts" setup></script>

<style lang="scss" scoped>
@use '@/styles/layout/headlineTwoColumns';
</style>
