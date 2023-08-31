import { defineStore } from 'pinia'
import { useDiscoveryQueries } from '../Queries/discoveryQueries'
import { DiscoveryType } from '@/components/Discovery/discovery.constants'
import { DiscoveryStore } from '@/types/discovery'
import { clientToServerValidation, discoveryFromClientToServer, discoveryFromServerToClient } from '@/dtos/discovery.dto'
import { useDiscoveryMutations } from '../Mutations/discoveryMutations'

const discoveryQueries = useDiscoveryQueries()

export const useDiscoveryStore = defineStore('discoveryStore', {
  state: () => ({
   
    discoveryFormActive:false,
    deleteModalOpen:false,
    foundLocations: [],
    foundTags: [],
    loading: false,
    loadedDiscoveries: [],
    locationError: '',
    locationSearch: '',
    selectedDiscovery: {},
    snmpV3Enabled: false,
    soloTypeEditor: true,
    soloTypePageActive: false,
    tagError:'',
    tagSearch: '',
    validationErrors:{}
  } as DiscoveryStore),
  actions: {
    async init(){
      const b = await discoveryQueries.getDiscoveries()
      await discoveryQueries.getLocations()
      if (b.data && !b.error){
        this.loadedDiscoveries = discoveryFromServerToClient(b.data,discoveryQueries.locations)
      }
      for (const b of this.loadedDiscoveries){
        if ([DiscoveryType.ICMP,DiscoveryType.Azure].includes(b.type as DiscoveryType) && b.id){
          await discoveryQueries.getTagsByActiveDiscoveryId(b.id)
          console.log('TAGS BY ACTIVE:',discoveryQueries.tagsByActiveDiscoveryId)
          b.tags = discoveryQueries.tagsByActiveDiscoveryId
        }
        if ([DiscoveryType.SyslogSNMPTraps].includes(b.type as DiscoveryType) && b.id){
          await discoveryQueries.getTagsByPassiveDiscoveryId(b.id)
          b.tags = discoveryQueries.tagsByPassiveDiscoveryId
        }
      }
    },
    clearLocationAuto(){
      this.foundLocations = []
      this.locationSearch = ''
    },
    clearTagAuto(){
      this.foundTags = []
      this.tagSearch = ''
    },
    backToTypePage(){
      this.discoveryFormActive = false
      this.soloTypePageActive = true
    },
    startNewDiscovery(){
      if (this.soloTypeEditor){
        this.soloTypePageActive = true
        this.discoveryFormActive = false
      } else {
        this.discoveryFormActive = true
      }
      this.selectedDiscovery = {name:undefined,id:undefined,tags:[],locations:[],type:undefined,meta:{}}
    },
    createOrUpdateDiscovery(){
      console.log('create!')
    },
    closeDeleteModal(){
      this.deleteModalOpen = false
    },
    openDeleteModal(){
      this.deleteModalOpen = true
    },
    editDiscovery(item: any){
      this.discoveryFormActive = true
      this.selectedDiscovery = {...item}
    },
    async searchForLocation(searchVal: string){
      this.locationSearch = searchVal
      await discoveryQueries.getLocations()
      this.foundLocations = discoveryQueries.locations.filter((b) => b.location?.includes(searchVal)).map((d) => d.location)
    },
    locationSelected(location: any) {
      this.selectedDiscovery.locations?.push(discoveryQueries.locations.find((d) => d.location === location))
      this.foundLocations = []
      this.locationSearch = ''
    },
    removeLocation(location: any){
      this.selectedDiscovery.locations = this.selectedDiscovery.locations?.filter((d) => d.id !== location.id)
    },
    removeTag(tag: any){
      this.selectedDiscovery.tags = this.selectedDiscovery.tags?.filter((d) => d.id !== tag.id)
    },
    tagSelected(tag: any) {
      this.selectedDiscovery.tags?.push(discoveryQueries.tagsSearched.find((d) => d.name === tag) as any)
      this.foundTags = []
      this.tagSearch = ''
    },
    setSelectedDiscoveryValue(key:string,value:any){
      (this.selectedDiscovery as Record<string,any>)[key] = value
    },
    activateForm(key: string, value: any){
      this.setSelectedDiscoveryValue(key,value)
      this.soloTypePageActive = false
      this.discoveryFormActive = true
    },
    async searchForTags(searchVal: string){
      this.tagSearch = searchVal
      await discoveryQueries.getTagsSearch(searchVal)
      this.foundTags = discoveryQueries.tagsSearched.map((b) => b.name || '')
    },
    toggleDiscovery(){
      this.discoveryFormActive = !this.discoveryFormActive
    },
    async cancelUpdate(){
      this.selectedDiscovery = {}
      this.discoveryFormActive = false
      this.soloTypePageActive = false
    },
    async deleteDiscovery(){
      const discoveryMutations = useDiscoveryMutations()
      this.loading = true
      await discoveryMutations.deleteActiveIcmpDiscovery({request:{id:this.selectedDiscovery.id}})
      this.loading = false
      this.closeDeleteModal()
    },
    async saveSelectedDiscovery() {
      const discoveryMutations = useDiscoveryMutations()
      this.loading = true
      const {isValid,validationErrors} = await clientToServerValidation(this.selectedDiscovery)
      this.validationErrors = validationErrors
      if (isValid){
        await discoveryMutations.createOrUpdateDiscovery(discoveryFromClientToServer(this.selectedDiscovery))
        await this.init()
      }
      this.loading = false
    }
  }
})
