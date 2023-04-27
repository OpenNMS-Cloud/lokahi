import mountWithPiniaVillus from 'tests/mountWithPiniaVillus'
import Graphs from '@/containers/Graphs.vue'
import { downloadCanvas, downloadMultipleCanvases } from '@/components/Graphs/utils'
import { createRouter, createWebHistory } from 'vue-router'
import { RouterLinkStub } from '@vue/test-utils'

// mock the route param
const mockRouter = createRouter({ history: createWebHistory(), routes: [] })
mockRouter.currentRoute.value.params = { id: '1' }

const mockCanvas = {
  offsetWidth: 100,
  offsetHeight: 100,
  toDataURL: () => '',
  getContext: (...p: any) => {
    return {
      drawImage: (...p: any) => {}
    } as CanvasRenderingContext2D
  }
} as HTMLCanvasElement

vi.mock('jspdf', () => {
  class MockJsPdf {
    val1: string
    val2: string
    internal: object
    constructor(val1: string, val2: string) {
      this.val1 = val1
      this.val2 = val2
      this.internal = {
        pageSize: {
          height: 200,
          width: 200
        }
      }
    }
    addImage() {}
    getImageProperties() {
      return { width: 100, height: 100 }
    }
    save() {}
  }

  return {
    default: MockJsPdf
  }
})

const wrapper = mountWithPiniaVillus({
  component: Graphs,
  global: {
    plugins: [mockRouter],
    stubs: {
      RouterLink: RouterLinkStub
    }
  }
})

test('The Graphs container mounts correctly', () => {
  expect(wrapper).toBeTruthy()
})

test('The downloadCanvas function runs without errors', () => {
  const mock = vi.fn(downloadCanvas)
  mock(mockCanvas, 'filename')
  expect(mock).toHaveReturned()
})

test('The downloadMultipleCanvases function runs without errors', () => {
  const spy = vi.spyOn(document, 'createElement')
  spy.mockImplementationOnce(() => mockCanvas)
  const mock = vi.fn(downloadMultipleCanvases)
  mock(mockCanvas, [mockCanvas] as unknown as HTMLCollectionOf<HTMLCanvasElement>)
  expect(mock).toHaveReturned()
})
