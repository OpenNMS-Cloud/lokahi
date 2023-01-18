<template>
</template>

<script setup lang="ts">
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';

.container {
	display: flex;
	flex-direction: column;
	margin: 0 1rem;

	.discovery-cards {
		display: flex;
	}

	.footer {
		padding: var(variables.$spacing-m);
		padding-top: var(variables.$spacing-s);

		> p {
			margin-bottom: 10px;
		}
	}

  .passive-tools {
    display: flex;
    flex-direction: column;
    margin-top: 18px;
  }
}
</style>
