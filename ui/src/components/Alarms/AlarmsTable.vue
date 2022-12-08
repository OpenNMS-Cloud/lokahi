<template>
  <div class="container">
    <p>Alarms</p>
    <div class="feather-row">
      <div class="feather-col-12">
        <FeatherButton primary @click="trigger">Send Alarm</FeatherButton>
        <FeatherButton secondary @click="clear">Clear Alarm</FeatherButton>
      </div>
    </div>
    <div class="feather-row">
      <div class="feather-col-12">
        <table aria-label="Alarms Table">
          <thead>
            <tr>
              <th>Severity</th>
              <th>Description</th>
              <th>Last Event Time</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="alarm in alarmsQueries.alarms" :key="alarm?.id || ''">
              <td>
                <div class="severity">
                  <div :class="alarm?.severity?.toLowerCase()" class="status-box"></div>
                  {{ alarm?.severity }}
                </div>
              </td>
              <td>{{ alarm?.description }}</td>
              <td v-date>{{ alarm?.lastEventTime }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { getMockEvent } from '@/types/mocks'
import { useAlarmMutations } from '@/store/Mutations/alarmMutations'
import { useEventMutations } from '@/store/Mutations/eventMutations'
import { useAlarmsQueries } from '@/store/Queries/alarmsQueries'
import { AlarmAckDtoInput } from '@/types/graphql'
import useSpinner from '@/composables/useSpinner'
import { AlarmDto } from '@/types/graphql'

const alarmsQueries = useAlarmsQueries()
const eventMutations = useEventMutations()
const alarmMutations = useAlarmMutations()
const { startSpinner, stopSpinner } = useSpinner()

const trigger = async () => {
  startSpinner()
  await eventMutations.createEvent({ event: getMockEvent() })
  setTimeout(async () => {
    await alarmsQueries.fetch()
    stopSpinner()
  }, 350)
}

const clear = async () => {
  startSpinner()
  const promises = alarmsQueries.alarms.map((alarm) => 
    alarmMutations.clearAlarm({ id: alarm?.id, ackDTO: { user: 'admin' } as AlarmAckDtoInput }))

  await Promise.all(promises)
  setTimeout(async () => {
    await alarmsQueries.fetch()
    stopSpinner()
  }, 350)
}
</script>

<style lang="scss" scoped>
@use "@featherds/styles/themes/variables";
@use "@featherds/styles/mixins/typography";
@use "@featherds/styles/mixins/elevation";
@use "@featherds/table/scss/table";

.container {
  @include elevation.elevation(1);
  background: var(variables.$surface);
  padding: 10px 30px 30px 30px;
  width: 700px;
  margin: auto;
  margin-top: 10px;

  p {
    @include typography.headline1;
    margin-bottom: 24px;
  }

  table {
    @include table.table;

    .severity {
      display: flex;
      .status-box {
        display: block;
        width: 12px;
        height: 12px;
        margin: 5px 10px 0px -10px;

        &.cleared {
          background: var(variables.$success);
        }
        &.warning {
          background: var(variables.$warning);
        }
      }
    }
  }
}
</style>
