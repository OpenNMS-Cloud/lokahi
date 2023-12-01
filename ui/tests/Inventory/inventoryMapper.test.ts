import { InventoryMapper } from '@/mappers'
import { MonitoredStates } from '@/types'

describe('Inventory Mapper Tests', () => {

  test('The inventory mapper can convert a server response properly', () => {
    const output = InventoryMapper.fromServer([{
      id:1,
      monitoredState:MonitoredStates.MONITORED,
      monitoringLocationId:1,
      nodeLabel:'192.168.1.1',
      scanType:'DISCOVERY_SCAN',
      nodeAlias: 'alias',
      ipInterfaces:[
        {
          id:1,
          ipAddress:'192.168.1.1',
          nodeId:1,
          snmpPrimary:true
        }
      ],
      location:{id:1,location:'default'},
      tags: [{id:1,name:'default'}]
    }],{status:'success',data:{result:[
      {
        metric:{__name__:'response_time_msec',instance:'192.168.1.1',location_id:'1',monitor:'ICMP',node_id:'1',system_id:'default'},value:[12213123,0.423],values:[]}
    ],resultType:'vector'}})

    expect(output).toEqual({nodes:[{id:1,
        
      location: {id:1,location:'default'},
      metrics:{metric:{__name__:'response_time_msec','instance':'192.168.1.1','location_id':'1',monitor:'ICMP',node_id:'1',system_id:'default'},value:[12213123,0.423],values:[]},tags:[{id:1,name:'default'}],
      monitoredState:MonitoredStates.MONITORED,monitoringLocationId:1,nodeLabel:'192.168.1.1', nodeAlias: 'alias',scanType: 'DISCOVERY_SCAN',ipInterfaces:[{id:1,ipAddress:'192.168.1.1',nodeId:1,snmpPrimary:true}]}]})
  })

})