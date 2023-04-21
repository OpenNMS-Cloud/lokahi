export default {
  Alerts: {
    title: 'Alert Status',
    timePeriod: '24hours'
  },
  TopApplications: {
    title: 'Top 10 Applications',
    redirectLink: 'Flows',
    timePeriod: '24hours',
    emptyTitle: 'Data being collected.',
    emptySubtitle: 'No applications data was found in last 24 hours..'
  },
  NetworkTraffic: {
    title: 'Total Network Traffic',
    redirectLink: 'Inventory',
    timePeriod: '24hours',
    emptyTitle: 'Data being collected.',
    emptySubtitle: 'No network traffic data was found in last 24 hours..'
  },
  MenuLabel: 'Quick Actions',
  MenuLinks: [
    {
      name: 'Add Discovery',
      link: 'Discovery'
    },
    {
      name: 'Add Monitoring Policy',
      link: 'Monitoring Policies'
    },
    {
      name: 'View Flows',
      link: 'Flows'
    }
  ]
}
