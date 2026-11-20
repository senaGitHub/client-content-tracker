/**
 * Toasts live outside every dialog and sheet, but they must stay usable while one is open (their
 * "Geri al" undoes a status change made in the edit sheet). Reka treats a press or focus there as
 * an "outside" interaction and would close the modal, so its outside events are cancelled when
 * they come from the toaster. Used by DialogContent and SheetContent.
 */
export function keepOpenOnToast(event: Event) {
  const original = (event as CustomEvent<{ originalEvent?: Event }>).detail?.originalEvent
  const target = (original?.target ?? event.target) as Element | null
  if (target instanceof Element && target.closest('[data-sonner-toaster]')) event.preventDefault()
}
