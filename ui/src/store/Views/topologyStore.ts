import { defineStore } from 'pinia'
import { IdLabelProps } from '@/types'
import { Edges, Layouts, Node, Nodes } from 'v-network-graph'
import { DisplayType, ViewType } from '@/components/Topology/topology.constants'
import { formatTopologyGraphs, orderPowergridGraph } from '@/components/Topology/topology.helpers'
import { TopologyGraph, TopologyGraphList, NodePoint, SZLRequest, VerticesAndEdges } from '@/types/topology'

export interface State {
  isTopologyView: boolean // switch between geo-map and topology
  selectedView: string // map, d3, circle layout etc.
  selectedDisplay: string // nodes, powergrid etc.
  edges: Edges
  vertices: Nodes
  semanticZoomLevel: number
  isLeftDrawerOpen: boolean
  isRightDrawerOpen: boolean
  focusObjects: IdLabelProps[]
  layout: Record<string, NodePoint>
  defaultObjects: Node[]
  highlightFocusedObjects: boolean
  modalState: boolean
  nodeIcons: Record<string, string>
  topologyGraphs: TopologyGraphList[]
  topologyGraphsDisplay: TopologyGraphList
  topologyGraphsSubLayers: TopologyGraph[]
  container: string
  namespace: string
  idsWithSubLayers: string[]
}

