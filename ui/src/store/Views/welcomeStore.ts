import { defineStore } from 'pinia'
import * as yup from 'yup'
import {
  CertificateResponse,
  ListNodeMetricsQuery,
  Node
} from '@/types/graphql'
import { createAndDownloadBlobFile } from '@/components/utils'
import { ItemPreviewProps } from '@/components/Common/commonTypes'
import { useLocationMutations } from '../Mutations/locationMutations'
import { useDiscoveryMutations } from '../Mutations/discoveryMutations'
import { useDiscoveryQueries } from '../Queries/discoveryQueries'
import { REGEX_EXPRESSIONS } from '@/components/Discovery/discovery.constants'
import { validationErrorsToStringRecord } from '@/services/validationService'
import { useWelcomeQueries } from '../Queries/welcomeQueries'

interface WelcomeStoreState {
  copied: boolean
  copyButtonCopy: string
  defaultLocationName: string
  delayCounter: number
  detectedDevice: Partial<Node> | undefined
  devicePreview: ItemPreviewProps
  discoverySubmitted: boolean
  discoveryErrorTimeout: number
  doneLoading: boolean
  doneGradient: boolean
  downloading: boolean;
  downloadCopy: string
  downloaded: boolean
  firstDiscovery: Record<string, string>
  firstDiscoveryErrors: Record<string, string>
  firstDiscoveryValidation: yup.ObjectSchema<{}>,
  firstLocation: { id: number, location: string }
  invalidForm: boolean
  minionCert: CertificateResponse
  minionErrorTimeout: number,
  minionStatusCopy: string
  minionStatusLoading: boolean
  minionStatusStarted: boolean
  minionStatusSuccess: boolean
  modifiedDockerCommand: string
  ready: boolean
  refreshing: boolean
  showOnboarding: boolean
  slide: number
  slideOneCollapseVisible: boolean
  slideThreeDisabled: boolean
  validateOnKeyup: boolean
}

