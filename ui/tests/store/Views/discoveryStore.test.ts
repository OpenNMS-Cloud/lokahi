import { createTestingPinia } from '@pinia/testing'
import { setActiveClient, useClient } from 'villus'
import { useDiscoveryStore } from '@/store/Views/discoveryStore'


describe('Discovery Store', () => {
  beforeEach(() => {
    createTestingPinia({ stubActions: false })
    setActiveClient(useClient({ url: 'http://test/graphql' })) // Create and set a client
  })
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('Test for init', () => {
    const discoveryStore = useDiscoveryStore()
    vi.spyOn(discoveryStore, 'init')
    discoveryStore.init()
    expect(discoveryStore.init).toBeCalled()
  })

  it('Test for activateHelp', () => {
    const discoveryStore = useDiscoveryStore()
    vi.spyOn(discoveryStore, 'activateHelp')
    discoveryStore.activateHelp('testing')
    expect(discoveryStore.activateHelp).toBeCalledWith('testing')
    expect(discoveryStore.instructionsType).toBe('testing')
    expect(discoveryStore.helpActive).toBe(true)

  })


  it('Test for disableHelp', () => {
    const discoveryStore = useDiscoveryStore()
    vi.spyOn(discoveryStore, 'disableHelp')
    discoveryStore.disableHelp()
    expect(discoveryStore.disableHelp).toBeCalled()
    expect(discoveryStore.helpActive).toBe(false)

  })


  it('Test for clearLocationAuto', () => {
    const discoveryStore = useDiscoveryStore()
    vi.spyOn(discoveryStore, 'clearLocationAuto')
    discoveryStore.clearLocationAuto()
    expect(discoveryStore.clearLocationAuto).toBeCalled()
    expect(discoveryStore.foundLocations).toStrictEqual([])
    expect(discoveryStore.locationSearch).toBe('')
  })

  it('Test for clearTagAuto', () => {
    const discoveryStore = useDiscoveryStore()
    vi.spyOn(discoveryStore, 'clearTagAuto')
    discoveryStore.clearTagAuto()
    expect(discoveryStore.clearTagAuto).toBeCalled()
    expect(discoveryStore.foundTags).toStrictEqual([])
    expect(discoveryStore.tagSearch).toBe('')
  })

  it('Test for backToDiscovery', () => {
    const discoveryStore = useDiscoveryStore()
    vi.spyOn(discoveryStore, 'backToDiscovery')
    discoveryStore.backToDiscovery()
    expect(discoveryStore.backToDiscovery).toBeCalled()
    expect(discoveryStore.newDiscoveryModalActive).toBe(false)
    expect(discoveryStore.discoveryFormActive).toBe(false)
    expect(discoveryStore.validateOnKeyUp).toBe(false)
    expect(discoveryStore.validationErrors).toStrictEqual({})
    expect(discoveryStore.selectedDiscovery).toStrictEqual({})
  })

  it('Test for closeNewModal', () => {
    const discoveryStore = useDiscoveryStore()
    vi.spyOn(discoveryStore, 'closeNewModal')
    discoveryStore.closeNewModal()
    expect(discoveryStore.closeNewModal).toBeCalled()
    expect(discoveryStore.newDiscoveryModalActive).toBe(false)
  })

  it('Test for backToTypePage', () => {
    const discoveryStore = useDiscoveryStore()
    vi.spyOn(discoveryStore, 'backToTypePage')
    discoveryStore.backToTypePage()
    expect(discoveryStore.backToTypePage).toBeCalled()
    expect(discoveryStore.discoveryFormActive).toBe(false)
    expect(discoveryStore.validationErrors).toStrictEqual({})
    expect(discoveryStore.discoveryTypePageActive).toBe(true)
  })

  it('Test for startNewDiscovery', () => {
    const discoveryStore = useDiscoveryStore()
    vi.spyOn(discoveryStore, 'startNewDiscovery')
    discoveryStore.startNewDiscovery()
    expect(discoveryStore.startNewDiscovery).toBeCalled()
    if (discoveryStore.soloTypeEditor) {

      expect(discoveryStore.discoveryTypePageActive).toBe(true)
      expect(discoveryStore.discoveryFormActive).toBe(false)

    } else {
      expect(discoveryStore.discoveryTypePageActive).toBe(true)
    }
  })

  it('Test for setupDefaultDiscovery', () => {
    const discoveryStore = useDiscoveryStore()
    vi.spyOn(discoveryStore, 'setupDefaultDiscovery')
    discoveryStore.setupDefaultDiscovery()
    expect(discoveryStore.setupDefaultDiscovery).toBeCalled()
    expect(discoveryStore.selectedDiscovery.tags).toStrictEqual([{name: 'default'}])
  })

  it('Test for setupDefaultDiscovery', () => {
    const discoveryStore = useDiscoveryStore()
    vi.spyOn(discoveryStore, 'setupDefaultDiscovery')
    discoveryStore.setupDefaultDiscovery()
    expect(discoveryStore.setupDefaultDiscovery).toBeCalled()
  })

  it('Test for applyDefaultLocation', () => {
    const discoveryStore = useDiscoveryStore()
    vi.spyOn(discoveryStore, 'applyDefaultLocation')
    discoveryStore.applyDefaultLocation([], undefined)
    expect(discoveryStore.applyDefaultLocation).toBeCalledWith([], undefined)
  })

  it('Test for closeDeleteModal', () => {
    const discoveryStore = useDiscoveryStore()
    vi.spyOn(discoveryStore, 'closeDeleteModal')
    discoveryStore.closeDeleteModal()
    expect(discoveryStore.closeDeleteModal).toBeCalled()
    expect(discoveryStore.deleteModalOpen).toStrictEqual(false)

  })

  it('Test for openDeleteModal', () => {
    const discoveryStore = useDiscoveryStore()
    vi.spyOn(discoveryStore, 'openDeleteModal')
    discoveryStore.openDeleteModal()
    expect(discoveryStore.openDeleteModal).toBeCalled()
    expect(discoveryStore.deleteModalOpen).toStrictEqual(true)
  })

  it('Test for editDiscovery', () => {
    const discoveryStore = useDiscoveryStore()
    vi.spyOn(discoveryStore, 'editDiscovery')
    discoveryStore.editDiscovery('testing')
    expect(discoveryStore.editDiscovery).toBeCalledWith('testing')
    expect(discoveryStore.validationErrors).toStrictEqual({})
    expect(discoveryStore.discoveryFormActive).toBe(true)
    expect(discoveryStore.discoveryTypePageActive).toBe(false)
  })
  it('Test for searchForLocation', () => {
    const discoveryStore = useDiscoveryStore()
    vi.spyOn(discoveryStore, 'searchForLocation')
    discoveryStore.searchForLocation('testing')
    expect(discoveryStore.searchForLocation).toBeCalledWith('testing')
  })

  it('Test for locationSelected', () => {
    const discoveryStore = useDiscoveryStore()
    vi.spyOn(discoveryStore, 'locationSelected')
    discoveryStore.locationSelected('testing')
    expect(discoveryStore.locationSelected).toBeCalledWith('testing')
    expect(discoveryStore.foundLocations).toStrictEqual([])
    expect(discoveryStore.locationSearch).toBe('')
  })

  it('Test for removeLocation', () => {
    const discoveryStore = useDiscoveryStore()
    vi.spyOn(discoveryStore, 'removeLocation')
    discoveryStore.removeLocation('testing')
    expect(discoveryStore.removeLocation).toBeCalledWith('testing')
  })

  it('Test for removeTag', () => {
    const discoveryStore = useDiscoveryStore()
    vi.spyOn(discoveryStore, 'removeTag')
    discoveryStore.removeTag('testing')
    expect(discoveryStore.removeTag).toBeCalledWith('testing')
  })

  it('Test for tagSelected', () => {
    const discoveryStore = useDiscoveryStore()
    vi.spyOn(discoveryStore, 'tagSelected')
    discoveryStore.tagSelected('testing')
    expect(discoveryStore.tagSelected).toBeCalledWith('testing')

  })

  it('Test for setMetaSelectedDiscoveryValue', () => {
    const discoveryStore = useDiscoveryStore()
    vi.spyOn(discoveryStore, 'setMetaSelectedDiscoveryValue')
    discoveryStore.setMetaSelectedDiscoveryValue('testing', 1)
    expect(discoveryStore.setMetaSelectedDiscoveryValue).toBeCalledWith('testing', 1)
  })

  it('Test for setSelectedDiscoveryValue', () => {
    const discoveryStore = useDiscoveryStore()
    vi.spyOn(discoveryStore, 'setSelectedDiscoveryValue')
    discoveryStore.setSelectedDiscoveryValue('testing', 1)
    expect(discoveryStore.setSelectedDiscoveryValue).toBeCalledWith('testing', 1)
  })


  it('Test for activateForm', () => {
    const discoveryStore = useDiscoveryStore()
    vi.spyOn(discoveryStore, 'activateForm')
    discoveryStore.activateForm('testing', 1)
    expect(discoveryStore.activateForm).toBeCalledWith('testing', 1)
    expect(discoveryStore.discoveryTypePageActive).toBe(false)
    expect(discoveryStore.discoveryFormActive).toBe(true)
  })

  it('Test for searchForTags', () => {
    const discoveryStore = useDiscoveryStore()
    vi.spyOn(discoveryStore, 'searchForTags')
    discoveryStore.searchForTags('testing')
    expect(discoveryStore.foundTags).toStrictEqual([])
    expect(discoveryStore.searchForTags).toBeCalledWith('testing' )
  })


  it('Test for toggleDiscovery', () => {
    const discoveryStore = useDiscoveryStore()
    vi.spyOn(discoveryStore, 'toggleDiscovery')

    discoveryStore.toggleDiscovery({id: 1, name: 'faizan', tags: [], locations: [], type: '', meta: { communityStrings: '',
      udpPorts: '',
      toggle: { toggle: false, id: 1 },
      ipRanges: ''

    } })
    expect(discoveryStore.toggleDiscovery).toBeCalledWith({id: 1, name: 'faizan', tags: [], locations: [], type: '', meta: { communityStrings: '',
      udpPorts: '',
      toggle: { toggle: false, id: 1 },
      ipRanges: ''

    } })
  })


  it('Test for cancelUpdate', () => {
    const discoveryStore = useDiscoveryStore()
    vi.spyOn(discoveryStore, 'cancelUpdate')
    discoveryStore.cancelUpdate()
    expect(discoveryStore.cancelUpdate).toBeCalled()
    expect(discoveryStore.selectedDiscovery).toStrictEqual({})
    expect(discoveryStore.discoveryFormActive).toBe(false)
    expect(discoveryStore.discoveryTypePageActive).toBe(false)
  })
  it('Test for deleteDiscovery', () => {
    const discoveryStore = useDiscoveryStore()
    vi.spyOn(discoveryStore, 'deleteDiscovery')
    discoveryStore.deleteDiscovery()
    expect(discoveryStore.deleteDiscovery).toBeCalled()
  })

  it('Test for customValidator', () => {
    const discoveryStore = useDiscoveryStore()
    vi.spyOn(discoveryStore, 'customValidator')
    discoveryStore.customValidator({
      id: 1,
      name: '',
      tags: [],
      locations: [],
      type: '',
      meta: {  communityStrings: '',
        udpPorts: '',
        toggle: { toggle: false, id: 1 }}
    })
    expect(discoveryStore.customValidator).toBeCalledWith( {
      id: 1,
      name: '',
      tags: [],
      locations: [],
      type: '',
      meta: {  communityStrings: '',
        udpPorts: '',
        toggle: { toggle: false, id: 1 }}
    })
  })

  it('Test for validateDiscovery', () => {
    const discoveryStore = useDiscoveryStore()
    vi.spyOn(discoveryStore, 'validateDiscovery')
    discoveryStore.validateDiscovery()
    expect(discoveryStore.validateDiscovery).toBeCalled()
  })

  it('Test for saveSelectedDiscovery', () => {
    const discoveryStore = useDiscoveryStore()
    vi.spyOn(discoveryStore, 'saveSelectedDiscovery')
    discoveryStore.saveSelectedDiscovery()
    expect(discoveryStore.saveSelectedDiscovery).toBeCalled()
  })
})


