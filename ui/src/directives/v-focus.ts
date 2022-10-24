/**
 * Add `v-focus` to any  
 * FeatherInput to autofocus on mount
 */
const featherInputFocusDirective = {
  mounted(featherInput: HTMLElement) {
    const baseInput = featherInput.getElementsByTagName('input').item(0) as HTMLElement
    baseInput.focus()
  }
}
 
export default featherInputFocusDirective
