export default {
  Discovery: {
    pageHeadline: 'Discoveries',
    button: {
      add: 'Add Discovery'
    },
    empty: 'No discoveries performed.'
  },
}

export const Instructions = {
  activeDiscoveryTitle: 'What is Active Discovery?',
  activeDiscoverySubtitle:
    'Active discovery queries nodes and cloud APIs to detect the entities that you want to monitor.',
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
    'Passive discovery uses SNMP traps to identify network devices. It does so by monitoring their activity through events, flows, and indirectly by evaluating other devices configuration settings.',
  passiveNote: 'Note that you can set only one passive discovery by location.',
  passiveListCharacteristics: {
    benefits: 'Benefits:',
    benefitsDescription: 'Low bandwidth consumption.',
    disadvantages: 'Disadvantages:',
    disadvantagesDescription:
      'May miss devices if they are not active. All devices must be enabled and configured to send Syslogs.'
  },
  learnMoreLink: {
    label: 'Visit our product documentation',
    link: 'https://docs.opennms.com/'
  }
}
