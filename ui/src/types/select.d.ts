import { ISelectItemType } from '@featherds/select/src/components/types'

export interface ISelectDropdown {
  label: string,
  options: ISelectItemType[],
  optionText: string
}
