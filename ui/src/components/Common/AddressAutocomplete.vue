<template>
  <div>
    <FeatherAutocomplete
      class="my-autocomplete"
      label="Address"
      type="single"
      v-model="addressModalRef"
      minChar="1"
      noResults=""
      :loading="loading"
      :results="results"
      @search="(e) => search(e)"
      @update:model-value="(e) => onAddressModelUpdate(e)"
    ></FeatherAutocomplete>
  </div>
</template>

<script lang="ts" setup>
import { OpenStreetMapProvider } from 'leaflet-geosearch'
import { debounce } from 'lodash'
import { IAutocompleteItemType } from '@featherds/autocomplete'
import { PropType } from 'vue'


const props = defineProps({
  addressModel: {
    required: true,
    type: Object
  },
  onAddressModelUpdate: {
    type: Function as PropType<(e: any) => void>,
    default: () => ({})
  }
})

// List of alternative providers: https://smeijer.github.io/leaflet-geosearch/providers/algolia
const provider = new OpenStreetMapProvider()
const addressModalRef = computed(() => props.addressModel)
const results = ref([] as IAutocompleteItemType[])
const loading = ref(false)

console.log(addressModalRef)

const search = debounce(async (q: string) => {
  loading.value = true
  q = q.replace(/\s+$/g, '')
  if(q.length == 0) {
    return
  }
  const addresses = await provider.search({ query: q.replace(/\s+$/g, '') })
  results.value = addresses
    .filter((x) => matchQuery(q, x.label))
    .map((x) => ({
      _text: x.label,
      value: x
    }))

  if(results.value.length == 0 || results.value[0]._text != q ) {
    results.value.unshift({_text: q, value: {label: q, x: null, y: null}} as IAutocompleteItemType)
  }
  loading.value = false
}, 1000)

const matchQuery = function (q: string, label: string) {
  const words = q.split(/[\s.,;]/)
  const lowerCase = label.toLowerCase()
  const results = words.filter((w) => lowerCase.indexOf(w) == -1)
  return (results.length == 0)
}

</script>

<style lang="scss" scoped>
@use '@/styles/mediaQueriesMixins.scss';
</style>
