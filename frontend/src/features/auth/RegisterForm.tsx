import { z } from 'zod/v4';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { Eye, EyeOff, Loader2, UserPlus } from 'lucide-react';
import type { ApiError, RegisterPayload, UserResponse } from '../../api/authApi';

const registerSchema = z.object({
  fullName: z.string().min(1, 'Full name is required').max(150, 'Max 150 characters'),
  email: z.string().min(1, 'Email is required').email('Enter a valid email address').max(255),
  password: z
    .string()
    .min(8, 'Password must be at least 8 characters')
    .regex(
      /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^a-zA-Z\d]).{8,}$/,
      'Password must contain an uppercase letter, lowercase letter, number, and special character'
    ),
  phone: z
    .string()
    .regex(/^(\+[1-9]\d{6,14})?$/, 'Phone must be in E.164 format (e.g. +1234567890)')
    .optional()
    .or(z.literal('')),
});

export type RegisterFormValues = z.infer<typeof registerSchema>;

interface RegisterFormProps {
  onSubmit: (values: RegisterPayload) => Promise<UserResponse>;
  onSuccess: (user: UserResponse) => void;
}

export function RegisterForm({ onSubmit, onSuccess }: RegisterFormProps) {
  const [showPassword, setShowPassword] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      fullName: '',
      email: '',
      password: '',
      phone: '',
    },
  });

  async function handleFormSubmit(values: RegisterFormValues) {
    setServerError(null);
    try {
      const payload: RegisterPayload = {
        fullName: values.fullName,
        email: values.email,
        password: values.password,
        phone: values.phone?.trim() ? values.phone.trim() : undefined,
      };
      const response = await onSubmit(payload);
      onSuccess(response);
    } catch (err) {
      const apiError = err as ApiError;
      setServerError(
        apiError?.message ?? 'An unexpected error occurred during registration. Please try again.'
      );
    }
  }

  return (
    <form
      id="register-form"
      onSubmit={handleSubmit(handleFormSubmit)}
      noValidate
      className="flex flex-col gap-4"
    >
      {serverError && (
        <div
          id="register-server-error"
          role="alert"
          className="rounded-lg border border-destructive/40 bg-destructive/10 px-4 py-3 text-sm text-destructive"
        >
          {serverError}
        </div>
      )}

      {/* Full Name */}
      <div className="flex flex-col gap-1.5">
        <label htmlFor="register-fullName" className="text-sm font-medium text-foreground">
          Full Name
        </label>
        <input
          id="register-fullName"
          type="text"
          placeholder="Jane Doe"
          {...register('fullName')}
          aria-invalid={!!errors.fullName}
          className="rounded-lg border border-input bg-background px-3.5 py-2.5 text-sm text-foreground placeholder:text-muted-foreground outline-none ring-offset-background transition focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 aria-invalid:border-destructive aria-invalid:ring-destructive/40"
        />
        {errors.fullName && (
          <p id="register-fullName-error" className="text-xs text-destructive" role="alert">
            {errors.fullName.message}
          </p>
        )}
      </div>

      {/* Email */}
      <div className="flex flex-col gap-1.5">
        <label htmlFor="register-email" className="text-sm font-medium text-foreground">
          Email
        </label>
        <input
          id="register-email"
          type="email"
          autoComplete="email"
          placeholder="you@example.com"
          {...register('email')}
          aria-invalid={!!errors.email}
          className="rounded-lg border border-input bg-background px-3.5 py-2.5 text-sm text-foreground placeholder:text-muted-foreground outline-none ring-offset-background transition focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 aria-invalid:border-destructive aria-invalid:ring-destructive/40"
        />
        {errors.email && (
          <p id="register-email-error" className="text-xs text-destructive" role="alert">
            {errors.email.message}
          </p>
        )}
      </div>

      {/* Password */}
      <div className="flex flex-col gap-1.5">
        <label htmlFor="register-password" className="text-sm font-medium text-foreground">
          Password
        </label>
        <div className="relative">
          <input
            id="register-password"
            type={showPassword ? 'text' : 'password'}
            autoComplete="new-password"
            placeholder="••••••••"
            {...register('password')}
            aria-invalid={!!errors.password}
            className="w-full rounded-lg border border-input bg-background px-3.5 py-2.5 pr-11 text-sm text-foreground placeholder:text-muted-foreground outline-none ring-offset-background transition focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 aria-invalid:border-destructive aria-invalid:ring-destructive/40"
          />
          <button
            type="button"
            id="register-toggle-password"
            aria-label={showPassword ? 'Hide password' : 'Show password'}
            onClick={() => setShowPassword(v => !v)}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition"
          >
            {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
          </button>
        </div>
        {errors.password && (
          <p id="register-password-error" className="text-xs text-destructive" role="alert">
            {errors.password.message}
          </p>
        )}
      </div>

      {/* Phone (Optional) */}
      <div className="flex flex-col gap-1.5">
        <label htmlFor="register-phone" className="text-sm font-medium text-foreground">
          Phone <span className="text-xs text-muted-foreground font-normal">(Optional, E.164)</span>
        </label>
        <input
          id="register-phone"
          type="tel"
          placeholder="+1234567890"
          {...register('phone')}
          aria-invalid={!!errors.phone}
          className="rounded-lg border border-input bg-background px-3.5 py-2.5 text-sm text-foreground placeholder:text-muted-foreground outline-none ring-offset-background transition focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 aria-invalid:border-destructive aria-invalid:ring-destructive/40"
        />
        {errors.phone && (
          <p id="register-phone-error" className="text-xs text-destructive" role="alert">
            {errors.phone.message}
          </p>
        )}
      </div>

      {/* Submit button */}
      <button
        id="register-submit"
        type="submit"
        disabled={isSubmitting}
        className="mt-2 flex items-center justify-center gap-2 rounded-lg bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground transition hover:opacity-90 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
      >
        {isSubmitting ? (
          <>
            <Loader2 size={16} className="animate-spin" />
            Creating account…
          </>
        ) : (
          <>
            <UserPlus size={16} />
            Create Account
          </>
        )}
      </button>
    </form>
  );
}