export const useTopologyStore = defineStore('topologyStore', {
  state: () =>
    <State>{
      isTopologyView: false,
      selectedView: '',
      selectedDisplay: '',
      edges: {},
      vertices: {},
      semanticZoomLevel: 1,
      isLeftDrawerOpen: true,
      isRightDrawerOpen: false,
      focusObjects: [],
      layout: {},
      defaultObjects: [],
      highlightFocusedObjects: false,
      modalState: false,
      nodeIcons: {},
      topologyGraphs: [],
      topologyGraphsDisplay: {} as TopologyGraphList,
      topologyGraphsSubLayers: [],
      container: '',
      namespace: '',
      idsWithSubLayers: []
    },
  actions: {
    /**
     * Parses a response from one of many calls
     * that contain vertices and edges.
     *
     * Calls are either initial GET calls, or
     * POST calls with SZL/Focus
     *
     * @param resp VerticesAndEdges
     * @param context VuexContext
     *
     * Whether to add the edges or not.
     * We may not want to if they contain links to sublayer nodes that are unavailable on this response.
     * @param preventLinks boolean
     */
    parseVerticesAndEdges(resp: VerticesAndEdges, preventLinks = false) {
      const edges: Edges = {}
      const vertices: Nodes = {}

      if (!preventLinks) {
        for (const edge of resp.edges) {
          edges[edge.label] = { source: edge.source.id, target: edge.target.id }
        }
      }

      for (const vertex of resp.vertices) {
        vertices[vertex.id] = {
          name: vertex.label,
          id: vertex.id,
          tooltip: vertex.tooltipText,
          label: vertex.label,
          icon: 'generic_icon',
          namespace: vertex.namespace
        }
      }

      if (resp.defaultFocus && resp.defaultFocus.vertexIds.length) {
        const defaultIds = resp.defaultFocus.vertexIds.map((obj) => obj.id)

        if (defaultIds.length) {
          const defaultObjects = defaultIds.map((id) => vertices[id])
          this.defaultObjects = defaultObjects.filter((dos) => dos)
        }
      }

      if (resp.focus) {
        const defaultIds = resp.focus.vertices

        if (defaultIds.length) {
          const defaultObjects = defaultIds.map((id) => vertices[id])
          this.defaultObjects = defaultObjects.filter((dos) => dos)
        }
      }

      this.edges = edges
      this.vertices = vertices
      this.updateObjectFocusedProperty()
      this.updateVerticesIconPaths()
      this.updateSubLayerIndicator()
    },

    async getVerticesAndEdges() {
      // TODO: add GQL call to get the data
      const resp = { edges: [], vertices: [] } as VerticesAndEdges

      if (resp) {
        this.parseVerticesAndEdges(resp)
      }
    },

    async getTopologyGraphs() {
      // TODO: add GQL call to get the data
      const topologyGraphs = [] as TopologyGraphList[]
      this.topologyGraphs = topologyGraphs
    },

    async getTopologyGraphByContainerAndNamespace({ containerId, namespace }: Record<string, string>) {
      // TODO: add GQL call to get the data
      const topologyGraph = {} as VerticesAndEdges

      if (topologyGraph) {
        this.container = containerId
        this.namespace = namespace
        this.parseVerticesAndEdges(topologyGraph, true) // true to prevent adding edges here

        // save which ids have sublayers, to show indicator
        const idsWithSubLayers = topologyGraph.edges.map((edge) => edge.id.split('.')[0])
        this.idsWithSubLayers = idsWithSubLayers

        // set focus to the defaults
        this.replaceFocusObjects(this.defaultObjects)
      }
    },

    setSemanticZoomLevel(SML: number) {
      this.semanticZoomLevel = SML
      this.getObjectDataByLevelAndFocus()
    },

    async getObjectDataByLevelAndFocus() {
      let resp: false | VerticesAndEdges

      try {
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        const SZLRequest: SZLRequest = {
          semanticZoomLevel: this.semanticZoomLevel,
          verticesInFocus: this.focusObjects.map((obj) => obj.id)
        }

        // TODO: add GQL call to get the data
        resp = {} as VerticesAndEdges

        if (resp) {
          this.parseVerticesAndEdges(resp)
        }
      } catch (err) {
        // error handling
      }
    },

    changeIcon(nodeIdIconKey: Record<string, string>) {
      this.nodeIcons = { ...this.nodeIcons, ...nodeIdIconKey }
      this.updateVerticesIconPaths()
    },

    // map, d3, circle, etc.
    setSelectedView(view: string) {
      this.selectedView = view
      this.isTopologyView = view !== ViewType.map
    },

    // nodes, powergrid, etc.
    async setSelectedDisplay(display: string) {
      this.selectedDisplay = display

      const graphsToDisplay = this.getGraphsDisplay

      this.topologyGraphsDisplay = graphsToDisplay
      this.topologyGraphsSubLayers = graphsToDisplay.graphs

      if (graphsToDisplay.graphs?.length) {
        await this.getTopologyGraphByContainerAndNamespace({
          containerId: graphsToDisplay.id,
          namespace: graphsToDisplay.graphs[0].namespace
        })
      }
    },

    /**
     * Focus
     */
    replaceFocusObjects(objects: IdLabelProps[] | Node[]) {
      this.focusObjects = (objects as IdLabelProps[]).filter((id) => id)
      this.getObjectDataByLevelAndFocus()
    },

    addFocusObject(object: IdLabelProps | Node) {
      this.focusObjects = [...this.focusObjects, object as IdLabelProps]
      this.getObjectDataByLevelAndFocus()
    },

    removeFocusObject(nodeId: string) {
      this.focusObjects = this.focusObjects.filter((obj) => obj.id !== nodeId)
      this.getObjectDataByLevelAndFocus()
    },

    useDefaultFocus() {
      this.replaceFocusObjects(this.defaultObjects)
    },

    /**
     * Network graph custom property updates.
     * Run every time after parsing the vertices and edges.
     */

    // prop for whether object is focused or not
    updateObjectFocusedProperty() {
      const vertices = this.vertices
      const edges = this.edges

      try {
        const focusedIds = this.focusObjects.map((obj) => obj.id)

        for (const vertex of Object.values(vertices)) {
          if (focusedIds.includes(vertex.id)) {
            vertex.focused = true
          } else {
            vertex.focused = false
          }
        }

        for (const edge of Object.values(edges)) {
          if (focusedIds.includes(edge.target) && focusedIds.includes(edge.source)) {
            edge.focused = true
          } else {
            edge.focused = false
          }
        }

        this.vertices = vertices
        this.edges = edges
      } catch (err) {
        // error handling
      }
    },

    // icon path prop
    updateVerticesIconPaths() {
      const vertices = this.vertices
      const nodeIcons = this.nodeIcons

      for (const [id, iconKey] of Object.entries(nodeIcons)) {
        if (vertices[id]) {
          vertices[id]['icon'] = iconKey
        }
      }

      this.vertices = vertices
    },

    updateSubLayerIndicator() {
      const idsWithSubLayers = this.idsWithSubLayers
      const { graphs = [] }: TopologyGraphList = this.getGraphsDisplay
      const vertices = this.vertices

      for (const { namespace, index } of graphs) {
        for (const vertex of Object.values(vertices)) {
          // if vertex has sublayer and is within graph namespace
          if (idsWithSubLayers.includes(vertex.id) && vertex.namespace === namespace) {
            // add the the next layer object for the context nav
            const i = index ?? -1 // to ensure graph has index  property
            if (i >= 0) {
              vertex['subLayer'] = graphs[i + 1]
            }
          }
        }
      }

      this.vertices = vertices
    }
  },
  getters: {
    getCircleLayout(state: State): Record<string, NodePoint> {
      const centerY = 350
      const centerX = 350
      const radius = 250

      const vertexNames = Object.keys(state.vertices)
      const layout = {} as Record<string, NodePoint>

      for (let i = 0; i < vertexNames.length; i++) {
        layout[vertexNames[i]] = {
          x: Number((centerX + radius * Math.cos((2 * Math.PI * i) / vertexNames.length)).toFixed(0)),
          y: Number((centerY + radius * Math.sin((2 * Math.PI * i) / vertexNames.length)).toFixed(0))
        }
      }

      return layout
    },

    getLayout(state: State): Layouts {
      if (state.selectedView === ViewType.circle) {
        return {
          nodes: this.getCircleLayout
        }
      }

      return {} as Layouts
    },

    /**
     * Return topology display graphs, if available.
     * Otherwise return object with empty graphs array.
     *
     * API does not return proper layer order,
     * but the id is made up of proper ordered layer names.
     * This can be used during layer selection / context menu nav.
     *
     * @param state topology store
     * @returns TopologyGraphList object with its sub layers list, if any
     */
    getGraphsDisplay(state: State): TopologyGraphList {
      const topologyGraph: TopologyGraphList =
        formatTopologyGraphs(state.topologyGraphs).filter(({ type }) => type === state.selectedDisplay)[0] || {}

      let graph: TopologyGraphList = { graphs: [], id: 'N/A', label: 'N/A', type: 'N/A' }

      if (!topologyGraph.graphs?.length) return graph

      if (topologyGraph.type === DisplayType.powergrid) {
        graph = {
          ...topologyGraph,
          // ordering might no longer required since layer order from API response seems in good order
          ...orderPowergridGraph(topologyGraph.graphs, topologyGraph.id)
        }
      } else {
        graph = topologyGraph
      }

      return graph
    },

    getGraphs(state: State): TopologyGraphList[] {
      return formatTopologyGraphs(state.topologyGraphs)
    }
  }
})
