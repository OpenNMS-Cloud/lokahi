import { createRouter, createWebHistory } from 'vue-router'
import Appliances from '@/containers/Appliances.vue'
import NodeStatus from '@/containers/NodeStatus.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/appliances',
      name: 'Appliances',
      component: Appliances
    },
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
      path: '/:pathMatch(.*)*', // catch other paths and redirect
      redirect: '/'
    }
    /* Hidden routes below are for development purposes only 
        TODO: need to make these avail only in dev local
    */
    /* {
      path: '/templates',
      name: 'Templates',
      component: () => import('../../templates/index.vue')
    },
    {
      path: '/templates/layout/headlineTwoColumns',
      name: 'HeadlineTwoColumns',
      component: () => import('../../templates/layout/headlineTwoColumns.vue')
    } */
  ]
})

export default router
