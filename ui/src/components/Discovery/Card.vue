<template>
  <div class="card">
    <div class="title">{{ title }}</div>
    <div class="content">
      <img
        class="img"
        alt="discovery image"
        :src="imgUrl"
      />
      <slot name="footer" />
    </div>
  </div>
</template>

<script lang="ts" setup>
defineProps<{
	title: string
	imgUrl?: string
}>()
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';

.card {
	width: 37%;
	border: 1px solid #d8d8d8;
	border-radius: 5px;
	background-color: var(variables.$surface);
	margin-right: 20px;
}

.title {
	padding: var(variables.$spacing-l) var(variables.$spacing-m);
	background-color: var(variables.$shade-4);
	font-size: 18px;
	font-weight: 600;
}

.content {
	.img {
		width: 100%;
    min-height: 170px;
	}
}
</style>
