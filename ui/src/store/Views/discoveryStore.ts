import { defineStore } from 'pinia'
import { useDiscoveryQueries } from '../Queries/discoveryQueries'
import { DiscoveryType } from '@/components/Discovery/discovery.constants'
import { DiscoveryStore, DiscoveryTrapMeta } from '@/types/discovery'
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
    validationErrors:{},
    validateOnKeyUp: false
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
      if (this.validateOnKeyUp){
        this.validateDiscovery()
      }
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
      if (this.validateOnKeyUp){
        this.validateDiscovery()
      }
    },
    setMetaSelectedDiscoveryValue(key:string,value:any){
      console.log('SELECTED',this.selectedDiscovery);
      (this.selectedDiscovery as Record<string,any>).meta[key] = value
      console.log('SELECTED',this.selectedDiscovery)
      if (this.validateOnKeyUp){
        this.validateDiscovery()
      }
    },
    setSelectedDiscoveryValue(key:string,value:any){
      (this.selectedDiscovery as Record<string,any>)[key] = value
      if (this.validateOnKeyUp){
        this.validateDiscovery()
      }
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
    async toggleDiscovery(){
      const discoveryMutations = useDiscoveryMutations()
      const meta = this.selectedDiscovery?.meta as DiscoveryTrapMeta
      const toggleObject = {toggle:!meta.toggle?.toggle || false,id:meta.toggle?.id || this.selectedDiscovery.id}
      this.loading = true
      await discoveryMutations.togglePassiveDiscovery({toggle:toggleObject})
      await this.init()
      this.loading = false
    },
    async cancelUpdate(){
      this.selectedDiscovery = {}
      this.discoveryFormActive = false
      this.soloTypePageActive = false
    },
    async deleteDiscovery(){
      const discoveryMutations = useDiscoveryMutations()
      this.loading = true
      if (this.selectedDiscovery.type === DiscoveryType.SyslogSNMPTraps){
        await discoveryMutations.deletePassiveDiscovery({id:this.selectedDiscovery.id})
      }else if(this.selectedDiscovery.type === DiscoveryType.ICMP){
        await discoveryMutations.deleteActiveIcmpDiscovery({id:this.selectedDiscovery.id})
      }
      this.loading = false
      this.closeDeleteModal()
    },
    async validateDiscovery(){
      console.log('validating:',this.selectedDiscovery)
      const {isValid,validationErrors} = await clientToServerValidation(this.selectedDiscovery)
      this.validationErrors = validationErrors
      return isValid
    },
    async saveSelectedDiscovery() {
      const discoveryMutations = useDiscoveryMutations()
      this.loading = true
      const isValid = await this.validateDiscovery()
      if (isValid){
        if (this.selectedDiscovery.type === DiscoveryType.SyslogSNMPTraps){
          await discoveryMutations.upsertPassiveDiscovery({passiveDiscovery:discoveryFromClientToServer(this.selectedDiscovery)})
        } else {
          await discoveryMutations.createOrUpdateDiscovery({request:discoveryFromClientToServer(this.selectedDiscovery)})
        }
        await this.init()
        this.validateOnKeyUp = false
      }else {
        this.validateOnKeyUp = true
      }
      this.loading = false
    }
  }
})
