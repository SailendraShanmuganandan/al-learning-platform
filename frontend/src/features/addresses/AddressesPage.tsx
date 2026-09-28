import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { z } from 'zod/v4';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  MapPin, Plus, Trash2, Edit3, Star, Loader2, X,
  CheckCircle2, AlertTriangle, ArrowLeft, User as UserIcon,
} from 'lucide-react';
import {
  listAddresses,
  createAddress,
  updateAddress,
  deleteAddress,
  setDefaultAddress,
  type AddressResponse,
} from '../../api/userApi';
import type { ApiError } from '../../api/authApi';

// ---------------------------------------------------------------------------
// Schema
// ---------------------------------------------------------------------------

const addressSchema = z.object({
  recipientName: z.string().min(1, 'Recipient name is required').max(150),
  phone: z.string().regex(/^\+[1-9]\d{6,14}$/, 'Phone must be in E.164 format (e.g. +911234567890)'),
  addressLine1: z.string().min(1, 'Address line 1 is required').max(255),
  addressLine2: z.string().max(255).optional().or(z.literal('')),
  city: z.string().min(1, 'City is required').max(100),
  district: z.string().min(1, 'District is required').max(100),
  postalCode: z.string().min(1, 'Postal code is required').max(20),
  isDefault: z.boolean().optional(),
});

type AddressFormValues = z.infer<typeof addressSchema>;

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------

type FormMode = 'add' | 'edit';

