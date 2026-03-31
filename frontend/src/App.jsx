import { BrowserRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import * as Dialog from '@radix-ui/react-dialog'

const queryClient = new QueryClient()

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <div className="min-h-screen bg-gray-50 flex items-center justify-center">
          <div className="text-center">
            <h1 className="text-2xl font-bold text-gray-900">Planner</h1>
            <p className="mt-2 text-gray-500">ADHD-friendly daily work management</p>
            <Dialog.Root>
              <Dialog.Trigger asChild>
                <button className="mt-4 px-4 py-2 bg-indigo-600 text-white rounded hover:bg-indigo-700">
                  Open Dialog (Radix UI test)
                </button>
              </Dialog.Trigger>
              <Dialog.Portal>
                <Dialog.Overlay className="fixed inset-0 bg-black/50" />
                <Dialog.Content className="fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 bg-white p-6 rounded shadow-lg">
                  <Dialog.Title className="text-lg font-semibold">Ready to go</Dialog.Title>
                  <Dialog.Description className="mt-2 text-gray-600">
                    Scaffold verified: React 18 + Vite + Tailwind CSS + Radix UI + TanStack Query + React Router.
                  </Dialog.Description>
                  <Dialog.Close asChild>
                    <button className="mt-4 px-3 py-1 bg-gray-200 rounded hover:bg-gray-300">
                      Close
                    </button>
                  </Dialog.Close>
                </Dialog.Content>
              </Dialog.Portal>
            </Dialog.Root>
          </div>
        </div>
      </BrowserRouter>
    </QueryClientProvider>
  )
}

export default App
