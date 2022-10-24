import { createTestingPinia } from '@pinia/testing'
import { setActivePinia } from 'pinia'
import { createClient, setActiveClient } from 'villus'

const setActive = (state: any) => {
  setActivePinia(createTestingPinia({
    initialState: state
  }))

  setActiveClient(createClient({
    url: 'http://test/graphql'
  }))
}

const setAppliancesStore = (minionsDevices: any) => {
  const { minions: tableMinions, devices: tableDevices } = minionsDevices
  const state = {
    appliancesQueries: { 
      tableMinions,
      tableDevices
    }
  }

  setActive(state)
}

export {
  setAppliancesStore
}