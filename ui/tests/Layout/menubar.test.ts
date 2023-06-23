import mountWithPiniaVillus from 'tests/mountWithPiniaVillus'
import Menubar from '@/components/Layout/Menubar.vue'
import useKeycloak from '@/composables/useKeycloak'
import useTheme from '@/composables/useTheme'
import Keycloak from 'keycloak-js'

test('The menubar mounts', () => {
  const wrapper = mountWithPiniaVillus({
    component: Menubar,
    shallow: false
  })
  expect(wrapper).toBeTruthy()
})

// Skipped until after EAR
test.skip('The toggle dark btn triggers the composible function, and the ref updates', async () => {
  // mock authenticate to show dark/light mode btn
  const { setKeycloak } = useKeycloak()
  setKeycloak({ authenticated: true } as Keycloak)

  // get theme hook, mock on change theme callback
  const theme = useTheme()
  const unchangedThemeValue = theme.isDark.value
  const mockFn = vi.fn(() => 'this is a mock callback')
  theme.onThemeChange(mockFn)

  // get and trigger dark/light mode btn
  const wrapper = mountWithPiniaVillus({
    component: Menubar,
    shallow: false
  })
  const toggleDarkBtn = wrapper.get('[data-test="toggle-dark"]')
  await toggleDarkBtn.trigger('click')

  // expect theme callback to have run and isDark value to have changes
  expect(mockFn).toHaveBeenCalledOnce()
  expect(theme.isDark.value).toBe(!unchangedThemeValue)
})
