<template>
  <div>
    <FeatherAutocomplete
      class="my-autocomplete"
      label="Address"
      type="single"
      :modelValue="addressModelValue"
      :minChar="1"
      noResults=""
      :loading="loading"
      :results="results"
      @search="(e) => search(e)"
      @update:modelValue="(e) => $emit('onAddressChange', e)"
    ></FeatherAutocomplete>
  </div>
</template>

<script lang="ts" setup>
import { OpenStreetMapProvider } from 'leaflet-geosearch'
import { debounce } from 'lodash'
import { IAutocompleteItemType } from '@featherds/autocomplete'

const props = defineProps<{
  addressModel: Record<string, any>
}>()

// List of alternative providers: https://smeijer.github.io/leaflet-geosearch/providers/algolia
const provider = new OpenStreetMapProvider()
const results = ref([] as IAutocompleteItemType[])
const loading = ref(false)
const addressModelValue = ref()

watch(
  props,
  async () => {
    await nextTick()
    if (props.addressModel.address) {
      addressModelValue.value = { ...props.addressModel, _text: props.addressModel?.address }
    }
  },
  { immediate: true }
)

const search = debounce(async (q: string) => {
  loading.value = true
  q = q.trim()
  if (q.length == 0) {
    return
  }
  const addresses = await provider.search({ query: q })
  results.value = addresses
    .filter((x) => matchQuery(q, x.label))
    .map((x) => ({
      _text: x.label,
      value: x
    }))

  if (results.value.length == 0 || results.value[0]._text != q) {
    results.value.unshift({ _text: q, value: { label: q, x: null, y: null } } as IAutocompleteItemType)
  }
  loading.value = false
}, 1000)

const matchQuery = function (q: string, label: string) {
  const words = q.split(/[\s.,;]/)
  const lowerCase = label.toLowerCase()
  const results = words.filter((w) => lowerCase.indexOf(w) == -1)
  return results.length == 0
}
</script>

<style lang="scss" scoped>
@use '@/styles/mediaQueriesMixins.scss';
</style>
