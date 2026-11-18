import { useState } from 'react';
import { z } from 'zod/v4';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  User, Mail, Phone, Shield, CheckCircle2, AlertCircle,
  Edit3, Save, X, Loader2,
} from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';
import { updateMyProfile, type UpdateProfilePayload } from '../../api/userApi';
import type { ApiError } from '../../api/authApi';
import { PageContainer } from '../../components/layout/PageContainer';
import { PageHeader } from '../../components/layout/PageHeader';

const schema = z.object({
  fullName: z.string().min(1, 'Full name is required').max(150, 'Max 150 characters'),
  phone: z
    .string()
    .regex(/^\+[1-9]\d{6,14}$|^$/, 'Phone must be in E.164 format (e.g. +911234567890) or left blank')
    .optional()
    .or(z.literal('')),
});

type FormValues = z.infer<typeof schema>;

export default function ProfilePage() {
  const auth = useAuth();

  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [saveSuccess, setSaveSuccess] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { fullName: auth.user?.fullName ?? '', phone: auth.user?.phone ?? '' },
  });

  const isVerified = auth.user?.isEmailVerified ?? auth.user?.is_email_verified ?? false;

  async function onSubmit(values: FormValues) {
    setSaving(true);
    setSaveError(null);
    setSaveSuccess(false);
    try {
      const payload: UpdateProfilePayload = {
        fullName: values.fullName,
        phone: values.phone ?? '',
      };
      const updated = await updateMyProfile(payload);
      auth.updateUser(updated);
      setSaveSuccess(true);
      setEditing(false);
    } catch (err) {
      const apiErr = err as ApiError;
      setSaveError(apiErr?.message ?? 'Failed to save changes. Please try again.');
    } finally {
      setSaving(false);
    }
  }

  function cancelEdit() {
    reset({ fullName: auth.user?.fullName ?? '', phone: auth.user?.phone ?? '' });
    setSaveError(null);
    setEditing(false);
  }

  return (
    <PageContainer>
      <div className="space-y-6">
        <PageHeader
          title="My Profile"
          description="View and update your account information."
        />

        {/* Email verification banner */}
        {!isVerified && (
          <div className="flex items-center gap-3 rounded-xl border border-amber-500/30 bg-amber-500/10 px-4 py-3 text-xs text-amber-800">
            <AlertCircle size={16} className="shrink-0 text-amber-600" />
            <span>
              <strong>Email not verified.</strong> Please verify your email to unlock purchases and registrations.
            </span>
          </div>
        )}

        {saveSuccess && (
          <div
            role="alert"
            className="flex items-center gap-3 rounded-xl border border-green-500/30 bg-green-500/10 px-4 py-3 text-xs text-green-800"
          >
            <CheckCircle2 size={16} className="shrink-0 text-green-600" />
            Profile updated successfully!
          </div>
        )}

        {/* Profile card */}
        <div className="rounded-2xl border border-border bg-card shadow-sm overflow-hidden">
          {/* Card header */}
          <div className="flex items-center justify-between px-6 py-4 border-b border-border">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-full bg-primary/10 text-primary">
                <User size={20} />
              </div>
              <span className="font-semibold text-foreground">Account Details</span>
            </div>
            {!editing && (
              <button
                id="profile-edit-btn"
                type="button"
                onClick={() => setEditing(true)}
                className="flex items-center gap-1.5 rounded-lg bg-primary/10 hover:bg-primary/20 text-primary px-3 py-1.5 text-xs font-semibold transition"
              >
                <Edit3 size={13} /> Edit
              </button>
            )}
          </div>

          {/* Read-only view */}
          {!editing && (
            <dl className="divide-y divide-border">
              <ProfileRow icon={<User size={15} />} label="Full Name" value={auth.user?.fullName ?? '—'} id="profile-full-name" />
              <ProfileRow
                icon={<Mail size={15} />}
                label="Email"
                value={
                  <span className="flex items-center gap-2">
                    {auth.user?.email}
                    {isVerified ? (
                      <span className="inline-flex items-center gap-1 text-xs font-semibold text-green-600">
                        <CheckCircle2 size={12} /> Verified
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1 text-xs font-semibold text-amber-600">
                        <AlertCircle size={12} /> Not verified
                      </span>
                    )}
                  </span>
                }
                id="profile-email"
              />
              <ProfileRow icon={<Phone size={15} />} label="Phone" value={auth.user?.phone ?? '—'} id="profile-phone" />
              <ProfileRow icon={<Shield size={15} />} label="Role" value={auth.user?.role ?? '—'} id="profile-role" />
            </dl>
          )}

          {/* Edit form */}
          {editing && (
            <form id="profile-edit-form" onSubmit={handleSubmit(onSubmit)} className="px-6 py-6 flex flex-col gap-5">
              {saveError && (
                <div role="alert" className="rounded-lg border border-destructive/40 bg-destructive/10 px-4 py-3 text-sm text-destructive">
                  {saveError}
                </div>
              )}

              {/* Full Name */}
              <div className="flex flex-col gap-1.5">
                <label htmlFor="profile-input-fullname" className="text-sm font-medium text-foreground">
                  Full Name
                </label>
                <input
                  id="profile-input-fullname"
                  type="text"
                  autoComplete="name"
                  {...register('fullName')}
                  aria-invalid={!!errors.fullName}
                  className="w-full rounded-lg border border-input bg-background px-3.5 py-2.5 text-sm text-foreground placeholder:text-muted-foreground outline-none transition focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 aria-invalid:border-destructive"
                />
                {errors.fullName && (
                  <p id="profile-fullname-error" className="text-xs text-destructive" role="alert">
                    {errors.fullName.message}
                  </p>
                )}
              </div>

              {/* Email — read-only */}
              <div className="flex flex-col gap-1.5">
                <label className="text-sm font-medium text-foreground">
                  Email <span className="text-xs font-normal text-muted-foreground">(cannot be changed)</span>
                </label>
                <input
                  type="email"
                  value={auth.user?.email ?? ''}
                  disabled
                  className="w-full rounded-lg border border-border bg-muted px-3.5 py-2.5 text-sm text-muted-foreground cursor-not-allowed"
                />
              </div>

              {/* Phone */}
              <div className="flex flex-col gap-1.5">
                <label htmlFor="profile-input-phone" className="text-sm font-medium text-foreground">
                  Phone <span className="text-xs font-normal text-muted-foreground">(optional, E.164 format)</span>
                </label>
                <input
                  id="profile-input-phone"
                  type="tel"
                  placeholder="+911234567890"
                  autoComplete="tel"
                  {...register('phone')}
                  aria-invalid={!!errors.phone}
                  className="w-full rounded-lg border border-input bg-background px-3.5 py-2.5 text-sm text-foreground placeholder:text-muted-foreground outline-none transition focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 aria-invalid:border-destructive"
                />
                {errors.phone && (
                  <p id="profile-phone-error" className="text-xs text-destructive" role="alert">
                    {errors.phone.message}
                  </p>
                )}
              </div>

              {/* Actions */}
              <div className="flex items-center gap-3 pt-1">
                <button
                  id="profile-save-btn"
                  type="submit"
                  disabled={saving}
                  className="flex items-center gap-2 rounded-lg bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground shadow transition hover:opacity-90 disabled:opacity-60 disabled:cursor-not-allowed"
                >
                  {saving ? <><Loader2 size={15} className="animate-spin" /> Saving…</> : <><Save size={15} /> Save changes</>}
                </button>
                <button
                  id="profile-cancel-btn"
                  type="button"
                  onClick={cancelEdit}
                  className="flex items-center gap-2 rounded-lg border border-border px-4 py-2.5 text-sm font-medium text-foreground hover:bg-muted transition"
                >
                  <X size={15} /> Cancel
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
    </PageContainer>
  );
}

// -------------------------------------------------------------------------
// Sub-component
// -------------------------------------------------------------------------

function ProfileRow({
  icon,
  label,
  value,
  id,
}: {
  icon: React.ReactNode;
  label: string;
  value: React.ReactNode;
  id: string;
}) {
  return (
    <div className="flex items-center justify-between px-6 py-4 gap-4">
      <dt className="flex items-center gap-2 text-sm text-muted-foreground min-w-[120px]">
        {icon}
        {label}
      </dt>
      <dd id={id} className="text-sm font-medium text-foreground text-right">
        {value}
      </dd>
    </div>
  );
}
