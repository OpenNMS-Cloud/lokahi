import { createRouter, createWebHistory } from 'vue-router'
import NodeDetails from '@/containers/NodeDetails.vue'
import NodeStatus from '@/containers/NodeStatus.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'Dashboard',
      component: () => import('@/containers/Dashboard.vue')
    },
    {
      path: '/graphs/:id',
      name: 'Graphs',
      component: () => import('@/containers/Graphs.vue')
    },
    {
      path: '/map',
      name: 'Map',
      component: () => import('@/containers/Map.vue')
    },
    {
      path: '/inventory',
      name: 'Inventory',
      component: () => import('@/containers/Inventory.vue')
    },
    {
      path: '/discovery',
      name: 'Discovery',
      component: () => import('@/containers/Discovery.vue')
    },
    {
      path: '/discovery/:id',
      name: 'Discovery Selected',
      component: () => import('@/containers/Discovery.vue')
    },
    {
      path: '/monitoring-policies',
      name: 'Monitoring Policies',
      component: () => import('@/containers/MonitoringPoliciesLegacy.vue')
    },
    {
      path: '/monitoring-policies/:id',
      name: 'Monitoring Policies Selected',
      component: () => import('@/containers/MonitoringPoliciesLegacy.vue')
    },
    {
      path: '/monitoring-policies-new',
      name: 'Monitoring Policies New',
      component: () => import('@/containers/MonitoringPolicies.vue')
    },
    {
      path: '/monitoring-policies-new/:id',
      name: 'Monitoring Policies New Selected',
      component: () => import('@/containers/MonitoringPolicies.vue')
    },
    {
      path: '/synthetic-transactions',
      name: 'Synthetic Transactions',
      component: () => import('@/containers/SyntheticTransactions.vue')
    },
    {
      path: '/alerts',
      name: 'Alerts',
      component: () => import('@/containers/Alerts.vue')
    },
    {
      path: '/locations',
      name: 'Locations',
      component: () => import('@/containers/Locations.vue')
    },
    {
      // older node page
      path: '/node/:id',
      name: 'Node',
      component: NodeDetails
    },
    {
      path: '/node-status/:id',
      name: 'Node Status',
      component: NodeStatus
    },
    {
      path: '/flows',
      name: 'Flows',
      component: () => import('@/containers/Flows.vue')
    },
    {
      path: '/welcome',
      name: 'Welcome',
      component: () => import('@/containers/Welcome.vue')
    },
    {
      path: '/:pathMatch(.*)*', // catch other paths and redirect
      redirect: '/'
    }
  ]
})

export default router
