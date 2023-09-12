import { defineStore } from 'pinia'
import { useDiscoveryQueries } from '../Queries/discoveryQueries'
import { DiscoveryType, InstructionsType } from '@/components/Discovery/discovery.constants'
import { DiscoveryStore, DiscoveryTrapMeta, NewOrUpdatedDiscovery } from '@/types/discovery'
import { clientToServerValidation, discoveryFromClientToServer, discoveryFromServerToClient } from '@/dtos/discovery.dto'
import { useDiscoveryMutations } from '../Mutations/discoveryMutations'


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
    instructionsType: InstructionsType.Active,
    helpActive:false,
    newDiscoveryModalActive: false,
    selectedDiscovery: {},
    snmpV3Enabled: false,
    soloTypeEditor: true,
    discoveryTypePageActive: false,
    tagError:'',
    tagSearch: '',
    validationErrors:{},
    validateOnKeyUp: false
  } as DiscoveryStore),
  actions: {
    async init(){
      const discoveryQueries = useDiscoveryQueries()
      const latestDiscoveries = await discoveryQueries.getDiscoveries()
      await discoveryQueries.getLocations()
      if (latestDiscoveries.data && !latestDiscoveries.error){
        this.loadedDiscoveries = discoveryFromServerToClient(latestDiscoveries.data,discoveryQueries.locations)
      }
      for (const loadedDiscovery of this.loadedDiscoveries){
        if ([DiscoveryType.ICMP,DiscoveryType.Azure].includes(loadedDiscovery.type as DiscoveryType) && loadedDiscovery.id){
          await discoveryQueries.getTagsByActiveDiscoveryId(loadedDiscovery.id)
          loadedDiscovery.tags = discoveryQueries.tagsByActiveDiscoveryId
        }
        if ([DiscoveryType.SyslogSNMPTraps].includes(loadedDiscovery.type as DiscoveryType) && loadedDiscovery.id){
          await discoveryQueries.getTagsByPassiveDiscoveryId(loadedDiscovery.id)
          loadedDiscovery.tags = discoveryQueries.tagsByPassiveDiscoveryId
        }
      }
      if (this.loadedDiscoveries.length === 0){
        this.startNewDiscovery()
      }
      this.loading = false
    },
    activateHelp(type: string){
      this.instructionsType = type
      this.helpActive = true
    },
    disableHelp(){
      this.helpActive = false
    },
    clearLocationAuto(){
      this.foundLocations = []
      this.locationSearch = ''
    },
    clearTagAuto(){
      this.foundTags = []
      this.tagSearch = ''
    },
    backToDiscovery(){
      this.newDiscoveryModalActive = false
      this.discoveryFormActive = false
      this.validateOnKeyUp = false
      this.discoveryTypePageActive = false
      this.validationErrors = {}
      this.selectedDiscovery = {}
    },
    closeNewModal(){
      this.newDiscoveryModalActive = false
    },
    backToTypePage(){
      this.discoveryFormActive = false
      this.validateOnKeyUp = false
      this.validationErrors = {}
      this.discoveryTypePageActive = true
    },
    startNewDiscovery(){
      if (this.soloTypeEditor){
        this.discoveryTypePageActive = true
        this.discoveryFormActive = false
      } else {
        this.discoveryFormActive = true
      }
      this.setupDefaultDiscovery()
    },
    setupDefaultDiscovery(){
      const discoveryQueries = useDiscoveryQueries()
      this.selectedDiscovery = {name:undefined,id:undefined,tags:[],locations:[],type:undefined,meta:{clientId:'',clientSecret:'',clientSubscriptionId:'',directoryId:'',communityStrings:'public',udpPorts:'161'}}
      const defaultLocation = discoveryQueries.locations.find((d) => d.location === 'default')
      if (defaultLocation){
        this.selectedDiscovery.locations = [defaultLocation]
      }
      this.selectedDiscovery.tags = [{name:'default'}]
    },
    closeDeleteModal(){
      this.deleteModalOpen = false
    },
    openDeleteModal(){
      this.deleteModalOpen = true
    },
    editDiscovery(item: any){
      this.discoveryFormActive = true
      this.discoveryTypePageActive = false
      this.selectedDiscovery = {...item}
    },
    async searchForLocation(searchVal: string){
      const discoveryQueries = useDiscoveryQueries()
      this.locationSearch = searchVal
      await discoveryQueries.getLocations()
      this.foundLocations = discoveryQueries.locations.filter((b) => b.location?.includes(searchVal)).map((d) => d.location)
    },
    locationSelected(location: any) {
      const discoveryQueries = useDiscoveryQueries()
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
      const discoveryQueries = useDiscoveryQueries()
      let foundTag = discoveryQueries.tagsSearched.find((d) => d.name === tag) as any
      if (!foundTag){
        foundTag = {name: tag}
      }
      this.selectedDiscovery.tags?.push(foundTag)
      this.foundTags = []
      this.tagSearch = ''
      if (this.validateOnKeyUp){
        this.validateDiscovery()
      }
    },
    setMetaSelectedDiscoveryValue(key:string,value:any){
      if (!this.selectedDiscovery.meta){
        this.selectedDiscovery.meta = {}
      }
      (this.selectedDiscovery as Record<string,any>).meta[key] = value
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
      this.discoveryTypePageActive = false
      this.discoveryFormActive = true
    },
    async searchForTags(searchVal: string){
      const discoveryQueries = useDiscoveryQueries()
      this.tagSearch = searchVal
      await discoveryQueries.getTagsSearch(searchVal)
      this.foundTags = discoveryQueries.tagsSearched.map((b) => b?.name ?? '')
    },
    async toggleDiscovery(clickedToggle: NewOrUpdatedDiscovery){
      const discoveryMutations = useDiscoveryMutations()
      const meta = clickedToggle?.meta as DiscoveryTrapMeta
      const toggleObject = {toggle: !meta.toggle?.toggle || false, id: meta.toggle?.id || clickedToggle.id}
      this.loading = true
      await discoveryMutations.togglePassiveDiscovery({toggle:toggleObject})
      await this.init()
      this.loading = false
    },
    async cancelUpdate(){
      this.selectedDiscovery = {}
      this.discoveryFormActive = false
      this.discoveryTypePageActive = false
    },
    async deleteDiscovery(){
      const discoveryMutations = useDiscoveryMutations()
      this.loading = true
      if (this.selectedDiscovery.type === DiscoveryType.SyslogSNMPTraps){
        await discoveryMutations.deletePassiveDiscovery({id:this.selectedDiscovery.id})
      }else if(this.selectedDiscovery.type === DiscoveryType.ICMP){
        await discoveryMutations.deleteActiveIcmpDiscovery({id:this.selectedDiscovery.id})
      }
      this.backToDiscovery()
      this.closeDeleteModal()
      await this.init()
      this.loading = false
    },
    async validateDiscovery(){
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
        this.newDiscoveryModalActive = true
        this.validateOnKeyUp = false
      }else {
        this.validateOnKeyUp = true
      }
      this.loading = false
    }
  }
})
