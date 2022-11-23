<template>
  <PageHeader :heading="heading" class="mx-l" />
  <!-- tabs anchor-->
  <!-- expandable filter: search, select, sorting -->
  <Filter @selected-item="onSelectedItem"/>
  <!-- card: title, metric chip list, text anchor list, icon action list -->

  <!-- This ctrl should later be moved into Filter. -->
  <TagManagerCtrl />
  <TagManager />
</template>

<script lang="ts" setup>
import { ISelectItemType } from '@featherds/select/src/components/types'

const heading = 'Network Inventory'

const onSelectedItem = (selectedItem: ISelectItemType) => {
  console.log('onSelectedItem',selectedItem)
}
</script>

<style lang="scss" scoped>
</style>