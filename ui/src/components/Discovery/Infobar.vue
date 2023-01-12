;
<template>
  <div class="infobar">
    <div>{{Texts.Infobar.title}}</div>
    <div class="list">
      <div class="item">
        <FeatherIcon
          class="icon"
          :icon="CheckCircle"
        />{{Texts.Infobar.text1}}
      </div>
      <div class="item">
        <FeatherIcon
          class="icon"
          :icon="CheckCircle"
        />{{Texts.Infobar.text2}}
      </div>
      <div class="item">
        <FeatherIcon
          class="icon"
          :icon="CheckCircle"
        />
        <div>
         {{Texts.Infobar.text3}}
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
import Texts from '@/components/Discovery/discovery.text'
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
