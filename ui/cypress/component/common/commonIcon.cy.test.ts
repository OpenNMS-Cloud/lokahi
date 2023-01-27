import Icon from '@/components/Common/Icon.vue'
import BubbleChart from '@material-design-icons/svg/outlined/bubble_chart.svg'

describe('Icon', () => {
  test('Should display an icon', () => {
    cy.mount(Icon, {
      props: {
        icon: {
          image: BubbleChart,
          title: 'Bubble Chart'
        }
      }
    })

    cy.get('svg').should('exist') // ok
    cy.get('svg.feather-icon').should('be.visible') // ok
  })
})
