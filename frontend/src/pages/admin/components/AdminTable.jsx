import { useState } from 'react'

export function AdminTable({ columns, data, onEdit, onDelete, entityName }) {
  const [sortCol, setSortCol] = useState(null)
  const [sortDir, setSortDir] = useState('asc')

  const handleSort = (col) => {
    if (sortCol === col) {
      setSortDir(d => d === 'asc' ? 'desc' : 'asc')
    } else {
      setSortCol(col)
      setSortDir('asc')
    }
  }

  const sorted = sortCol
    ? [...data].sort((a, b) => {
        const va = a[sortCol] ?? ''
        const vb = b[sortCol] ?? ''
        const cmp = String(va).localeCompare(String(vb), undefined, { numeric: true })
        return sortDir === 'asc' ? cmp : -cmp
      })
    : data

  const truncateId = (id) => id ? String(id).slice(0, 8) : ''

  return (
    <div className="overflow-x-auto border border-gray-200 rounded-lg">
      <table className="w-full text-sm">
        <thead>
          <tr className="bg-gray-50 border-b border-gray-200">
            <th className="px-3 py-2 text-left text-xs font-semibold text-gray-600 cursor-pointer"
                onClick={() => handleSort('id')}>
              ID {sortCol === 'id' && (sortDir === 'asc' ? '\u2191' : '\u2193')}
            </th>
            {columns.map(col => (
              <th key={col.key}
                  className="px-3 py-2 text-left text-xs font-semibold text-gray-600 cursor-pointer"
                  onClick={() => handleSort(col.key)}>
                {col.label} {sortCol === col.key && (sortDir === 'asc' ? '\u2191' : '\u2193')}
              </th>
            ))}
            <th className="px-3 py-2 text-right text-xs font-semibold text-gray-600">Actions</th>
          </tr>
        </thead>
        <tbody>
          {sorted.length === 0 && (
            <tr>
              <td colSpan={columns.length + 2} className="px-3 py-8 text-center text-gray-400">
                No {entityName} found
              </td>
            </tr>
          )}
          {sorted.map((row, i) => (
            <tr key={row.id} className={i % 2 === 0 ? 'bg-white' : 'bg-gray-50/50'}>
              <td className="px-3 py-2 text-xs text-gray-400 font-mono cursor-pointer"
                  title={row.id}
                  onClick={() => navigator.clipboard.writeText(row.id)}>
                {truncateId(row.id)}
              </td>
              {columns.map(col => (
                <td key={col.key} className="px-3 py-2 text-gray-700 max-w-[200px] truncate">
                  {col.render ? col.render(row[col.key], row) : formatValue(row[col.key])}
                </td>
              ))}
              <td className="px-3 py-2 text-right space-x-2">
                <button onClick={() => onEdit(row)}
                        className="text-xs text-blue-600 hover:text-blue-800 font-medium">
                  Edit
                </button>
                <button onClick={() => onDelete(row)}
                        className="text-xs text-red-600 hover:text-red-800 font-medium">
                  Delete
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function formatValue(val) {
  if (val == null) return '\u2014'
  if (typeof val === 'boolean') return val ? 'Yes' : 'No'
  if (typeof val === 'string' && val.match(/^\d{4}-\d{2}-\d{2}T/)) {
    return new Date(val).toLocaleDateString()
  }
  return String(val)
}
