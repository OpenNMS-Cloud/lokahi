import { createRouter, createWebHistory } from 'vue-router'
import Appliances from '@/containers/Appliances.vue'
import NodeStatus from '@/containers/NodeStatus.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'Appliances',
      component: Appliances
    },
    {
      path: '/dashboard', // Temporary route - will change to '/' once complete.
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
      component: () => import('@/containers/Map.vue'),
      children: [
        {
          path: '',
          name: 'MapAlarms',
          component: () => import('@/components/Map/MapAlarmsGrid.vue')
        },
        {
          path: 'nodes',
          name: 'MapNodes',
          component: () => import('@/components/Map/MapNodesGrid.vue')
        }
      ]
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
      path: '/my-discovery',
      name: 'My Discovery',
      component: () => import('@/containers/MyDiscovery.vue')
    },
    {
      path: '/monitoring-policies',
      name: 'Monitoring Policies',
      component: () => import('@/containers/MonitoringPolicies.vue')
    },
    {
      path: '/synthetic-transactions',
      name: 'Synthetic Transactions',
      component: () => import('@/containers/SyntheticTransactions.vue')
    },
    {
      path: '/node/:id',
      name: 'Node',
      component: NodeStatus
    },
    {
      path: '/:pathMatch(.*)*', // catch other paths and redirect
      redirect: '/'
    }
  ]
})

export default router
