<template>
<div id="widget-bar">
  <div id="widget-flex-container">
    <FeatherCheckboxGroup label="Available Widgets">
      <FeatherCheckbox 
        v-for="(widget, index) in widgets"
        :key="index"
        v-model="widgetsRef[widget]" 
        @update:modelValue="(val: boolean | undefined) => triggerWidget(val, widget)">
          {{ getWidgetNameByIndex(widget) }}
      </FeatherCheckbox>

    </FeatherCheckboxGroup>
  </div>
</div>
</template>

<script setup lang="ts">
import { AppContext } from 'vue'
import { useWidgets, Widgets } from '@/composables/useWidgets'

const { widgets, addWidget, removeWidget, displayedWidgets } = useWidgets()
const widgetsRef = ref<any>({})
const widgetContainer = () => document.getElementById('widget-flex-container') as HTMLDivElement
const instance = getCurrentInstance()?.appContext as AppContext

const triggerWidget = (val: boolean | undefined, widget: string) => {
  if (val) {
    if (widget === Widgets.GEOMAP) {
      addWidget(instance, widget, widgetContainer(), {}, { height: '400px' })
    } else {
      addWidget(instance, widget, widgetContainer())
    }
  }
  else removeWidget(widget)
}

// returns a name based on the widget enum key
const getWidgetNameByIndex  = (widget: string) => {
  const index = Object.values(Widgets).indexOf(widget as unknown as Widgets)
  const name = Object.keys(Widgets)[index].toLocaleLowerCase()
  return name.charAt(0).toUpperCase() + name.slice(1)
}

// when changing routes, rm displayed widgets
// that are not available on that route
watchEffect(() => {
  for (const displayedWidget of displayedWidgets.value) {
    if (!widgets.value.includes(displayedWidget)) {
      removeWidget(displayedWidget)
      widgetsRef.value[displayedWidget] = false
    }
  }
})
</script>

<style scoped lang="scss">
@import "@featherds/styles/themes/variables";

#widget-bar {
  display: block;
  width: 500px;
  height: calc(100vh - 62px);
  border-left: 3px solid var($primary);

  #widget-flex-container {
    display: flex;
    flex-direction: column;
    margin-top: 10px;
    padding: 10px;
    gap: 10px;
  }
}
</style>