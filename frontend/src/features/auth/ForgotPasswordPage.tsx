import { useState } from 'react';
import { Link } from 'react-router-dom';
import { z } from 'zod/v4';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Mail, Loader2, CheckCircle2, ArrowLeft } from 'lucide-react';
import { forgotPassword } from '../../api/authApi';
import type { ApiError } from '../../api/authApi';

const schema = z.object({
  email: z
    .string()
    .min(1, 'Email is required')
    .email('Enter a valid email address'),
});

type FormValues = z.infer<typeof schema>;

export default function ForgotPasswordPage() {
  const [submitted, setSubmitted] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: '' },
  });

  async function onSubmit(values: FormValues) {
    setServerError(null);
    try {
      await forgotPassword(values.email);
      setSubmitted(true);
    } catch (err) {
      const apiError = err as ApiError;
      if (apiError?.status && apiError.status >= 500) {
        setServerError('Something went wrong. Please try again later.');
      } else {
        setSubmitted(true);
      }
    }
  }

  return (
    <div>
      <div className="mb-6 text-center">
        <h2 className="text-xl font-bold tracking-tight text-foreground">
          Forgot your password?
        </h2>
        <p className="text-xs text-muted-foreground mt-1">
          {submitted
            ? 'Check your email for a reset link.'
            : "Enter your email and we'll send you a reset link."}
        </p>
      </div>

      {submitted ? (
        <div id="forgot-password-success" className="flex flex-col items-center gap-4 text-center py-2">
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-green-500/10 text-green-600">
            <CheckCircle2 className="h-10 w-10" />
          </div>
          <p className="text-xs text-foreground leading-relaxed">
            If an account with that email exists, a password reset link has been sent.
            The link will expire in 1 hour.
          </p>
          <p className="text-[11px] text-muted-foreground">
            Didn't get the email? Check your spam folder, or{' '}
            <button
              id="forgot-password-retry"
              type="button"
              onClick={() => setSubmitted(false)}
              className="font-medium text-primary underline-offset-4 hover:underline"
            >
              try again
            </button>
            .
          </p>
        </div>
      ) : (
        <form
          id="forgot-password-form"
          onSubmit={handleSubmit(onSubmit)}
          noValidate
          className="flex flex-col gap-4"
        >
          {serverError && (
            <div
              id="forgot-password-error"
              role="alert"
              className="rounded-lg border border-destructive/40 bg-destructive/10 px-4 py-3 text-xs text-destructive"
            >
              {serverError}
            </div>
          )}

          <div className="flex flex-col gap-1.5">
            <label htmlFor="forgot-email" className="text-xs font-medium text-foreground">
              Email address
            </label>
            <input
              id="forgot-email"
              type="email"
              autoComplete="email"
              placeholder="you@example.com"
              {...register('email')}
              aria-invalid={!!errors.email}
              className="rounded-lg border border-input bg-background px-3.5 py-2 text-xs text-foreground placeholder:text-muted-foreground outline-none ring-offset-background transition focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 aria-invalid:border-destructive aria-invalid:ring-destructive/40"
            />
            {errors.email && (
              <p id="forgot-email-error" className="text-[11px] text-destructive" role="alert">
                {errors.email.message}
              </p>
            )}
          </div>

          <button
            id="forgot-password-submit"
            type="submit"
            disabled={isSubmitting}
            className="flex items-center justify-center gap-2 rounded-lg bg-primary px-4 py-2.5 text-xs font-semibold text-primary-foreground transition hover:opacity-90 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
          >
            {isSubmitting ? (
              <>
                <Loader2 size={14} className="animate-spin" />
                Sending…
              </>
            ) : (
              <>
                <Mail size={14} />
                Send reset link
              </>
            )}
          </button>
        </form>
      )}

      <p className="mt-6 text-center text-xs text-muted-foreground">
        <Link
          to="/login"
          id="forgot-back-to-login"
          className="inline-flex items-center gap-1 font-medium text-foreground underline-offset-4 hover:underline"
        >
          <ArrowLeft size={12} />
          Back to sign in
        </Link>
      </p>
    </div>
  );
}
