/**
 * Shared mock data for the scheduling / start-day flow.
 *
 * Used by Storybook stories (via MSW handlers). If E2E tests ever need the
 * same fixture data, they can import it here too and wrap it in page.route()
 * callbacks — same shape, different mock mechanism.
 *
 * Keep the response shapes in sync with the real API contracts in:
 *   backend/src/main/java/com/echel/planner/backend/schedule/
 *   backend/src/main/java/com/echel/planner/backend/event/
 *   backend/src/main/java/com/echel/planner/backend/task/
 */

export const fakeProjects = {
  writing: { id: 'proj-writing', name: 'Writing', color: '#9B89B8' },
  ops: { id: 'proj-ops', name: 'Ops & Admin', color: '#B8A6CF' },
  research: { id: 'proj-research', name: 'Research', color: '#7C6B9E' },
}

/**
 * TaskResponse shape matches backend/.../task/TaskResponse.java:
 *   id, title, projectId, projectName, projectColor, pointsEstimate,
 *   deadlineGroup ('TODAY' | 'THIS_WEEK' | null), energyLevel, etc.
 */
export const fakeSuggestedTasks = [
  {
    id: 'task-1',
    title: 'Draft Q2 product memo',
    projectId: fakeProjects.writing.id,
    projectName: fakeProjects.writing.name,
    projectColor: fakeProjects.writing.color,
    pointsEstimate: 3,
    deadlineGroup: 'TODAY',
    energyLevel: 'HIGH',
  },
  {
    id: 'task-2',
    title: 'Review peer feedback on chapter 4',
    projectId: fakeProjects.writing.id,
    projectName: fakeProjects.writing.name,
    projectColor: fakeProjects.writing.color,
    pointsEstimate: 2,
    deadlineGroup: 'THIS_WEEK',
    energyLevel: 'MEDIUM',
  },
  {
    id: 'task-3',
    title: 'Outline next blog post',
    projectId: fakeProjects.writing.id,
    projectName: fakeProjects.writing.name,
    projectColor: fakeProjects.writing.color,
    pointsEstimate: 1,
    deadlineGroup: null,
    energyLevel: 'LOW',
  },
  {
    id: 'task-4',
    title: 'Reconcile credit card statement',
    projectId: fakeProjects.ops.id,
    projectName: fakeProjects.ops.name,
    projectColor: fakeProjects.ops.color,
    pointsEstimate: 1,
    deadlineGroup: 'TODAY',
    energyLevel: 'LOW',
  },
  {
    id: 'task-5',
    title: 'Renew domain registration',
    projectId: fakeProjects.ops.id,
    projectName: fakeProjects.ops.name,
    projectColor: fakeProjects.ops.color,
    pointsEstimate: 1,
    deadlineGroup: 'THIS_WEEK',
    energyLevel: 'LOW',
  },
  {
    id: 'task-6',
    title: 'Read "Attention Is All You Need"',
    projectId: fakeProjects.research.id,
    projectName: fakeProjects.research.name,
    projectColor: fakeProjects.research.color,
    pointsEstimate: 3,
    deadlineGroup: null,
    energyLevel: 'HIGH',
  },
  {
    id: 'task-7',
    title: 'Summarize lit review notes',
    projectId: fakeProjects.research.id,
    projectName: fakeProjects.research.name,
    projectColor: fakeProjects.research.color,
    pointsEstimate: 2,
    deadlineGroup: 'THIS_WEEK',
    energyLevel: 'MEDIUM',
  },
  {
    id: 'task-8',
    title: 'Explore graph neural net tutorial',
    projectId: fakeProjects.research.id,
    projectName: fakeProjects.research.name,
    projectColor: fakeProjects.research.color,
    pointsEstimate: 2,
    deadlineGroup: null,
    energyLevel: 'MEDIUM',
  },
]

export const emptySchedule = []

export const emptyEvents = []

/**
 * Helpers for building fixture events / blocks for stories that need them.
 * Not used by the empty-day story but kept here for future variants.
 */
export function fakeEvent({ id, title, startTime, endTime, blockDate, project = fakeProjects.ops }) {
  return {
    id,
    blockDate,
    startTime,
    endTime,
    title,
    projectId: project.id,
    projectName: project.name,
    projectColor: project.color,
    energyLevel: null,
  }
}
