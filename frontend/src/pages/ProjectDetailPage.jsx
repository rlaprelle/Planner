import { useParams, Link } from 'react-router-dom'

export default function ProjectDetailPage() {
  const { projectId } = useParams()
  return (
    <div className="p-8">
      <Link to="/projects" className="text-sm text-indigo-600 hover:underline">← Back to Projects</Link>
      <h1 className="text-2xl font-semibold text-gray-900 mt-4">Project Tasks</h1>
      <p className="text-gray-500 mt-2">Coming soon.</p>
    </div>
  )
}
