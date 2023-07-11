<!-- 
  This component contains:
    - a label.
    - an input box, where text can be typed and/or pasted in.
    - a clickable icon to validae the text entered.

  A required type [IP, Community, Port] prop.
  An optional regex expression for validation.

  Note:
    - the clickable icon is shown if:
        - regexDelim prop is not empty and
        - the input box is not empty
 -->
<template>
  <div>
    <div class="content-editable-wrapper">
      <div class="label">
        <label for="contentEditable">{{ props.label }}</label>
        <FeatherPopover
          :pointer-alignment="PointerAlignment.center"
          :placement="PopoverPlacement.top"
          v-if="props.tooltipText"
        >
          <template #default>
            <div>
              <h5>{{ props.tooltipText.title }}</h5>
              <div v-html="props.tooltipText.description"></div>
            </div>
          </template>
          <template #trigger="{ attrs, on }">
            <FeatherIcon
              class="icon-help"
              :icon="Help"
              v-bind="attrs"
              v-on="on"
            >
            </FeatherIcon>
          </template>
        </FeatherPopover>
      </div>
      <div
        v-html="htmlString"
        @keyup="contentChange"
        ref="contentEditableRef"
        :contenteditable="!disabled"
        :id="'contentEditable_' + id"
        class="content-editable"
        :class="{ 'disabled-editable-content': disabled }"
        @blur="validateContent"
      />
      <span
        v-if="props.regexExpression && isContentNotEmpty"
        @click="validateContent"
        class="validate-format"
        ><Icon :icon="checkCircleIcon"
      /></span>
    </div>
    <div
      v-if="isContentInvalid"
      class="errorMsgBox"
    >
      {{ errorMsg }}
    </div>
  </div>
</template>

<script lang="ts" setup>
import CheckCircleIcon from '@featherds/icon/action/CheckCircle'
import { IIcon } from '@/types'
import { ContentEditableType } from '@/components/Discovery/discovery.constants'
import { PointerAlignment, PopoverPlacement } from '@featherds/popover'
import { PropType } from 'vue'
import { fncArgVoid } from '@/types'
import discoveryText from '@/components/Discovery/discovery.text'
import Help from '@featherds/icon/action/Help'
import { isEmpty } from 'lodash'

const emit = defineEmits(['content-formatted'])
const props = defineProps({
  contentType: {
    type: Number as PropType<ContentEditableType>,
    required: true
  },
  regexDelim: {
    type: String,
    default: ''
  },
  regexExpression: {
    type: Array<RegExp>
  },
  label: {
    type: String,
    default: ''
  },
  defaultContent: {
    type: [String, Number],
    default: ''
  },
  content: {
    type: [String, Number],
    default: ''
  },
  tooltipText: {
    type: Object
  },
  isRequired: {
    type: Boolean,
    required: false
  },
  id: {
    type: Number,
    required: true
  },
  disabled: {
    type: Boolean,
    required: false
  }
})

const isContentNotEmpty = ref(false)
const contentEditableRef = ref()
const isContentInvalid = ref(false)
const errorMsg = ref('')
const htmlString = ref(props.content || props.defaultContent) // to render string as html

watch(props, () => {
  htmlString.value = props.content
})

const contentChange = () => {
  isContentNotEmpty.value = contentEditableRef.value.textContent.length as boolean
}

const splitContent = (str: string[]): (string | number)[] | null => {
  if (props.contentType === ContentEditableType.UDPPort) {
    return str.map((p) => parseInt(p))
  }
  return str
}

const formatContent = (contentEditableStrings: string[], regexp: RegExp[]) => {
  const parent = document.getElementById('contentEditable_' + props.id)
  if (!parent) return
  contentEditableStrings
    .map((str: string) => {
      const node = document.createElement('div')
      node.textContent = str
      if (!regexp.map((t: RegExp) => t.test(str)).includes(true)) {
        isContentInvalid.value = true
        node.setAttribute('style', 'color: #ff555e')
      }
      parent.appendChild(node)
    })
    .join(';')
}

const extractTextFromNodes = (ceNode: HTMLElement) => {
  const rows = []
  let textElement = ceNode.innerHTML
  if (ceNode.innerHTML.indexOf('<') !== -1) {
    textElement = ceNode.innerHTML.substring(0, ceNode.innerHTML.indexOf('<'))
  }
  rows.push(textElement)

  const children = ceNode.children as HTMLCollection
  for (let child of children) {
    rows.push((child as HTMLElement).innerText)
  }
  return rows.filter((s) => !isEmpty(s)).join(',')
}

const validateContent = () => {
  const regexDelim = new RegExp(props.regexDelim)
  const ceNode = document.getElementById('contentEditable_' + props.id)

  if (!ceNode) return
  const textFromNodes = extractTextFromNodes(ceNode)
  const contentEditableStrings = textFromNodes.replace(/\s/g, '').split(regexDelim)
  const regexp = props.regexExpression?.map((r) => new RegExp(r))
  isContentInvalid.value = false

  if (regexp) {
    ceNode.innerHTML = ''
    formatContent(contentEditableStrings, regexp)
  }

  if (!isContentInvalid.value) {
    emit('content-formatted', splitContent(contentEditableStrings))
  } else {
    showErrorMsg()
  }
  return isContentInvalid.value
}

const showErrorMsg = () => {
  if (isContentInvalid.value) {
    if (!isContentNotEmpty.value && props.isRequired) {
      errorMsg.value = discoveryText.ContentEditable.errorRequired
    } else {
      errorMsg.value = discoveryText.ContentEditable.errorInvalidValue
    }
  }
}

const reset: fncArgVoid = () => {
  contentEditableRef.value.textContent = props.defaultContent
  errorMsg.value = ''
  isContentInvalid.value = false
}

const checkCircleIcon: IIcon = {
  image: markRaw(CheckCircleIcon),
  tooltip: 'Validate'
}

defineExpose({
  validateContent,
  reset
})
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@/styles/mediaQueriesMixins.scss';
@use '@/styles/vars.scss';
@use '@featherds/styles/mixins/typography';

.content-editable-wrapper {
  // TODO: mimic FeatherDS input component; label inside input box and animate it to be on top on the box when focus
  position: relative;
  > .label {
    display: flex;
    justify-content: space-between;
    align-items: center;
    > .icon-help {
      height: 20px;
      width: 20px;
      min-width: 20px;
      border: none;
      padding: 0;
      > svg {
        font-size: 1.2rem;
      }
    }
  }
  > .content-editable {
    border: 1px solid var(variables.$secondary-text-on-surface);
    border-radius: vars.$border-radius-xs;
    padding: var(variables.$spacing-xs) var(variables.$spacing-m) var(variables.$spacing-xl);
    height: 200px;
    overflow: scroll;
    outline: none;
    > .error {
      color: red;
    }
  }

  > .disabled-editable-content {
    background: var(variables.$shade-4);
  }
  > .validate-format {
    position: absolute;
    right: 5px;
    bottom: 0;
    :deep(> .feather-icon) {
      width: 1.5rem;
      height: 1.5rem;
      outline: none;
      color: var(variables.$success);
      &:hover {
        cursor: pointer;
      }
    }
  }
}

.errorMsgBox {
  height: 24px;
  @include typography.caption;
  color: var(variables.$error);
  padding-top: var(variables.$spacing-xs);
}
</style>
