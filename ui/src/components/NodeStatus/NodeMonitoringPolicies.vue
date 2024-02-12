<template>
  <div>
    <section class="node-component-header">
      <h3 data-test="heading" class="node-label">Monitoring Policies</h3>
    </section>
    <section class="node-component-content">
      <div>MP-001-snmp/icmp</div>
      <div>MP-002-azure-policy-1</div>
    </section>
  </div> 
</template>

<script lang="ts" setup>


</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@/styles/vars';
@use '@/styles/mediaQueriesMixins';
@use '@featherds/styles/mixins/typography';

.node-component-header {
  margin-bottom: var(variables.$spacing-s);
  display: flex;
  flex-direction: row;
  gap: 0.5rem;
  align-items: center;
  justify-content: space-between;
}

.node-component-label {
  margin: 0;
  line-height: 20px;
  letter-spacing: 0.28px;
}
 
.node-component-content {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  gap: 2rem;
}

</style>

