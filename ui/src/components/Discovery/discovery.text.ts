export default {
  Discovery: {
    pageHeadline: 'Discovery',
    headline1: 'Select a discovery',
    headline2: 'ICMP/SNMP Discovery Setup',
    nameInputLabel: 'ICMP/SNMP name',
    noneDiscoverySelectedMsg: 'Select a discovery to get started',
    button: {
      add: 'New Discovery',
      cancel: 'Cancel',
      submit: 'Save discovery'
    }
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
      label: 'Enter community string (optional)'
    },
    UDPPort: {
      label: 'Enter UDP port (optional)'
    }
  },
  Infobar: {
    title: 'With Zero Touch Provisioning, BTO discovers your network actively and passively',
    text1: 'We can queue nodes for discovery',
    text2: 'You can configure your nodes to deliver notifications to BTO',
    text3: 'You can customize how BTO discovers your network using '
  },
  Instructions: {
    title: 'Discovery Instructions',
    subtitle: 'Select a location (total: 11) to review specific guidelines'
  }
}

export const DiscoverySyslogSNMPTrapsForm = {
  headline: 'ICMP/SNMP Discovery Setup',
  location: 'Search a location',
  tag: 'Search/Add tags (optional)',
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

export const Azure = {
  title: 'Azure Discovery Setup',
  saveBtnText: 'Save Discovery',
  cancelBtnText: 'Cancel'
}

export const SuccessModalOptions = {
  successMsg: 'setup successfully!',
  title: 'You may be interested in...',
  viewNodes: 'View Detected Nodes',
  addDiscovery: 'Add Another Discovery',
  addTransaction: 'Add Synthetic Transaction',
  addMonitoring: 'Add Monitoring Policy',
  checkboxText: "Don't show this again"
}
