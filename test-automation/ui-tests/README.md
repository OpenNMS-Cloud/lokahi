# Integration Tests

## Running

### Locally
Tests can be run locally via IntelliJ, but require access to Azure - specifically: permissions to read secrets from key vault 
`automation-test-vault`. 

### CI/CD

Retrieve the Service Principal (SP) from Bitwarden (`opennms-automation-testing`). This SP has permissions to read from 
key vault `automation-test-vault`.

Set the environment variables as follows:

- AZURE_CLIENT_ID
- AZURE_TENANT_ID
- AZURE_CLIENT_SECRET