export default function AddressesPage() {
  const [addresses, setAddresses] = useState<AddressResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  // Form panel state
  const [formOpen, setFormOpen] = useState(false);
  const [formMode, setFormMode] = useState<FormMode>('add');
  const [editingAddress, setEditingAddress] = useState<AddressResponse | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [formSubmitting, setFormSubmitting] = useState(false);

  // Delete confirmation state
  const [deleteTarget, setDeleteTarget] = useState<AddressResponse | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  // Success toast
  const [toast, setToast] = useState<string | null>(null);

  const showToast = (msg: string) => {
    setToast(msg);
    setTimeout(() => setToast(null), 3500);
  };

  // -------------------------------------------------------------------------
  // Load addresses
  // -------------------------------------------------------------------------

  const loadAddresses = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      const data = await listAddresses();
      setAddresses(data);
    } catch (err) {
      const apiErr = err as ApiError;
      setLoadError(apiErr?.message ?? 'Failed to load addresses.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadAddresses(); }, [loadAddresses]);

  // -------------------------------------------------------------------------
  // Form
  // -------------------------------------------------------------------------

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<AddressFormValues>({
    resolver: zodResolver(addressSchema),
    defaultValues: { recipientName: '', phone: '', addressLine1: '', addressLine2: '', city: '', district: '', postalCode: '', isDefault: false },
  });

  function openAdd() {
    setFormMode('add');
    setEditingAddress(null);
    reset({ recipientName: '', phone: '', addressLine1: '', addressLine2: '', city: '', district: '', postalCode: '', isDefault: false });
    setFormError(null);
    setFormOpen(true);
  }

  function openEdit(addr: AddressResponse) {
    setFormMode('edit');
    setEditingAddress(addr);
    reset({
      recipientName: addr.recipientName,
      phone: addr.phone,
      addressLine1: addr.addressLine1,
      addressLine2: addr.addressLine2 ?? '',
      city: addr.city,
      district: addr.district,
      postalCode: addr.postalCode,
      isDefault: addr.isDefault,
    });
    setFormError(null);
    setFormOpen(true);
  }

  function closeForm() {
    setFormOpen(false);
    setEditingAddress(null);
    setFormError(null);
  }

  async function onSubmit(values: AddressFormValues) {
    setFormSubmitting(true);
    setFormError(null);
    try {
      const payload = {
        recipientName: values.recipientName,
        phone: values.phone,
        addressLine1: values.addressLine1,
        addressLine2: values.addressLine2 ?? '',
        city: values.city,
        district: values.district,
        postalCode: values.postalCode,
        isDefault: values.isDefault ?? false,
      };

      if (formMode === 'add') {
        await createAddress(payload);
        showToast('Address added successfully!');
      } else if (editingAddress) {
        await updateAddress(editingAddress.addressId, payload);
        showToast('Address updated successfully!');
      }
      closeForm();
      await loadAddresses();
    } catch (err) {
      const apiErr = err as ApiError;
      setFormError(apiErr?.message ?? 'Failed to save address.');
    } finally {
      setFormSubmitting(false);
    }
  }

  // -------------------------------------------------------------------------
  // Set default
  // -------------------------------------------------------------------------

  async function handleSetDefault(addr: AddressResponse) {
    try {
      await setDefaultAddress(addr.addressId, addr);
      showToast(`"${addr.recipientName}" is now the default address.`);
      await loadAddresses();
    } catch (err) {
      const apiErr = err as ApiError;
      showToast(apiErr?.message ?? 'Failed to set default address.');
    }
  }

  // -------------------------------------------------------------------------
  // Delete
  // -------------------------------------------------------------------------

  async function confirmDelete() {
    if (!deleteTarget) return;
    setDeleting(true);
    setDeleteError(null);
    try {
      await deleteAddress(deleteTarget.addressId);
      showToast('Address deleted.');
      setDeleteTarget(null);
      await loadAddresses();
    } catch (err) {
      const apiErr = err as ApiError;
      setDeleteError(apiErr?.message ?? 'Failed to delete address.');
    } finally {
      setDeleting(false);
    }
  }

  // -------------------------------------------------------------------------
  // Render
  // -------------------------------------------------------------------------

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <header className="border-b border-border bg-card px-6 py-4 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div
            className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary text-primary-foreground text-xs font-bold"
            aria-hidden
          >
            AL
          </div>
          <span className="font-semibold text-foreground">AL Learning Platform</span>
        </div>
        <div className="flex items-center gap-3">
          <Link
            to="/dashboard"
            className="flex items-center gap-1.5 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-foreground hover:bg-muted transition"
          >
            <ArrowLeft size={13} /> Dashboard
          </Link>
          <Link
            to="/profile"
            id="nav-profile"
            className="flex items-center gap-1.5 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-foreground hover:bg-muted transition"
          >
            <UserIcon size={13} /> Profile
          </Link>
        </div>
      </header>

      {/* Toast */}
      {toast && (
        <div
          role="status"
          aria-live="polite"
          className="fixed top-4 right-4 z-50 flex items-center gap-2 rounded-xl border border-green-500/30 bg-green-500/10 px-4 py-3 text-sm font-medium text-green-800 shadow-lg"
        >
          <CheckCircle2 size={16} className="text-green-600" />
          {toast}
        </div>
      )}

      <main className="max-w-3xl mx-auto px-6 py-10">
        {/* Page header */}
        <div className="mb-8 flex items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-foreground">Delivery Addresses</h1>
            <p className="text-sm text-muted-foreground mt-1">Manage your saved delivery addresses.</p>
          </div>
          <button
            id="add-address-btn"
            type="button"
            onClick={openAdd}
            className="flex items-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground shadow hover:opacity-90 transition"
          >
            <Plus size={15} /> Add Address
          </button>
        </div>

        {/* Loading */}
        {loading && (
          <div className="flex items-center justify-center py-20">
            <Loader2 className="animate-spin text-primary" size={32} />
          </div>
        )}

        {/* Load error */}
        {!loading && loadError && (
          <div className="rounded-xl border border-destructive/40 bg-destructive/10 px-4 py-6 text-sm text-destructive text-center">
            {loadError}
          </div>
        )}

        {/* Empty state */}
        {!loading && !loadError && addresses.length === 0 && (
          <div className="flex flex-col items-center justify-center py-20 gap-4 text-center">
            <div className="flex h-16 w-16 items-center justify-center rounded-full bg-muted text-muted-foreground">
              <MapPin size={32} />
            </div>
            <h2 className="text-lg font-semibold text-foreground">No addresses yet</h2>
            <p className="text-sm text-muted-foreground max-w-xs">
              Add your first delivery address to get started. It will automatically become your default.
            </p>
            <button
              type="button"
              onClick={openAdd}
              className="flex items-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground shadow hover:opacity-90 transition"
            >
              <Plus size={15} /> Add your first address
            </button>
          </div>
        )}

        {/* Address list */}
        {!loading && !loadError && addresses.length > 0 && (
          <div className="flex flex-col gap-4">
            {addresses.map((addr) => (
              <div
                key={addr.addressId}
                id={`address-card-${addr.addressId}`}
                className={`rounded-2xl border bg-card shadow-sm p-5 transition ${
                  addr.isDefault ? 'border-primary/40 ring-1 ring-primary/20' : 'border-border'
                }`}
              >
                <div className="flex items-start justify-between gap-4">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1">
                      <span className="font-semibold text-foreground truncate">{addr.recipientName}</span>
                      {addr.isDefault && (
                        <span className="inline-flex items-center gap-1 rounded-full bg-primary/10 text-primary px-2 py-0.5 text-xs font-semibold">
                          <Star size={10} fill="currentColor" /> Default
                        </span>
                      )}
                    </div>
                    <p className="text-sm text-muted-foreground">{addr.phone}</p>
                    <p className="text-sm text-foreground mt-1">
                      {addr.addressLine1}
                      {addr.addressLine2 && `, ${addr.addressLine2}`}
                    </p>
                    <p className="text-sm text-foreground">
                      {addr.city}, {addr.district} — {addr.postalCode}
                    </p>
                  </div>

                  {/* Actions */}
                  <div className="flex items-center gap-2 shrink-0">
                    {!addr.isDefault && (
                      <button
                        id={`set-default-btn-${addr.addressId}`}
                        type="button"
                        onClick={() => handleSetDefault(addr)}
                        title="Set as default"
                        className="rounded-lg border border-border px-2.5 py-1.5 text-xs font-medium text-foreground hover:bg-muted transition flex items-center gap-1"
                      >
                        <Star size={12} /> Set default
                      </button>
                    )}
                    <button
                      id={`edit-address-btn-${addr.addressId}`}
                      type="button"
                      onClick={() => openEdit(addr)}
                      title="Edit address"
                      className="rounded-lg border border-border p-1.5 text-muted-foreground hover:text-foreground hover:bg-muted transition"
                    >
                      <Edit3 size={14} />
                    </button>
                    <button
                      id={`delete-address-btn-${addr.addressId}`}
                      type="button"
                      onClick={() => { setDeleteTarget(addr); setDeleteError(null); }}
                      title="Delete address"
                      className="rounded-lg border border-destructive/30 p-1.5 text-destructive hover:bg-destructive/10 transition"
                    >
                      <Trash2 size={14} />
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </main>

      {/* ===== Add / Edit Form Modal ===== */}
      {formOpen && (
        <div
          className="fixed inset-0 z-40 flex items-center justify-center bg-black/50 px-4"
          aria-modal="true"
          role="dialog"
          aria-label={formMode === 'add' ? 'Add address' : 'Edit address'}
        >
          <div className="w-full max-w-lg rounded-2xl border border-border bg-card shadow-xl overflow-auto max-h-[90vh]">
            <div className="flex items-center justify-between px-6 py-4 border-b border-border">
              <h2 className="font-semibold text-foreground">
                {formMode === 'add' ? 'Add New Address' : 'Edit Address'}
              </h2>
              <button type="button" onClick={closeForm} className="text-muted-foreground hover:text-foreground transition">
                <X size={18} />
              </button>
            </div>

            <form id="address-form" onSubmit={handleSubmit(onSubmit)} noValidate className="px-6 py-6 flex flex-col gap-4">
              {formError && (
                <div role="alert" className="rounded-lg border border-destructive/40 bg-destructive/10 px-4 py-3 text-sm text-destructive">
                  {formError}
                </div>
              )}

              <div className="grid grid-cols-2 gap-4">
                <FormField label="Recipient Name" id="addr-recipient" error={errors.recipientName?.message} className="col-span-2">
                  <input id="addr-recipient" type="text" autoComplete="name" {...register('recipientName')}
                    className={inputCls(!!errors.recipientName)} />
                </FormField>

                <FormField label="Phone (E.164)" id="addr-phone" error={errors.phone?.message} className="col-span-2">
                  <input id="addr-phone" type="tel" placeholder="+911234567890" autoComplete="tel" {...register('phone')}
                    className={inputCls(!!errors.phone)} />
                </FormField>

                <FormField label="Address Line 1" id="addr-line1" error={errors.addressLine1?.message} className="col-span-2">
                  <input id="addr-line1" type="text" autoComplete="address-line1" {...register('addressLine1')}
                    className={inputCls(!!errors.addressLine1)} />
                </FormField>

                <FormField label="Address Line 2 (optional)" id="addr-line2" className="col-span-2">
                  <input id="addr-line2" type="text" autoComplete="address-line2" {...register('addressLine2')}
                    className={inputCls(false)} />
                </FormField>

                <FormField label="City" id="addr-city" error={errors.city?.message}>
                  <input id="addr-city" type="text" autoComplete="address-level2" {...register('city')}
                    className={inputCls(!!errors.city)} />
                </FormField>

                <FormField label="District" id="addr-district" error={errors.district?.message}>
                  <input id="addr-district" type="text" {...register('district')}
                    className={inputCls(!!errors.district)} />
                </FormField>

                <FormField label="Postal Code" id="addr-postal" error={errors.postalCode?.message}>
                  <input id="addr-postal" type="text" autoComplete="postal-code" {...register('postalCode')}
                    className={inputCls(!!errors.postalCode)} />
                </FormField>

                <div className="flex items-center gap-2.5 col-span-2 pt-1">
                  <input
                    id="addr-is-default"
                    type="checkbox"
                    {...register('isDefault')}
                    className="h-4 w-4 accent-primary cursor-pointer"
                  />
                  <label htmlFor="addr-is-default" className="text-sm text-foreground cursor-pointer select-none">
                    Set as default address
                  </label>
                </div>
              </div>

              <div className="flex items-center gap-3 pt-2">
                <button
                  id="address-form-submit"
                  type="submit"
                  disabled={formSubmitting}
                  className="flex items-center gap-2 rounded-lg bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground shadow hover:opacity-90 disabled:opacity-60 disabled:cursor-not-allowed transition"
                >
                  {formSubmitting
                    ? <><Loader2 size={15} className="animate-spin" /> Saving…</>
                    : formMode === 'add' ? <><Plus size={15} /> Add Address</> : <><Edit3 size={15} /> Save Changes</>
                  }
                </button>
                <button type="button" onClick={closeForm}
                  className="rounded-lg border border-border px-4 py-2.5 text-sm font-medium text-foreground hover:bg-muted transition">
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ===== Delete Confirmation Modal ===== */}
      {deleteTarget && (
        <div
          className="fixed inset-0 z-40 flex items-center justify-center bg-black/50 px-4"
          aria-modal="true"
          role="dialog"
          aria-label="Confirm delete"
        >
          <div className="w-full max-w-md rounded-2xl border border-border bg-card shadow-xl p-6">
            <div className="flex items-center gap-3 mb-4">
              <div className="flex h-10 w-10 items-center justify-center rounded-full bg-destructive/10 text-destructive">
                <AlertTriangle size={20} />
              </div>
              <h2 className="font-semibold text-foreground">Delete Address?</h2>
            </div>
            <p className="text-sm text-muted-foreground mb-4">
              Are you sure you want to delete the address for <strong>{deleteTarget.recipientName}</strong>?
              {deleteTarget.isDefault && ' A new default will be chosen automatically if other addresses exist.'}
              {' '}This action cannot be undone.
            </p>
            {deleteError && (
              <div role="alert" className="mb-4 rounded-lg border border-destructive/40 bg-destructive/10 px-4 py-3 text-sm text-destructive">
                {deleteError}
              </div>
            )}
            <div className="flex items-center gap-3">
              <button
                id="confirm-delete-btn"
                type="button"
                disabled={deleting}
                onClick={confirmDelete}
                className="flex items-center gap-2 rounded-lg bg-destructive px-4 py-2.5 text-sm font-semibold text-white shadow hover:opacity-90 disabled:opacity-60 disabled:cursor-not-allowed transition"
              >
                {deleting ? <><Loader2 size={15} className="animate-spin" /> Deleting…</> : <><Trash2 size={15} /> Delete</>}
              </button>
              <button
                type="button"
                onClick={() => { setDeleteTarget(null); setDeleteError(null); }}
                className="rounded-lg border border-border px-4 py-2.5 text-sm font-medium text-foreground hover:bg-muted transition"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

// -------------------------------------------------------------------------
// Helpers
// -------------------------------------------------------------------------

const inputCls = (invalid: boolean) =>
  `w-full rounded-lg border ${invalid ? 'border-destructive' : 'border-input'} bg-background px-3.5 py-2.5 text-sm text-foreground placeholder:text-muted-foreground outline-none transition focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2`;

function FormField({
  label, id, error, children, className,
}: {
  label: string; id: string; error?: string; children: React.ReactNode; className?: string;
}) {
  return (
    <div className={`flex flex-col gap-1.5 ${className ?? ''}`}>
      <label htmlFor={id} className="text-sm font-medium text-foreground">{label}</label>
      {children}
      {error && <p className="text-xs text-destructive" role="alert">{error}</p>}
    </div>
  );
}
