import { AlertCountByType, Severity } from '@/types/graphql'

export const getCountMap = (types: AlertCountByType[] = [], severities: Severity[] = [], statuses: string[] = []) => {
  const map: any = {}
  const severitiesAndStatuses = [...severities, ...statuses]

  for (const type of types) {
    for (const item of severitiesAndStatuses) {
      if (type.countType?.split('_')[1].toUpperCase() === item.toUpperCase()) {
        map[item] = type.count
      }
    }
  }

  return map
}
