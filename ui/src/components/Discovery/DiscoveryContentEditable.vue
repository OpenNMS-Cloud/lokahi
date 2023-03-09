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
  <div class="content-editable-wrapper">
    <div class="label">
      <label for="contentEditable">{{ props.label }}</label>
      <FeatherTooltip
        v-if="props.tooltipText"
        :title="props.tooltipText"
        v-slot="{ attrs, on }"
      >
        <FeatherButton
          v-bind="attrs"
          v-on="on"
          icon="info"
          class="icon-help"
          ><FeatherIcon :icon="Help"> </FeatherIcon
        ></FeatherButton>
      </FeatherTooltip>
    </div>
    <div
      v-html="htmlString"
      @keyup="contentChange"
      ref="contentEditableRef"
      contenteditable="true"
      id="contentEditable"
      class="content-editable"
    />
    <span
      v-if="props.regexDelim && isContentNotEmpty"
      @click="validateAndFormat"
      class="validate-format"
      ><Icon :icon="checkCircleIcon"
    /></span>
  </div>
</template>

<script lang="ts" setup>
import ipRegex from 'ip-regex'
import CheckCircleIcon from '@featherds/icon/action/CheckCircle'
import { IIcon } from '@/types'
import { ContentEditableType } from '@/components/Discovery/discovery.constants'
import { PropType } from 'vue'
import { fncArgVoid } from '@/types'
import Help from '@featherds/icon/action/Help'

const emit = defineEmits(['is-content-invalid', 'content-formatted'])

const props = defineProps({
  contentType: {
    type: Number as PropType<ContentEditableType>,
    required: true
  },
  regexDelim: {
    type: String,
    default: ''
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
    type: String
  }
})

let isContentInvalid = true
const isContentNotEmpty = ref(false)
const contentEditableRef = ref()
const htmlString = ref(props.content || props.defaultContent) // to render string as html

watch(props, () => {
  htmlString.value = props.content
})

const contentChange = () => {
  isContentNotEmpty.value = contentEditableRef.value.textContent.length as boolean
}

const validateAndFormat: fncArgVoid = () => {
  isContentInvalid = validateContent()

  const highlightedString = highlightInvalid()
  if (highlightedString.length) htmlString.value = highlightedString

  emit('is-content-invalid', isContentInvalid)

  if (!isContentInvalid) emit('content-formatted', splitContent(contentEditableRef.value.textContent))
}

const splitContent = (str: string): (string | number)[] | null => {
  if (!str || str.length < 2) return null

  const regexDelim = new RegExp(props.regexDelim)
  let contentEditableStrings = str.split(regexDelim)

  if (props.contentType === ContentEditableType.UDPPort) {
    return contentEditableStrings.map((p) => parseInt(p))
  }
  return contentEditableStrings
}

const validateContent = () => {
  const regexDelim = new RegExp(props.regexDelim)
  const contentEditableStrings = contentEditableRef.value.textContent.split(regexDelim)
  let isInvalid = false

  switch (props.contentType) {
    case ContentEditableType.IP:
      // isInvalid = contentEditableStrings.some((str: string) => !ipRegex({ exact: true }).test(str))
      break
    case ContentEditableType.CommunityString:
      break
    case ContentEditableType.UDPPort:
      break
    default:
  }
  return isInvalid
}

const highlightInvalid = () => {
  const regexDelim = new RegExp(props.regexDelim)
  const contentEditableStrings = contentEditableRef.value.textContent.split(regexDelim)
  let highlightInvalidString = ''
  let errorStr = '<span style="color: #ff555e">{{}}</span>'

  switch (props.contentType) {
    case ContentEditableType.IP:
      /* highlightInvalidString = contentEditableStrings
        .map((str: string) => {
          if (ipRegex({ exact: true }).test(str)) return str

          return errorStr.replace('{{}}', str)
        })
        .join(';') */
      break
    case ContentEditableType.CommunityString:
      break
    case ContentEditableType.UDPPort:
      break
    default:
  }

  return highlightInvalidString
}

const reset: fncArgVoid = () => {
  contentEditableRef.value.textContent = props.defaultContent
}

const checkCircleIcon: IIcon = {
  image: markRaw(CheckCircleIcon),
  tooltip: 'Validate'
}
const iconHelp: IIcon = {
  image: markRaw(Help),
  tooltip: props.tooltipText,
  size: 1.2
}

defineExpose({
  validateAndFormat,
  reset
})
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@/styles/mediaQueriesMixins.scss';
@use '@/styles/vars.scss';

.content-editable-wrapper {
  // TODO: mimic FeatherDS input component; label inside input box and animate it to be on top on the box when focus
  position: relative;
  > .label {
    display: flex;
    justify-content: space-between;
    align-items: center;
    > .icon-help {
      height: 20px;
      color: var(variables.$secondary-variant);
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
</style>
