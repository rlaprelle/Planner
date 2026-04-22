import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'

export function useAdminCrud({ queryKey, listFn, createFn, updateFn, deleteFn }) {
  const queryClient = useQueryClient()
  const [formOpen, setFormOpen] = useState(false)
  const [editItem, setEditItem] = useState(null)
  const [deleteItem, setDeleteItem] = useState(null)

  const { data = [], isLoading } = useQuery({ queryKey, queryFn: listFn })

  const createMutation = useMutation({
    mutationFn: createFn,
    onSuccess: () => { queryClient.invalidateQueries({ queryKey }); setFormOpen(false) },
  })
  const updateMutation = useMutation({
    mutationFn: ({ id, data }) => updateFn(id, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey }); setFormOpen(false); setEditItem(null) },
  })
  const deleteMutation = useMutation({
    mutationFn: deleteFn,
    onSuccess: () => { queryClient.invalidateQueries({ queryKey }); setDeleteItem(null) },
  })

  const openCreate = () => {
    createMutation.reset()
    updateMutation.reset()
    setEditItem(null)
    setFormOpen(true)
  }
  const openEdit = (row) => {
    createMutation.reset()
    updateMutation.reset()
    setEditItem(row)
    setFormOpen(true)
  }
  const openDelete = (row) => setDeleteItem(row)

  const handleFormClose = (open) => {
    setFormOpen(open)
    if (!open) {
      setEditItem(null)
      createMutation.reset()
      updateMutation.reset()
    }
  }
  const handleDeleteClose = (open) => { if (!open) setDeleteItem(null) }

  const handleSubmit = (formData) => {
    if (editItem) {
      updateMutation.mutate({ id: editItem.id, data: formData })
    } else {
      createMutation.mutate(formData)
    }
  }

  const confirmDelete = () => deleteMutation.mutate(deleteItem.id)

  const saveError = updateMutation.error || createMutation.error || null

  return {
    data, isLoading,
    formOpen, editItem, deleteItem,
    openCreate, openEdit, openDelete, setDeleteItem,
    handleFormClose, handleDeleteClose,
    handleSubmit, confirmDelete,
    isSaving: createMutation.isPending || updateMutation.isPending,
    isDeleting: deleteMutation.isPending,
    saveError,
  }
}
