context('Login', () => {
  beforeEach(() => {
    cy.visit('/')
  })

  // not logged
  it('If not logged, Sign In form should display', () => {
    cy.get('header > h1').should('contain', 'Sign In')
    cy.get('form')
      .findByRole('button', { name: /Sign in/i })
      .should('exist')
  })

  // logging in
  it('If logged, welcome user heading should display', () => {
    cy.get('form')
      .findByPlaceholderText('Username')
      .click()
      .type('admin')
      .end()
      .findByPlaceholderText('Password')
      .click()
      .type('admin')
      .end()
      .findByRole('button', { name: /Sign in/i })
      .click()

    cy.get('[data-test="header-welcome"]').should('contain', 'Welcome, admin')
  })
})
