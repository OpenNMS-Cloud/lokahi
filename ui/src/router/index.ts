import { createRouter, createWebHistory } from 'vue-router'
import Appliances from '@/containers/Appliances.vue'
import { useWidgets, Widgets } from '@/composables/useWidgets' 

const { setAvailableWidgets } = useWidgets()

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'Appliances',
      component: Appliances,
      beforeEnter: () => {
        setAvailableWidgets(Widgets.GEOMAP)
      }
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
      beforeEnter: () => {
        setAvailableWidgets(Widgets.DEVICES, Widgets.MINIONS)
      },
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
      path: '/node/:id',
      name: 'Node',
      component: () => import('@/containers/NodeStatus.vue'),
      beforeEnter: () => {
        setAvailableWidgets(Widgets.TAGS)
      }
    },
    {
      path: '/:pathMatch(.*)*', // catch other paths and redirect
      redirect: '/'
    }
  ]
})

export default router
