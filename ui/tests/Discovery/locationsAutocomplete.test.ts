import DiscoveryLocationsAutocomplete from '@/components/Discovery/DiscoveryLocationsAutocomplete.vue'
import mount from 'tests/mountWithPiniaVillus'
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
      props: {
        type: 'multiple'
      }
    })
  })
  afterAll(() => {
    wrapper.unmount()
  })

  it('The DiscoveryLocationsAutocomplete component mounts correctly', () => {
    expect(wrapper).toBeTruthy()
  })

  it('Should find 2 results by query string', () => {
    wrapper.vm.locations = locationsMock
    wrapper.vm.search('on')
    expect(wrapper.vm.filteredLocations[0].id).toEqual(1)
    expect(wrapper.vm.filteredLocations[1].id).toEqual(3)
  })

  it('Should remove the location by id', () => {
    wrapper.vm.selectedLocation = locationsMock[0]
    wrapper.vm.removeLocation()
    expect(wrapper.vm.selectedLocation).toEqual(null)
  })
})
