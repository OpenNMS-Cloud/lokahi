;
<template>
  <div class="infobar">
    <div>With Zero Touch Provisioning, BTO discovers your network actively and passively:</div>
    <div class="list">
      <div class="item">
        <FeatherIcon
          class="icon"
          :icon="CheckCircle"
        />We can queue nodes for discovery
      </div>
      <div class="item">
        <FeatherIcon
          class="icon"
          :icon="CheckCircle"
        />You can configure your nodes to deliver notifications to BTO
      </div>
      <div class="item">
        <FeatherIcon
          class="icon"
          :icon="CheckCircle"
        />
        <div>
          You can customize how BTO discovers your network using
          <a
            class="link"
            href="my-discovery"
            >My Discovery</a
          >
        </div>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import CheckCircle from '@featherds/icon/action/CheckCircle'
</script>
<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
.infobar {
    background-color: var(--feather-secondary);
    padding: var(variables.$spacing-l);
    color: var(variables.$surface);
    width: 75.2%;
    margin-bottom: var(variables.$spacing-l);

    > .list {
        display:flex;
        justify-content: space-between;
        margin-top: 15px;

        > .item {
            width: 90%;
            font-size: 16px;
            font-weight: 600;
            display: flex;
            align-items: center;

            .icon {
                font-size: 40px;
                color: var(variables.$success);
                margin-right: 8px;
            }

            .link {
                color: var(variables.$primary-variant);
            }
        }
    }
}
</style>
