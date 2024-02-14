<template>
  <div>
    <section class="node-component-header">
      <h3 data-test="heading" class="node-label">Monitoring Policies</h3>
    </section>
    <section v-if="store.monitoringPolicies.length > 0" class="node-component-content">
      <ul>
        <li v-for="policy in store.monitoringPolicies">
          <p @click="handleRoute(policy)" href="">{{ policy.name }}</p>
        </li>
      </ul>
    </section>
    <section class="node-component-content" v-if="store.monitoringPolicies.length === 0">
    No Discovery found.
    </section>
  </div> 
</template>

<script lang="ts" setup>
import { useMonitoringPoliciesStore } from '@/store/Views/monitoringPoliciesStore'
import { NewOrUpdatedDiscovery } from '@/types/discovery';
const store = useMonitoringPoliciesStore()
const router = useRouter()
onMounted(() => store.getMonitoringPolicies())
onUnmounted(() => store.$reset())
const handleRoute = (policy: NewOrUpdatedDiscovery ) => {
  router.push({
    path: `/monitoring-policies-selected/${policy.id}`
  })
}
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
  > ul {
    > li {
      padding: 5px 0px;
      p {
        overflow: hidden;
        color: var(--open-source-dark-blue-900-secondary, #273180);
        font-kerning: none;
        font-feature-settings: 'calt' off;
        text-overflow: ellipsis;
        white-space: nowrap;

        /* Open Source/Caption Text */
        font-family: Inter;
        font-size: 12px;
        font-style: normal;
        font-weight: 400;
        line-height: 16px; /* 133.333% */

        cursor: pointer;
      }
    }
  }
}

</style>

