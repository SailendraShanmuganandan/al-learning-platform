import { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { z } from 'zod/v4';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Eye, EyeOff, Loader2, ShieldCheck, CheckCircle2, AlertTriangle } from 'lucide-react';
import { resetPassword } from '../../api/authApi';
import type { ApiError } from '../../api/authApi';

// ---------------------------------------------------------------------------
// Schema — same password-strength rules as registration
// ---------------------------------------------------------------------------

const schema = z
  .object({
    newPassword: z
      .string()
      .min(8, 'Password must be at least 8 characters')
      .regex(
        /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^a-zA-Z\d]).{8,}$/,
        'Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character',
      ),
    confirmPassword: z.string().min(1, 'Please confirm your new password'),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: "Passwords don't match",
    path: ['confirmPassword'],
  });

type FormValues = z.infer<typeof schema>;

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

type PageState = 'form' | 'success' | 'invalid';

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') ?? '';

  const [pageState, setPageState] = useState<PageState>('form');
  const [serverError, setServerError] = useState<string | null>(null);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);

  // If no token in URL, go straight to invalid state
  useEffect(() => {
    if (!token) {
      setPageState('invalid');
    }
  }, [token]);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { newPassword: '', confirmPassword: '' },
  });

  async function onSubmit(values: FormValues) {
    setServerError(null);
    try {
      await resetPassword(token, values.newPassword);
      setPageState('success');
    } catch (err) {
      const apiError = err as ApiError;
      if (apiError?.status === 400) {
        // Invalid / expired / already-used token
        setPageState('invalid');
      } else {
        setServerError(apiError?.message ?? 'An unexpected error occurred. Please try again.');
      }
    }
  }

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-background px-4">
      {/* Background decorative gradient */}
      <div aria-hidden className="pointer-events-none fixed inset-0 -z-10 overflow-hidden">
        <div className="absolute -top-40 -right-40 h-[600px] w-[600px] rounded-full bg-primary/5 blur-3xl" />
        <div className="absolute -bottom-40 -left-40 h-[500px] w-[500px] rounded-full bg-primary/5 blur-3xl" />
      </div>

      <div className="w-full max-w-md">
        {/* Wordmark */}
        <div className="mb-8 flex flex-col items-center gap-2 text-center">
          <div
            className="flex h-12 w-12 items-center justify-center rounded-2xl bg-primary text-primary-foreground text-xl font-bold shadow-lg"
            aria-hidden
          >
            AL
          </div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">
            {pageState === 'success'
              ? 'Password reset!'
              : pageState === 'invalid'
                ? 'Link expired or invalid'
                : 'Set a new password'}
          </h1>
          <p className="text-sm text-muted-foreground">
            {pageState === 'success'
              ? 'Your password has been updated.'
              : pageState === 'invalid'
                ? 'This reset link is invalid, expired, or has already been used.'
                : 'Enter your new password below.'}
          </p>
        </div>

        {/* Card */}
        <div className="rounded-2xl border border-border bg-card p-8 shadow-sm">
          {pageState === 'success' && (
            <div id="reset-password-success" className="flex flex-col items-center gap-4 text-center">
              <CheckCircle2 className="h-12 w-12 text-green-500" />
              <p className="text-sm text-foreground">
                Your password has been reset successfully. You can now sign in with your new password.
              </p>
              <Link
                to="/login"
                id="reset-password-go-login"
                className="flex items-center justify-center gap-2 w-full rounded-lg bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground transition hover:opacity-90 active:scale-[0.98] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
              >
                Go to sign in
              </Link>
            </div>
          )}

          {pageState === 'invalid' && (
            <div id="reset-password-invalid" className="flex flex-col items-center gap-4 text-center">
              <AlertTriangle className="h-12 w-12 text-amber-500" />
              <p className="text-sm text-foreground">
                This password reset link is invalid, expired, or has already been used.
                Reset links are valid for 1 hour.
              </p>
              <Link
                to="/forgot-password"
                id="reset-password-request-new"
                className="flex items-center justify-center gap-2 w-full rounded-lg bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground transition hover:opacity-90 active:scale-[0.98] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
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
              className="flex flex-col gap-5"
            >
              {serverError && (
                <div
                  id="reset-password-error"
                  role="alert"
                  className="rounded-lg border border-destructive/40 bg-destructive/10 px-4 py-3 text-sm text-destructive"
                >
                  {serverError}
                </div>
              )}

              {/* New password */}
              <div className="flex flex-col gap-1.5">
                <label htmlFor="reset-new-password" className="text-sm font-medium text-foreground">
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
                    className="w-full rounded-lg border border-input bg-background px-3.5 py-2.5 pr-11 text-sm text-foreground placeholder:text-muted-foreground outline-none ring-offset-background transition focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 aria-invalid:border-destructive aria-invalid:ring-destructive/40"
                  />
                  <button
                    type="button"
                    id="reset-toggle-password"
                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                    onClick={() => setShowPassword((v) => !v)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition"
                  >
                    {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                  </button>
                </div>
                {errors.newPassword && (
                  <p id="reset-new-password-error" className="text-xs text-destructive" role="alert">
                    {errors.newPassword.message}
                  </p>
                )}
              </div>

              {/* Confirm password */}
              <div className="flex flex-col gap-1.5">
                <label htmlFor="reset-confirm-password" className="text-sm font-medium text-foreground">
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
                    className="w-full rounded-lg border border-input bg-background px-3.5 py-2.5 pr-11 text-sm text-foreground placeholder:text-muted-foreground outline-none ring-offset-background transition focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 aria-invalid:border-destructive aria-invalid:ring-destructive/40"
                  />
                  <button
                    type="button"
                    id="reset-toggle-confirm"
                    aria-label={showConfirm ? 'Hide password' : 'Show password'}
                    onClick={() => setShowConfirm((v) => !v)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition"
                  >
                    {showConfirm ? <EyeOff size={16} /> : <Eye size={16} />}
                  </button>
                </div>
                {errors.confirmPassword && (
                  <p id="reset-confirm-password-error" className="text-xs text-destructive" role="alert">
                    {errors.confirmPassword.message}
                  </p>
                )}
              </div>

              <button
                id="reset-password-submit"
                type="submit"
                disabled={isSubmitting}
                className="flex items-center justify-center gap-2 rounded-lg bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground transition hover:opacity-90 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
              >
                {isSubmitting ? (
                  <>
                    <Loader2 size={16} className="animate-spin" />
                    Resetting…
                  </>
                ) : (
                  <>
                    <ShieldCheck size={16} />
                    Reset password
                  </>
                )}
              </button>
            </form>
          )}
        </div>

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
    </div>
  );
}
