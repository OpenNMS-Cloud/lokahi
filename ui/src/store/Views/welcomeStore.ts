import { defineStore } from 'pinia'
import * as yup from 'yup'
import {
  CertificateResponse,
  ListNodeMetricsQuery,
  Node
} from '@/types/graphql'
import { createAndDownloadBlobFile } from '@/components/utils'
import router from '@/router'
import { ItemPreviewProps } from '@/components/Common/commonTypes'
import { useLocationMutations } from '../Mutations/locationMutations'
import { useDiscoveryMutations } from '../Mutations/discoveryMutations'
import { useDiscoveryQueries } from '../Queries/discoveryQueries'
import { REGEX_EXPRESSIONS } from '@/components/Discovery/discovery.constants'
import { validationErrorsToStringRecord } from '@/services/validationService'
import useMinionCmd from '@/composables/useMinionCmd'
import { ComputedRef } from 'vue'
import { useWelcomeQueries } from '../Queries/welcomeQueries'
import { activateDiscoveryHelpGuide, activateMinionHelpGuide, disableMinionHelpGuide } from '@/services/pendoService'

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
  minionCmd: {
    minionDockerCmd: ComputedRef<string>;
    setPassword: (pass: string) => string;
    setMinionId: (minionIdString: string) => string;
    clearMinionCmdVals: () => void;
  }
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
      title: 'Node Discovery',
      loading: false,
      itemTitle: '',
      itemSubtitle: '',
      itemStatuses: [
      ],
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
      name: yup.string().required("Please enter a name."),
      ip: yup.string().required("Please enter an IP.").matches(new RegExp(REGEX_EXPRESSIONS.IP[0]), 'Single IP address only. You cannot enter a range.'),
      communityString: yup.string(),
      port: yup.number()
    }).required(),
    firstLocation: { id: -1, location: '' },
    invalidForm: true,
    minionCert: { password: '', certificate: '' },
    minionErrorTimeout: -1,
    minionCmd: useMinionCmd(),
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
      const minions = await getAllMinions();
      await this.createDefaultLocation();
      if (minions?.length > 0) {
        onboardingState = false
      } else {
        router.push('/welcome');
      }
      this.showOnboarding = onboardingState
      setTimeout(() => {
        this.ready = true;
      }, 400)
      setTimeout(() => {
        this.doneGradient = true;
      }, 410)
      setTimeout(() => {
        this.doneLoading = true;
      }, 650)
    },
    buildItemStatus({ title, condition, status }: { title: string, status: string, condition: boolean }) {

      const successStatus = condition ? this.getSuccessStatus() : this.getFailureStatus();

      return {
        title,
        status,
        ...successStatus
      }
    },
    async createDefaultLocation() {
      const { getLocationsForWelcome } = useWelcomeQueries();
      const locations = await getLocationsForWelcome();
      const existingDefault = locations?.find((def: { location: string }) => def.location === 'default');
      if (!existingDefault) {
        const locationMutations = useLocationMutations();
        const { data } = await locationMutations.createLocation({ location: 'default' })
        if (data?.location && data?.id) {
          this.firstLocation = { location: data.location, id: data.id }
        }
      } else {
        this.firstLocation = existingDefault;
      }
    },
    copyDockerClick() {
      navigator.clipboard.writeText(this.dockerCmd()).then(() => (this.copied = true))
      this.copyButtonCopy = 'Copied';
      setTimeout(() => {
        this.copyButtonCopy = 'Copy';
        this.copied = false
      }, 5000)
    },
    dockerCmd() {
      let dcmd = this.minionCmd.minionDockerCmd

      if (location.origin === 'https://onmshs.local:1443' || location.origin.startsWith('http://localhost:')) {
        dcmd = `docker run --rm -p 8181:8181 -p 8101:8101 -p 1162:1162/udp -p 8877:8877/udp -p 4729:4729/udp -p 9999:9999/udp -p 162:162/udp -e USE_KUBERNETES="false" -e MINION_GATEWAY_HOST="host.docker.internal" -e MINION_GATEWAY_PORT=1443 -e MINION_GATEWAY_TLS="true" -e GRPC_CLIENT_TRUSTSTORE=/opt/karaf/gateway.crt --mount type=bind,source="${import.meta.env.VITE_MINION_PATH}/target/tmp/server-ca.crt",target="/opt/karaf/gateway.crt",readonly -e GRPC_CLIENT_KEYSTORE='/opt/karaf/minion.p12' -e GRPC_CLIENT_KEYSTORE_PASSWORD='${this.minionCert.password}' -e MINION_ID='default' --mount type=bind,source="/PATH_TO_DOWNLOADED_FILE/${this.defaultLocationName}-certificate.p12",target="/opt/karaf/minion.p12",readonly  -e GRPC_CLIENT_OVERRIDE_AUTHORITY="minion.onmshs.local" -e IGNITE_SERVER_ADDRESSES="localhost" opennms/lokahi-minion:latest`
      }

      //If the user made edits, thats the one we want.
      if (this.modifiedDockerCommand) {
        dcmd = this.modifiedDockerCommand
      }

      return dcmd;
    },
    async downloadClick() {
      const { getMinionCertificate } = useWelcomeQueries();
      this.downloadCopy = 'Downloading'
      this.downloading = true;
      this.minionCert = await getMinionCertificate(this.firstLocation.id)
      this.downloading = false;
      this.downloaded = true
      this.downloadCopy = 'Downloaded'
      this.minionCmd.setPassword(this.minionCert.password ?? '');
      this.minionCmd.setMinionId(this.defaultLocationName);
      createAndDownloadBlobFile(this.minionCert.certificate, `${this.defaultLocationName}-certificate.p12`)

      this.refreshing = true
      this.refreshMinions()

      this.minionStatusLoading = true
      this.minionStatusStarted = true
      this.updateMinionStatusCopy()
      setTimeout(() => {
        this.downloadCopy = 'Download';
        this.downloaded = false;
      }, 5000)

      this.minionErrorTimeout = window.setTimeout(() => {
        activateMinionHelpGuide();
      }, 600000)

    },
    async getFirstNode() {
      const defaultLatency = 0;
      const timeoutDelay = 10000;
      const maxDelayLoops = 9; // 10 seconds * 9 loops === 1.5 minutes of waiting before showing a DOWN discovery. 
      const { getDiscoveries } = useDiscoveryQueries();
      const { getNodeDetails } = useWelcomeQueries();
      await getDiscoveries();
      const details = await getNodeDetails(this.firstDiscovery.name);
      const metric = details?.metrics?.nodeLatency?.data?.result?.[0]?.values?.[0]?.[1]
      if ((details.detail && metric) || details.detail && this.delayCounter > maxDelayLoops) {
        this.stopDiscoveryErrorTimeout();
        this.setDevicePreview(details.detail, details.metrics, metric ?? defaultLatency);
        this.devicePreview.loading = false;
        this.slideThreeDisabled = false;
      } else {
        if (details.detail) {
          this.delayCounter += 1;
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
      if (this.slide !== 2) {
        this.stopMinionErrorTimeout()
      }
      if (this.slide !== 3) {
        this.stopDiscoveryErrorTimeout();
      }
      if (this.slide === 3) {
        this.loadDevicePreview()
      }
      this.scrollTop()
    },

    async refreshMinions() {
      if (this.refreshing && this.firstLocation.location) {
        const { getAllMinions } = useWelcomeQueries();
        const localMinions = await getAllMinions()
        if (localMinions?.length > 0) {
          this.stopMinionErrorTimeout();
          this.refreshing = false
          this.minionStatusSuccess = true
          this.minionStatusLoading = false
          this.updateMinionStatusCopy();
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
      });
      this.devicePreview.itemStatuses[1] = this.buildItemStatus({
        title: 'ICMP', status: metric.toString(), condition:
          !!metric.toString()
      });
    },
    skipSlideThree() {
      this.stopMinionErrorTimeout();
      this.stopDiscoveryErrorTimeout();
      router.push('Dashboard')
    },
    async startDiscovery() {
      await this.validateFirstDiscovery();
      if (!this.invalidForm) {
        const { createDiscoveryConfig } = useDiscoveryMutations();

        await createDiscoveryConfig({
          request: {
            ipAddresses: [this.firstDiscovery.ip],
            locationId: this.firstLocation.id.toString(),
            name: this.firstDiscovery.name,
            snmpConfig: { ports: [Number(this.firstDiscovery.port)], readCommunities: [this.firstDiscovery.communityString] },
            tags: [{ name: 'default' }]
          }
        })

        this.devicePreview.loading = true;
        this.discoverySubmitted = true;
        this.discoveryErrorTimeout = window.setTimeout(() => activateDiscoveryHelpGuide(), 180000);
        this.getFirstNode();
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
        this.validateFirstDiscovery();
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
      this.modifiedDockerCommand = newCommand;
    },
    async validateFirstDiscovery() {
      try {
        await this.firstDiscoveryValidation.validate(this.firstDiscovery, { abortEarly: false });
        this.firstDiscoveryErrors = { ip: '', name: '', communityString: '', port: '' }
        this.invalidForm = false;
      } catch (e) {
        this.invalidForm = true;
        this.firstDiscoveryErrors = validationErrorsToStringRecord<{ ip: string, name: string, communityString?: string, port?: string }>(e as yup.ValidationError);
        this.validateOnKeyup = true;
      }
    },
  },
})
