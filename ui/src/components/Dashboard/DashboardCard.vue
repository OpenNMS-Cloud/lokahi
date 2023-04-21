<template>
  <div class="card">
    <div class="header">
      <div class="section-title">{{ texts.title }}</div>
      <div
        class="link"
        @click="router.push(redirectLink)"
      >
        {{ texts.redirectLink }}
      </div>
    </div>
    <div class="section-subtitle">{{ texts.timePeriod }}</div>
    <div class="content">
      <slot name="content"></slot>
    </div>
  </div>
</template>

<script setup lang="ts">
const router = useRouter()

defineProps<{
  texts: Record<string, string>
  redirectLink: string
}>()
</script>

<style scoped lang="scss">
@use '@featherds/styles/mixins/typography';
@use '@featherds/styles/themes/variables';
@use '@/styles/mediaQueriesMixins.scss';

.card {
  display: flex;
  flex-direction: column;
  width: 100%;
  background-color: var(variables.$surface);
  padding: var(variables.$spacing-l);
  border: 1px solid var(variables.$border-on-surface);
  @include mediaQueriesMixins.screen-md {
    width: 49.5%;
  }
  .header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    .link {
      @include typography.subtitle2();
      text-decoration: underline;
      cursor: pointer;
      color: var(variables.$primary);
    }
    .section-title {
      @include typography.headline3();
    }
    .section-subtitle {
      @include typography.caption();
    }
  }

  .content {
    margin-top: var(variables.$spacing-l);
    border: 1px solid var(variables.$border-on-surface);
    padding: var(variables.$spacing-s);
    flex: 1;
    @include mediaQueriesMixins.screen-md {
      padding: var(variables.$spacing-l);
    }
  }
}
</style>
