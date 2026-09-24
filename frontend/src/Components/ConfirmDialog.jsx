/*
 * File: ConfirmDialog.jsx
 * Purpose: The one confirmation dialog every "are you sure" action in the
 *          console uses - cancelling a reservation, deleting a node or slot,
 *          holding a slot out of service, deactivating an account. It is
 *          Modal with the two-button footer and busy-label swap those actions
 *          all share, so adding one more confirmation is a few props rather
 *          than another hand-built dialog.
 * Author:  <your name>
 * Created: 2026
 */
import { Button, Modal } from './PageControls'

export default function ConfirmDialog({
  title,
  description,
  confirmLabel,
  pendingLabel,
  cancelLabel = 'Cancel',
  tone = 'danger',
  busy = false,
  onConfirm,
  onClose,
  children,
}) {
  return (
    <Modal title={title} description={description} onClose={onClose}>
      {children ? <div className="mb-5">{children}</div> : null}

      <div className="flex justify-end gap-2">
        <Button variant="secondary" onClick={onClose} disabled={busy}>
          {cancelLabel}
        </Button>
        <Button variant={tone} disabled={busy} onClick={onConfirm}>
          {busy ? (pendingLabel ?? `${confirmLabel}...`) : confirmLabel}
        </Button>
      </div>
    </Modal>
  )
}
