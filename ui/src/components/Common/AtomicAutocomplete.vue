<template>
  <div
    :class="['wrapper', disabledClass]"
    ref="wrapper"
  >
    <div
      @click="wrapperClickCheck()"
      tabIndex="0"
      for="atomic-input"
      class="atomic-input-wrapper"
    >
      <div class="pre">
        <FeatherIcon :icon="Search" />
      </div>
      <div class="main">
        <label for="atomic-input">{{ inputLabel }}</label>
        <input
          id="atomic-input"
          tabIndex="0"
          :value="inputValue"
          @keydown="keyDownCheck"
          @input="(event) => textChanged((event.target as HTMLInputElement)?.value)"
          :placeholder="inputLabel"
          class="atomic-auto-input"
          :class="{ 'err-input': errMsg }"
          data-ref-id="feather-autocomplete-input"
          :disabled="disabled"
        />
      </div>
      <div class="post">
        <FeatherIcon
          :icon="KeyboardArrowDown"
          class="drop-icon"
        />
      </div>
    </div>
    <div
      label=""
      :class="['list', resultsVisible ? 'visible' : 'hidden']"
      ref="listRef"
      tabIndex="0"
    >
      <div
        class="list-item"
        tabIndex="0"
        :label="listItem"
        v-for="(listItem, index) in shortenedList"
        :key="index"
        @click="() => itemClicked(listItem, index)"
        @keydown="(d) => itemKey(d, listItem, index)"
      >
        {{ listItem }}
      </div>
      <div
        class="list-item"
        tabIndex="0"
        v-if="inputValue && !results.find((d) => d === inputValue) && allowNew"
        @click="() => itemClicked(inputValue, -1)"
        @keydown="(d) => itemKey(d, inputValue, -1)"
      >
        {{ inputValue }}
      </div>
      <FeatherSpinner v-if="loading" />
    </div>
    <div
      v-if="errMsg"
      class="err-msg"
    >
      {{ errMsg }}
    </div>
  </div>
  <div
    class="clicker"
    @click="outsideClicked"
    v-if="resultsVisible"
  ></div>
</template>
<script setup lang="ts">
import Search from '@featherds/icon/action/Search'
import KeyboardArrowDown from '@featherds/icon/navigation/ExpandMore'
import { PropType } from 'vue'

const wrapper = ref()
const listRef = ref()
const props = defineProps({
  errMsg: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
  allowNew: { type: Boolean, default: true },
  inputValue: { type: String, default: '' },
  inputLabel: { type: String, default: '' },
  itemClicked: { type: Function as PropType<(listItem: unknown, index: number) => void>, default: () => ({}) },
  loading: { type: Boolean, default: false },
  outsideClicked: { type: Function as PropType<() => void>, default: () => ({}) },
  resultsVisible: { type: Boolean, default: false },
  results: { type: Array, default: () => [] },
  textChanged: { type: Function as PropType<(text: string) => void>, default: () => ({}) },
  wrapperClicked: { type: Function as PropType<() => void>, default: () => ({}) }
})

const disabledClass = computed(() => (props.disabled ? 'disabled' : ''))
const keyDownCheck = (key: KeyboardEvent) => {
  if (key.key === 'ArrowDown') {
    listRef.value.querySelector('.list-item').focus()
  }
}

const itemKey = (keypress: KeyboardEvent, listItem: unknown, index: number) => {
  if (keypress.key === 'Enter') {
    props.itemClicked(listItem, index)
  }
  if (keypress.key === 'ArrowDown') {
    ;((keypress.target as HTMLInputElement)?.nextElementSibling as HTMLElement)?.focus()
  }
  if (keypress.key === 'ArrowUp') {
    ;((keypress.target as HTMLInputElement)?.previousElementSibling as HTMLElement)?.focus()
  }
}

const wrapperClickCheck = () => {
  if (!props.disabled) {
    props.wrapperClicked()
  }
}

const shortenedList = computed(() => (props.results?.length > 10 ? props.results?.slice(0, 10) : props.results))
</script>
<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';
@use '@featherds/styles/mixins/elevation';

.atomic-auto-input {
  min-width: 300px;
  background-color: transparent;
  border: none;
  outline: none;
  font-family: var(--feather-font-family);
  color: var(--feather-primary-text-on-surface);
  caret-color: var(--feather-primary);
  font-size: var(--feather-body-small-font-size);
  font-weight: var(--feather-body-small-font-weight);
  line-height: 1.75em;
  letter-spacing: var(--feather-body-small-letter-spacing);
  font-style: var(--feather-body-small-font-style);
  padding: 0.4rem 0.8rem;

  :focus {
    outline: none;
  }
}

.atomic-auto-input:focus {
  outline: none;
}

.list {
  position: absolute;
  background-color: var(--feather-surface);
  @include elevation.elevation(4);
  z-index: 2;
  width: calc(100% - 4px);
  left: 2px;
  outline: none;
}

.list-item {
  padding: 0.85em 0.95em;
  cursor: pointer;
}

.list-item:focus {
  outline: 2px solid var(--feather-primary);
}

.list-item:hover {
  background-image: linear-gradient(rgba(19, 23, 54, 0.08), rgba(19, 23, 54, 0.08)),
    linear-gradient(rgba(0, 0, 0, 0), rgba(0, 0, 0, 0));
}

.list.visible {
  display: block;
}

.list.hidden {
  display: none;
}

.clicker {
  z-index: 1;
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100vh;
}

.clicker.visible {
  display: block;
}

.clicker.hidden {
  display: none;
  pointer-events: none;
}

.drop-icon {
  cursor: pointer;
}

.wrapper {
  max-width: 100%;
  position: relative;

  :focus-within {
    outline: 2px solid var(--feather-primary);
  }
}

.wrapper.disabled {
  .atomic-input-wrapper {
    border-color: var(--feather-border-on-surface);
  }
  :focus-within {
    outline: none;
  }
  .feather-icon {
    color: var(--feather-disabled-text-on-surface);
  }
}

.atomic-input-wrapper {
  display: flex;
  padding: 0 0.75rem;
  align-items: center;
  border: 1px solid var(--feather-secondary-text-on-surface);
  border-radius: 4px;

  .feather-icon {
    font-size: 1.4em;
    line-height: 1.4em;
    top: 2px;
    position: relative;
    color: var(--feather-secondary-text-on-surface);
  }

  :focus {
    outline: none;
  }

  :focus-within {
    outline: none;
  }
}

.main {
  width: 100%;
  label {
    display: none;
  }
}

.err-msg {
  @include typography.caption;
  color: var(variables.$error);
  margin: var(variables.$spacing-xxs) 0 0 var(variables.$spacing-m);
}

.err-input::placeholder {
  color: var(variables.$error);
}
</style>