export const useWelcomeStore = defineStore('welcomeStore', {
  state: () =>
    ({
      copied: false,
      copyButtonCopy: 'Copy',
      delayCounter: 0,
      defaultLocationName: 'default',
      detectedDevice: {},
      devicePreview: {
        title: 'Minion Gateway',
        loading: false,
        loadingCopy: 'Loading first discovery. This can take up to 3 minutes.',
        itemTitle: '',
        itemSubtitle: '',
        itemStatuses: [],
        bottomCopy: 'We assigned your device to a location called \'default.\''
      },
      discoverySubmitted: false,
      discoveryErrorTimeout: -1,
      doneGradient: false,
      doneLoading: false,
      downloadCopy: 'Download',
      downloaded: false,
      downloading: false,
      firstDiscovery: { name: 'MyFirstDiscovery', ip: '', communityString: 'public', port: '161' },
      firstDiscoveryErrors: { name: '', ip: '', communityString: '', port: '' },
      firstDiscoveryValidation: yup.object().shape({
        name: yup.string().required('Please enter a name.'),
        ip: yup.string().required('Please enter an IP.').matches(new RegExp(REGEX_EXPRESSIONS.IP[0]), 'Single IP address only. You cannot enter a range.'),
        communityString: yup.string(),
        port: yup.number()
      }).required(),
      firstLocation: { id: -1, location: '' },
      invalidForm: true,
      minionCert: { password: '', certificate: '' },
      minionErrorTimeout: -1,
      minionStatusCopy: 'Waiting for the Docker Install Command to be complete.',
      minionStatusLoading: false,
      minionStatusStarted: false,
      minionStatusSuccess: false,
      modifiedDockerCommand: '',
      ready: false,
      refreshing: false,
      slide: 1,
      slideOneCollapseVisible: true,
      slideThreeDisabled: true,
      showOnboarding: false,
      validateOnKeyup: false
    } as WelcomeStoreState),
  actions: {
    async init() {
      let onboardingState = true
      const { getAllMinions } = useWelcomeQueries()
      const minions = await getAllMinions()
      if (minions?.length > 0) {
        onboardingState = false
      } else {
        // Import router dynamically, see https://opennms.atlassian.net/browse/HS-1654
        const router = (await import('@/router')).default
        router.push('/welcome')
      }
      this.showOnboarding = onboardingState
      setTimeout(() => {
        this.ready = true
      }, 400)
      setTimeout(() => {
        this.doneGradient = true
      }, 410)
      setTimeout(() => {
        this.doneLoading = true
      }, 650)
    },
    buildItemStatus({ title, condition, status }: { title: string, status: string, condition: boolean }) {

      const successStatus = condition ? this.getSuccessStatus() : this.getFailureStatus()

      return {
        title,
        status,
        ...successStatus
      }
    },
    async createDefaultLocation() {
      const { getLocationsForWelcome } = useWelcomeQueries()
      const locations = await getLocationsForWelcome()
      const existingDefault = locations?.find((def: { location: string }) => def.location === 'default')
      if (!existingDefault) {
        const locationMutations = useLocationMutations()
        const { data } = await locationMutations.createLocation({ location: 'default' })
        if (data?.location && data?.id) {
          this.firstLocation = { location: data.location, id: data.id }
        }
      } else {
        this.firstLocation = existingDefault
      }
    },
    copyDockerClick() {
      const dcmd = 'docker compose up -d'
      navigator.clipboard.writeText(dcmd).then(() => (this.copied = true))
      this.copyButtonCopy = 'Copied'
      setTimeout(() => {
        this.copyButtonCopy = 'Copy'
        this.copied = false
      }, 5000)
    },
    async downloadClick() {
      const { getMinionCertificate } = useWelcomeQueries()
      this.downloadCopy = 'Downloading'
      this.downloading = true
      this.minionCert = await getMinionCertificate(this.firstLocation.id)
      this.downloading = false
      this.downloaded = true
      this.downloadCopy = 'Downloaded'
      createAndDownloadBlobFile(this.minionCert.certificate, `minion-${this.defaultLocationName}.zip`)

      this.refreshing = true
      this.refreshMinions()

      this.minionStatusLoading = true
      this.minionStatusStarted = true
      this.updateMinionStatusCopy()
      setTimeout(() => {
        this.downloadCopy = 'Download'
        this.downloaded = false
      }, 5000)

      this.minionErrorTimeout = window.setTimeout(() => {
        this.minionStatusCopy = 'Please wait while we detect your Minion. This can sometimes take more than 10 minutes.'
      }, 600000)

    },
    async getFirstNode() {
      const defaultLatency = 0
      const timeoutDelay = 10000
      const maxDelayLoops = 9 // 10 seconds * 9 loops === 1.5 minutes of waiting before showing a DOWN discovery. 
      const { getDiscoveries } = useDiscoveryQueries()
      const { getNodeDetails } = useWelcomeQueries()
      await getDiscoveries()
      const details = await getNodeDetails(this.firstDiscovery.name)
      const metric = details?.metrics?.nodeLatency?.data?.result?.[0]?.values?.[0]?.[1]
      if ((details.detail && metric) || details.detail && this.delayCounter > maxDelayLoops) {
        this.stopDiscoveryErrorTimeout()
        this.setDevicePreview(details.detail, details.metrics, metric ?? defaultLatency)
        this.devicePreview.loading = false
        this.slideThreeDisabled = false
      } else {
        if (details.detail) {
          this.delayCounter += 1
        }
        setTimeout(() => { this.getFirstNode() }, timeoutDelay)
      }
    },
    getFailureStatus() {
      return {
        statusColor: 'var(--feather-error)',
        statusText: 'var(--feather-primary-text-on-color)'
      }
    },
    getSuccessStatus() {
      return {
        statusColor: 'var(--feather-success)',
        statusText: 'var(--feather-primary-text-on-color)'
      }
    },
    loadDevicePreview() {
      this.devicePreview.loading = true
    },
    nextSlide() {
      this.slide = this.slide + 1
      if (this.slide === 2) {
        if (this.firstLocation.id === -1) {
          this.createDefaultLocation()
        }
      }
      if (this.slide !== 2) {
        this.stopMinionErrorTimeout()
      }
      if (this.slide !== 3) {
        this.stopDiscoveryErrorTimeout()
      }
      if (this.slide === 3) {
        this.loadDevicePreview()
      }
      this.scrollTop()
    },

    async refreshMinions() {
      if (this.refreshing && this.firstLocation.location) {
        const { getAllMinions } = useWelcomeQueries()
        const localMinions = await getAllMinions()
        if (localMinions?.length > 0) {
          this.stopMinionErrorTimeout()
          this.refreshing = false
          this.minionStatusSuccess = true
          this.minionStatusLoading = false
          this.updateMinionStatusCopy()
        } else {
          setTimeout(() => { this.refreshMinions() }, 10000)
        }
      }
    },
    prevSlide() {
      this.slide = this.slide - 1
      this.scrollTop()
    },
    scrollTop() {
      const welcomeWrapper = document.querySelector('.welcome-wrapper')
      if (welcomeWrapper) {
        welcomeWrapper?.scrollTo({ top: 0, behavior: 'smooth' })
      }
    },
    setDevicePreview(detail: { nodeLabel?: string, createTime: number }, metrics: ListNodeMetricsQuery | null | undefined, metric: number) {
      this.devicePreview.itemTitle = detail.nodeLabel ?? ''
      this.devicePreview.itemSubtitle = 'Added ' + new Intl.DateTimeFormat('en-US').format(new Date(detail.createTime))
      this.devicePreview.itemStatuses[0] = this.buildItemStatus({
        title: 'Status', status: metrics?.nodeStatus?.status ?? '', condition:
          metrics?.nodeStatus?.status === 'UP'
      })
      this.devicePreview.itemStatuses[1] = this.buildItemStatus({
        title: 'ICMP', status: metric.toString(), condition:
          !!metric.toString()
      })
    },
    async skipSlideThree() {
      this.stopMinionErrorTimeout()
      this.stopDiscoveryErrorTimeout()
      // Import router dynamically, see https://opennms.atlassian.net/browse/HS-1654
      const router = (await import('@/router')).default
      router.push('Dashboard')
    },
    async startDiscovery() {
      await this.validateFirstDiscovery()
      if (!this.invalidForm) {
        const { createDiscoveryConfig } = useDiscoveryMutations()

        await createDiscoveryConfig({
          request: {
            ipAddresses: [this.firstDiscovery.ip],
            locationId: this.firstLocation.id.toString(),
            name: this.firstDiscovery.name,
            snmpConfig: { ports: [Number(this.firstDiscovery.port)], readCommunities: [this.firstDiscovery.communityString] },
            tags: [{ name: 'default' }]
          }
        })

        this.devicePreview.loading = true
        this.discoverySubmitted = true
        this.discoveryErrorTimeout = window.setTimeout(() => this.devicePreview.loadingCopy = 'Loading first discovery. This can take more than 3 minutes.', 180000)
        this.getFirstNode()
      }
    },
    stopDiscoveryErrorTimeout() {
      window.clearTimeout(this.discoveryErrorTimeout)
    },
    stopMinionErrorTimeout() {
      window.clearTimeout(this.minionErrorTimeout)
    },
    toggleSlideOneCollapse() {
      this.slideOneCollapseVisible = !this.slideOneCollapseVisible
    },
    updateFirstDiscovery(key: string, value: string | number | undefined) {
      if (typeof value === 'string') {
        this.firstDiscovery[key] = value
      }
      if (this.validateOnKeyup) {
        this.validateFirstDiscovery()
      }
    },
    updateMinionStatusCopy() {
      if (!this.minionStatusStarted) {
        this.minionStatusCopy = 'Waiting for the Docker Install Command to be complete.'
      }
      if (this.minionStatusStarted && this.minionStatusLoading) {
        this.minionStatusCopy = 'Please wait while we detect your Minion. This can take up to 10 minutes.'
      }
      if (this.minionStatusStarted && !this.minionStatusLoading && this.minionStatusSuccess) {
        this.minionStatusCopy = 'Minion detected.'
      }
    },
    updateDockerCommand(newCommand: string) {
      this.modifiedDockerCommand = newCommand
    },
    async validateFirstDiscovery() {
      try {
        await this.firstDiscoveryValidation.validate(this.firstDiscovery, { abortEarly: false })
        this.firstDiscoveryErrors = { ip: '', name: '', communityString: '', port: '' }
        this.invalidForm = false
      } catch (e) {
        this.invalidForm = true
        this.firstDiscoveryErrors = validationErrorsToStringRecord<{ ip: string, name: string, communityString?: string, port?: string }>(e as yup.ValidationError)
        this.validateOnKeyup = true
      }
    }
  }
})
