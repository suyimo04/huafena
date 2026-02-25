/** Field types supported by the questionnaire engine */
export type FieldType = 'SINGLE_CHOICE' | 'MULTI_CHOICE' | 'TEXT' | 'DATE' | 'NUMBER' | 'DROPDOWN'

export interface FieldOption {
  value: string
  label: string
}

export interface ConditionalLogicCondition {
  fieldKey: string
  operator: 'EQUALS' | 'NOT_EQUALS' | 'CONTAINS' | 'GREATER_THAN' | 'LESS_THAN' | 'IN' | 'NOT_IN'
  value: string | string[]
}

export interface ConditionalLogic {
  action: 'SHOW' | 'HIDE'
  logicOperator: 'AND' | 'OR'
  conditions: ConditionalLogicCondition[]
}

export interface ValidationRules {
  minLength?: number
  maxLength?: number
  pattern?: string
  min?: number
  max?: number
  minDate?: string
  maxDate?: string
  minSelect?: number
  maxSelect?: number
  customMessage?: string
}

export interface QuestionnaireField {
  key: string
  type: FieldType
  label: string
  required: boolean
  validationRules: ValidationRules | null
  conditionalLogic: ConditionalLogic | null
  options: FieldOption[] | null
}

export interface FieldGroup {
  name: string
  sortOrder: number
  fields: string[]
}

export interface QuestionnaireSchema {
  groups: FieldGroup[]
  fields: QuestionnaireField[]
}

/** Palette item representing a draggable field type */
export interface PaletteItem {
  type: FieldType
  label: string
  icon: string
}

/** All available field types for the palette */
export const FIELD_TYPE_LIST: PaletteItem[] = [
  { type: 'TEXT', label: '文本输入', icon: '📝' },
  { type: 'NUMBER', label: '数字输入', icon: '🔢' },
  { type: 'SINGLE_CHOICE', label: '单选题', icon: '🔘' },
  { type: 'MULTI_CHOICE', label: '多选题', icon: '☑️' },
  { type: 'DROPDOWN', label: '下拉选择', icon: '📋' },
  { type: 'DATE', label: '日期选择', icon: '📅' },
]

/** Create a default field with a unique key */
export function createDefaultField(type: FieldType, index: number): QuestionnaireField {
  const base: QuestionnaireField = {
    key: `field_${Date.now()}_${index}`,
    type,
    label: FIELD_TYPE_LIST.find((f) => f.type === type)?.label ?? '新字段',
    required: false,
    validationRules: null,
    conditionalLogic: null,
    options: null,
  }

  if (['SINGLE_CHOICE', 'MULTI_CHOICE', 'DROPDOWN'].includes(type)) {
    base.options = [
      { value: 'option1', label: '选项 1' },
      { value: 'option2', label: '选项 2' },
    ]
  }

  return base
}
