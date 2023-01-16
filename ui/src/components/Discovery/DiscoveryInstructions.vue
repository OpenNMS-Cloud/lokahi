<template>
  <div class="container">
    <div class="title">{{DiscoveryType[props.tool]}} {{Texts.Instructions.title}}</div>
	<p>{{Texts.Instructions.subtitle}}</p>
	<div class="locations">
		<DiscoveryLocations />
	</div>
  </div>
</template>

<script lang="ts" setup>
import Texts from '@/components/Discovery/discovery.text'
import { DiscoveryType } from '@/components/Discovery/discovery.constants'

const props = defineProps<{
	tool: number
}>()
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
.container {
	min-width: 500px;
	padding-top:20px;

	.title {
		font-size: 18px;
		font-weight: 600;
	}

	.locations {
		margin-top:20px
	}

	.feather-input-sub-text {
		display: none !important;
	}
}
</style>
