import WidgetBar from '@/components/Layout/Widgetbar.vue'
import { useWidgets, Widgets } from '@/composables/useWidgets'
import router from '@/router'
import setup from '../setupWrapper'

const { addWidget, removeWidget } = useWidgets()

const wrapper = setup({ 
  component: WidgetBar,
  global: {
    plugins: [router]
  },
  attachTo: document.body
})

const widgets = [
  ['Minion', Widgets.MINIONS],
  ['Device', Widgets.DEVICES],
  ['Map', Widgets.GEOMAP],
  ['Tag', Widgets.TAGS]
]

test.each(widgets)('Mounts and unmounts the "%s" widget properly', (_, widget) => {
  const element = wrapper.get('#widget-flex-container').element as HTMLElement

  // add the widget
  addWidget(wrapper.getCurrentComponent().appContext, widget, element)

  // test if it was added properly
  let renderedWidget = wrapper.find(`#${widget}`)
  expect(renderedWidget.exists()).toBe(true)
  
  // remove widget
  removeWidget(renderedWidget.element)

  // test if it was removes
  renderedWidget = wrapper.find(`#${widget}`)
  expect(renderedWidget.exists()).toBe(false)
})
