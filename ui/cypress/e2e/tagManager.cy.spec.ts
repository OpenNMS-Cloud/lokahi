// TODO: need to log in the app first
// /auth/realms/opennms/protocol/openid-connect/auth?client_id=horizon-stream&redirect_uri=http%3A%2F%2Flocalhost%3A8123%2F%3Ftheme%3Ddark&state=ae0e6368-f7e8-445d-b7c4-797c99f6649b&response_mode=fragment&response_type=code&scope=openid&nonce=9619fde3-eab7-4e31-8a5b-9786493c13cf
context('Tag Manager', () => {
  beforeEach(() => {
    cy.visit('/inventory')
  })

  it('Should have the header', () => {
    cy.findByLabelText('Network Inventory').should('exist')
    cy.findByRole('button', { name: 'tagging' }).click()
    cy.findByLabelText('Network Inventory').should('exist')
  })
})
