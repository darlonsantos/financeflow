import toast from 'react-hot-toast'

/**
 * Hook simplificado para toasts customizados
 */
export const useToast = () => {
  const success = (message: string) => {
    toast.success(message, {
      duration: 3000,
      position: 'top-right',
    })
  }

  const error = (message: string) => {
    toast.error(message, {
      duration: 4000,
      position: 'top-right',
    })
  }

  const info = (message: string) => {
    toast(message, {
      icon: 'ℹ️',
      duration: 3000,
      position: 'top-right',
    })
  }

  const loading = (message: string) => {
    return toast.loading(message, {
      position: 'top-right',
    })
  }

  const dismiss = (toastId?: string) => {
    if (toastId) {
      toast.dismiss(toastId)
    } else {
      toast.dismiss()
    }
  }

  const promise = <T,>(
    promise: Promise<T>,
    messages: {
      loading: string
      success: string
      error: string
    }
  ) => {
    return toast.promise(
      promise,
      {
        loading: messages.loading,
        success: messages.success,
        error: messages.error,
      },
      {
        position: 'top-right',
      }
    )
  }

  return {
    success,
    error,
    info,
    loading,
    dismiss,
    promise,
  }
}

export default useToast
