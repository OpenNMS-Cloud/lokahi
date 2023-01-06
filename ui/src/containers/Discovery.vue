<template>
  <div class="container">
    <PageHeader heading="Discovery" />
    <div>
      <!-- info bar -->
    </div>
    <div class="discovery-cards">
      <Card
        :title="'Active Discovery'"
        :imgUrl="ActiveDiscoveryImg"
      >
        <template v-slot:footer>
          <div class="footer">
            <p>Select tool(s) to configure your nodes to send logs to BTO</p>
            <ActiveDiscoveryToolsList @show-config-active-tool="showConfigActiveTool" />
          </div>
        </template>
      </Card>
      <Card
        :title="'Passive Discovery'"
        :imgUrl="PassiveDiscoveryImg"
      >
        <template v-slot:footer>
          <div class="footer">
            <p>Select tool(s) to configure your nodes to send logs to BTO</p>
            <div class="passive-tools">
              <PassiveTool
                :label="'Syslog'"
                @show-settings="() => showSettings('syslog')"
                @show-instructions="() => showInstructions('syslog')"
              />
              <PassiveTool
                :label="'SNMP Traps'"
                @show-settings="() => showSettings('snmp')"
                @show-instructions="() => showInstructions('snmp')"
              />
            </div>
          </div>
        </template>
      </Card>
    </div>
  </div>
</template>

<script setup lang="ts">
import ActiveDiscoveryImg from '@/assets/active-discovery.png'
import PassiveDiscoveryImg from '@/assets/passive-discovery.png'


const showSettings = (tool: string) => {
  console.log('show settings for', tool)
}


const showInstructions = (tool: string) => {
  console.log('show instructions for', tool)
}

const showConfigActiveTool = (tool: string) => {
  console.log('show setting for', tool)

}
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';

.container {
	display: flex;
	flex-direction: column;
	margin: 0 1rem;

	.discovery-cards {
		display: flex;
	}

	.footer {
		padding: var(variables.$spacing-m);
		padding-top: var(variables.$spacing-s);

		> p {
			margin-bottom: 10px;
		}
	}

  .passive-tools {
    display: flex;
    flex-direction: column;
    margin-top: 18px;
  }
}
</style>
