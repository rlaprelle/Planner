import { ProgressBar } from './ProgressBar'

export default {
  title: 'UI/ProgressBar',
  component: ProgressBar,
  tags: ['autodocs'],
  argTypes: {
    value: { control: { type: 'range', min: 0, max: 10 } },
    max: { control: { type: 'number', min: 1 } },
  },
}

export const Empty = {
  args: { value: 0, max: 10 },
}

export const HalfFull = {
  args: { value: 5, max: 10 },
}

export const Full = {
  args: { value: 10, max: 10 },
}
