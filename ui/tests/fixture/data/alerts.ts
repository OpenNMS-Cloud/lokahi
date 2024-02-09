// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-nocheck
import casual from 'casual'
import { rndSeverity } from '../../helpers/random'

casual.define('alert', function () {
  return {
    acknowledged: casual.coin_flip,
    description: casual.description,
    lastUpdateTimeMs: casual.unix_time,
    firstEventTimeMs: casual.unix_time,
    location: casual.word,
    ruleNameList: [casual.word],
    policyNameList: [casual.word],
    severity: rndSeverity()
  }
})

casual.define('alertsList', function () {
  return [
    casual.alert,
    casual.alert,
    casual.alert,
    casual.alert,
    casual.alert,
    casual.alert,
    casual.alert,
    casual.alert,
    casual.alert,
    casual.alert
  ]
})

const alert = casual.alert
const alertsList = casual.alertsList

export { alert, alertsList }
