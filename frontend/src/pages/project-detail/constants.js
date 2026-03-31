export const STATUS_OPTIONS = [
  { value: 'TODO', label: 'To Do' },
  { value: 'IN_PROGRESS', label: 'In Progress' },
  { value: 'BLOCKED', label: 'Blocked' },
  { value: 'DONE', label: 'Done' },
  { value: 'SKIPPED', label: 'Done for now' },
]

export const PRIORITY_OPTIONS = [
  { value: 1, label: '1 — Lowest' },
  { value: 2, label: '2 — Low' },
  { value: 3, label: '3 — Medium' },
  { value: 4, label: '4 — High' },
  { value: 5, label: '5 — Highest' },
]

export const POINTS_OPTIONS = [
  { value: 1, label: '1' },
  { value: 2, label: '2' },
  { value: 3, label: '3' },
  { value: 4, label: '4' },
  { value: 5, label: '5' },
]

export const ENERGY_OPTIONS = [
  { value: 'LOW', label: 'Low' },
  { value: 'MEDIUM', label: 'Medium' },
  { value: 'HIGH', label: 'High' },
]

export const DEADLINE_GROUP_LABELS = {
  TODAY: 'Today',
  THIS_WEEK: 'This Week',
  NO_DEADLINE: 'No Deadline',
}

// Priority dot colors
export const PRIORITY_DOT_COLORS = {
  1: 'bg-gray-300',
  2: 'bg-blue-400',
  3: 'bg-yellow-400',
  4: 'bg-orange-400',
  5: 'bg-red-500',
}

// Deadline badge colors
export const DEADLINE_BADGE_COLORS = {
  TODAY: 'bg-red-100 text-red-700',
  THIS_WEEK: 'bg-orange-100 text-orange-700',
  NO_DEADLINE: '',
}
