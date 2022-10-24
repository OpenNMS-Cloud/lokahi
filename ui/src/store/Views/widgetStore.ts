import { defineStore } from 'pinia'

const baseWidgetProps = {
  isWidget: true
}

export const useWidgetStore = defineStore('widgetStore', {
  state: () => ({
    deviceWidgetProps: baseWidgetProps,
    minionWidgetProps: baseWidgetProps,
    mapWidgetProps: baseWidgetProps,
    deviceStatusWidgetProps: baseWidgetProps
  })
})
