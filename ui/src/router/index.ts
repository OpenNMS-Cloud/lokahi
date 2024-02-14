import { createRouter, createWebHistory } from 'vue-router'
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
      path: '/discovery-selected/:id',
      name: 'Discovery Selected',
      component: () => import('@/containers/Discovery.vue')
    },
    {
      path: '/monitoring-policies',
      name: 'Monitoring Policies',
      component: () => import('@/containers/MonitoringPolicies.vue')
    },
    {
      path: '/monitoring-policies-selected/:id',
      name: 'Monitoring Policies Selected',
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
      path: '/node/:id',
      name: 'Node',
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
