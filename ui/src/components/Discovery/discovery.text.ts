export default {
  Discovery: {
    pageHeadline: 'Discovery',
    headline1: 'Select a discovery type',
    headline2: 'ICMP/SNMP Discovery Setup',
    nameInputLabel: 'ICMP/SNMP name',
    noneDiscoverySelectedMsg: 'Choose a discovery type to get started.',
    button: {
      add: 'Add Discovery',
      cancel: 'Cancel',
      submit: 'Save discovery'
    },
    error: {
      errorCreate: 'Error on creating discovery :('
    },
    empty: 'No discovery performed'
  },
  AddDiscoverySection: {
    activeDiscoveryTitle: 'Active Discovery',
    passiveDiscoveryTitle: 'Passive Discovery',
    icmpSnmp: 'ICMP/SNMP',
    azure: 'AZURE',
    syslog: 'Syslog & SNMP Traps'
  },
  ContentEditable: {
    IP: {
      label: 'Enter IP ranges and/or subnets'
    },
    CommunityString: {
      label: 'Enter community string'
    },
    UDPPort: {
      label: 'Enter UDP port'
    },
    errorRequired: 'The field is required.',
    errorInvalidValue: 'The field has invalid value.'
  }
}

export const DiscoverySNMPForm = {
  tag: 'Search/Add tags (optional)',
  nameInputLabel: 'ICMP/SNMP name',
  title: 'ICMP/SNMP Discovery Setup'
}

export const DiscoverySyslogSNMPTrapsForm = {
  headline: 'Syslog and SNMP Traps Discovery Setup',
  location: 'Search a location',
  help: {
    top: {
      heading: 'Forward Syslog to BTO with the following details:',
      list: {
        text1: 'Syslog Ingest IP: 193.177.129.26 (Minion IP)',
        text2: 'Syslog Ingest UDP Port: 20013',
        text3: 'Need help configuring Syslog forwarding?'
      }
    },
    step1: {
      heading: 'Step 1 heading',
      list: {
        text1: 'Enter configuration commands, one per line. End with CNTL/Z',
        text2: 'Router-Dallas(Config)#logging 192.168.0.30',
        text3: 'Router-Dallas(Config)#Servie timestamps debug datetime localtime show-timezone'
      }
    },
    step2: {
      heading: 'Step 2 heading',
      list: {
        text1: '...',
        text2: '...',
        text3: '...'
      }
    },
    step3: {
      heading: 'Step 3 heading',
      list: {
        text1: '...',
        text2: '...',
        text3: '...'
      }
    },
    step4: {
      heading: 'Step 4 heading',
      list: {
        text1: '...',
        text2: '...',
        text3: '...'
      }
    }
  }
}

export const Common = {
  tagsInput: 'Search/Add tags (optional)',
  tooltipIP: {
    title: 'IP list or IP ranges',
    description:
      'Separated by: [, ; .] <br />Examples: <br/> 127.0.0.1;<br />127.0.0.2,127.0.0.1-127.0.0.12  <br/> 172.16.0.0/12'
  },
  tooltipPort: {
    title: 'Port list',
    description: 'Separated by a space or one of these characters: [, ; :]'
  }
}

export const Azure = {
  title: 'Azure Discovery Setup',
  saveBtnText: 'Save Discovery',
  cancelBtnText: 'Cancel'
}

export const SuccessModalOptions = {
  successMsg: 'setup successful.',
  viewNodes: 'View Detected Nodes',
  addDiscovery: 'Add Another Discovery',
  addTransaction: 'Add Synthetic Transaction',
  addMonitoring: 'Add Monitoring Policy',
  checkboxText: "Don't show this again"
}

export const Instructions = {
  title: 'Discovery',
  subtitle:
    'The discovery process identifies devices and entities on your monitored network through either active or passive discovery.',
  activeDiscoveryTitle: 'What is Active Discovery?',
  activeDiscoverySubtitle:
    'Active discovery queries nodes and cloud APIs to detect the entities that you want to monitor. You can choose from two active discovery methods:',
  activeListTool: {
    tool1: 'ICMP/SNMP:',
    toolDescription1:
      'Performs a ping sweep and scans for SNMP MIBs on nodes that respond. You can click Validate to verify that you have at least one IP address, range, or subnet in your inventory.',
    tool2: 'Azure:',
    toolDescription2:
      'Connects to the Azure API, queries the virtual machines list, and creates entities for each VM in the node inventory.'
  },
  activeNote: 'You can create multiple discovery events to target specific areas of your network.',
  activeListCharacteristics: {
    benefits: 'Benefits:',
    benefitsDescription: 'Can be more comprehensive than passive discovery.',
    disadvantages: 'Disadvantages:',
    disadvantagesDescription: 'Can slow network performance as the discovery process tries to connect to all devices.'
  },
  passiveDiscoveryTitle: 'What is Passive Discovery?',
  passiveDiscoverySubtitle:
    'Passive discovery uses Syslog and SNMP traps to identify network devices. It does so by monitoring their activity through events, flows, and indirectly by evaluating other devices configuration settings.',
  passiveNote: 'Note that you can set only one passive discovery by location.',
  passiveListCharacteristics: {
    benefits: 'Benefits:',
    benefitsDescription: 'Low bandwidth consumption.',
    disadvantages: 'Disadvantages:',
    disadvantagesDescription:
      'May miss devices if they are not active. All devices must be enabled and configured to send Syslogs.'
  },
  learnMoreLink: {
    label: 'LEARN MORE',
    link: 'https://docs.opennms.com/'
  }
}
