import DiscoveryLocationsAutocomplete from '@/components/Discovery/DiscoveryLocationsAutocomplete.vue'
import mount from 'tests/mountWithPiniaVillus'
import { useLocationStore } from '@/store/Views/locationStore'
import { useDiscoveryStore } from '@/store/Views/discoveryStore'

let wrapper: any

const locationsMock = [
  {
    id: 1,
    location: 'Montreal'
  },
  {
    id: 2,
    location: 'Ottawa'
  },
  {
    id: 3,
    location: 'Toronto'
  },
  {
    id: 4,
    location: 'Vancouver'
  }
]

describe('Locations Autocomplete component', () => {
  beforeAll(() => {
    wrapper = mount({
      component: DiscoveryLocationsAutocomplete,
      shallow: false
    })
  })
  afterAll(() => {
    wrapper.unmount()
  })

  it('The DiscoveryLocationsAutocomplete component mounts correctly', () => {
    expect(wrapper).toBeTruthy()
  })

  it('Select location fn', () => {
    const locStore = useLocationStore()
    const discoveryStore = useDiscoveryStore()

    locStore.locationsList = locationsMock

    wrapper.vm.selectLocation({ name: 'Ottawa' })

    expect(discoveryStore.selectedLocation?.id).toBe(2)
  })
})
