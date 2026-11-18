import { useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { z } from 'zod/v4';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  Eye,
  EyeOff,
  Loader2,
  CheckCircle2,
  AlertTriangle,
  ShieldCheck,
} from 'lucide-react';
import { resetPassword } from '../../api/authApi';
import type { ApiError } from '../../api/authApi';

const schema = z
  .object({
    newPassword: z
      .string()
      .min(8, 'Password must be at least 8 characters')
      .regex(/[A-Z]/, 'Must contain at least one uppercase letter')
      .regex(/[a-z]/, 'Must contain at least one lowercase letter')
      .regex(/[0-9]/, 'Must contain at least one number'),
    confirmPassword: z.string().min(1, 'Confirm your password'),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  });

type FormValues = z.infer<typeof schema>;
type PageState = 'form' | 'success' | 'invalid';

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');

  const [pageState, setPageState] = useState<PageState>(token ? 'form' : 'invalid');
  const [serverError, setServerError] = useState<string | null>(null);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { newPassword: '', confirmPassword: '' },
  });

  async function onSubmit(values: FormValues) {
    if (!token) {
      setPageState('invalid');
      return;
    }
    setServerError(null);
    try {
      await resetPassword(token, values.newPassword);
      setPageState('success');
    } catch (err) {
      const apiError = err as ApiError;
      if (apiError?.status === 400 || apiError?.status === 404) {
        setPageState('invalid');
      } else {
        setServerError(
          apiError?.message || 'Failed to reset password. Please try again later.'
        );
      }
    }
  }

  return (
    <div>
      <div className="mb-6 text-center">
        <h2 className="text-xl font-bold tracking-tight text-foreground">
          {pageState === 'success'
            ? 'Password reset!'
            : pageState === 'invalid'
              ? 'Link expired or invalid'
              : 'Set a new password'}
        </h2>
        <p className="text-xs text-muted-foreground mt-1">
          {pageState === 'success'
            ? 'Your password has been updated.'
            : pageState === 'invalid'
              ? 'This reset link is invalid, expired, or has already been used.'
              : 'Enter your new password below.'}
        </p>
      </div>

      {pageState === 'success' && (
        <div id="reset-password-success" className="flex flex-col items-center gap-4 text-center py-2">
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-green-500/10 text-green-600">
            <CheckCircle2 className="h-10 w-10" />
          </div>
          <p className="text-xs text-foreground">
            Your password has been reset successfully. You can now sign in with your new password.
          </p>
          <Link
            to="/login"
            id="reset-password-go-login"
            className="flex items-center justify-center gap-2 w-full rounded-lg bg-primary px-4 py-2.5 text-xs font-semibold text-primary-foreground transition hover:opacity-90 active:scale-[0.98] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
          >
            Go to sign in
          </Link>
        </div>
      )}

      {pageState === 'invalid' && (
        <div id="reset-password-invalid" className="flex flex-col items-center gap-4 text-center py-2">
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-amber-500/10 text-amber-600">
            <AlertTriangle className="h-10 w-10" />
          </div>
          <p className="text-xs text-foreground">
            This password reset link is invalid, expired, or has already been used.
            Reset links are valid for 1 hour.
          </p>
          <Link
            to="/forgot-password"
            id="reset-password-request-new"
            className="flex items-center justify-center gap-2 w-full rounded-lg bg-primary px-4 py-2.5 text-xs font-semibold text-primary-foreground transition hover:opacity-90 active:scale-[0.98] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
          >
            Request a new reset link
          </Link>
        </div>
      )}

      {pageState === 'form' && (
        <form
          id="reset-password-form"
          onSubmit={handleSubmit(onSubmit)}
          noValidate
          className="flex flex-col gap-4"
        >
          {serverError && (
            <div
              id="reset-password-error"
              role="alert"
              className="rounded-lg border border-destructive/40 bg-destructive/10 px-4 py-3 text-xs text-destructive"
            >
              {serverError}
            </div>
          )}

          {/* New password */}
          <div className="flex flex-col gap-1.5">
            <label htmlFor="reset-new-password" className="text-xs font-medium text-foreground">
              New password
            </label>
            <div className="relative">
              <input
                id="reset-new-password"
                type={showPassword ? 'text' : 'password'}
                autoComplete="new-password"
                placeholder="••••••••"
                {...register('newPassword')}
                aria-invalid={!!errors.newPassword}
                className="w-full rounded-lg border border-input bg-background px-3.5 py-2 pr-10 text-xs text-foreground placeholder:text-muted-foreground outline-none ring-offset-background transition focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 aria-invalid:border-destructive aria-invalid:ring-destructive/40"
              />
              <button
                type="button"
                id="reset-toggle-password"
                aria-label={showPassword ? 'Hide password' : 'Show password'}
                onClick={() => setShowPassword((v) => !v)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition"
              >
                {showPassword ? <EyeOff size={14} /> : <Eye size={14} />}
              </button>
            </div>
            {errors.newPassword && (
              <p id="reset-new-password-error" className="text-[11px] text-destructive" role="alert">
                {errors.newPassword.message}
              </p>
            )}
          </div>

          {/* Confirm password */}
          <div className="flex flex-col gap-1.5">
            <label htmlFor="reset-confirm-password" className="text-xs font-medium text-foreground">
              Confirm new password
            </label>
            <div className="relative">
              <input
                id="reset-confirm-password"
                type={showConfirm ? 'text' : 'password'}
                autoComplete="new-password"
                placeholder="••••••••"
                {...register('confirmPassword')}
                aria-invalid={!!errors.confirmPassword}
                className="w-full rounded-lg border border-input bg-background px-3.5 py-2 pr-10 text-xs text-foreground placeholder:text-muted-foreground outline-none ring-offset-background transition focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 aria-invalid:border-destructive aria-invalid:ring-destructive/40"
              />
              <button
                type="button"
                id="reset-toggle-confirm"
                aria-label={showConfirm ? 'Hide password' : 'Show password'}
                onClick={() => setShowConfirm((v) => !v)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition"
              >
                {showConfirm ? <EyeOff size={14} /> : <Eye size={14} />}
              </button>
            </div>
            {errors.confirmPassword && (
              <p id="reset-confirm-password-error" className="text-[11px] text-destructive" role="alert">
                {errors.confirmPassword.message}
              </p>
            )}
          </div>

          <button
            id="reset-password-submit"
            type="submit"
            disabled={isSubmitting}
            className="flex items-center justify-center gap-2 rounded-lg bg-primary px-4 py-2.5 text-xs font-semibold text-primary-foreground transition hover:opacity-90 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
          >
            {isSubmitting ? (
              <>
                <Loader2 size={14} className="animate-spin" />
                Resetting…
              </>
            ) : (
              <>
                <ShieldCheck size={14} />
                Reset password
              </>
            )}
          </button>
        </form>
      )}

      <p className="mt-6 text-center text-xs text-muted-foreground">
        <Link
          to="/login"
          id="reset-back-to-login"
          className="font-medium text-foreground underline-offset-4 hover:underline"
        >
          Back to sign in
        </Link>
      </p>
    </div>
  );
}
