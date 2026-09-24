import { useState } from 'react';
import { Link } from 'react-router-dom';
import { registerUser, type UserResponse } from '../../api/authApi';
import { RegisterForm } from './RegisterForm';
import { Mail, ArrowRight } from 'lucide-react';

export default function RegisterPage() {
  const [registeredUser, setRegisteredUser] = useState<UserResponse | null>(null);

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-background px-4 py-8">
      {/* Background decorative gradient */}
      <div
        aria-hidden
        className="pointer-events-none fixed inset-0 -z-10 overflow-hidden"
      >
        <div className="absolute -top-40 -right-40 h-[600px] w-[600px] rounded-full bg-primary/5 blur-3xl" />
        <div className="absolute -bottom-40 -left-40 h-[500px] w-[500px] rounded-full bg-primary/5 blur-3xl" />
      </div>

      <div className="w-full max-w-md">
        {/* Wordmark */}
        <div className="mb-6 flex flex-col items-center gap-2 text-center">
          <div
            className="flex h-12 w-12 items-center justify-center rounded-2xl bg-primary text-primary-foreground text-xl font-bold shadow-lg"
            aria-hidden
          >
            AL
          </div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">
            AL Learning Platform
          </h1>
          <p className="text-sm text-muted-foreground">
            {registeredUser ? 'Account created' : 'Create your student account'}
          </p>
        </div>

        {/* Card */}
        <div className="rounded-2xl border border-border bg-card p-8 shadow-sm">
          {registeredUser ? (
            <div className="flex flex-col items-center text-center gap-4 py-2">
              <div className="flex h-16 w-16 items-center justify-center rounded-full bg-primary/10 text-primary">
                <Mail className="h-8 w-8" />
              </div>
              <h2 className="text-xl font-bold text-foreground">
                Check your email to verify your account
              </h2>
              <p className="text-sm text-muted-foreground">
                We&apos;ve sent a verification link to{' '}
                <strong className="text-foreground">{registeredUser.email}</strong>.
                Please check your inbox (and spam folder) and click the link to verify your account.
              </p>
              <div className="mt-4 w-full">
                <Link
                  to="/login"
                  id="post-register-login-btn"
                  className="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground shadow transition hover:opacity-90"
                >
                  Proceed to Login
                  <ArrowRight size={16} />
                </Link>
              </div>
            </div>
          ) : (
            <RegisterForm
              onSubmit={registerUser}
              onSuccess={(user) => setRegisteredUser(user)}
            />
          )}
        </div>

        {!registeredUser && (
          <p className="mt-6 text-center text-xs text-muted-foreground">
            Already have an account?{' '}
            <Link
              to="/login"
              id="register-login-link"
              className="font-medium text-foreground underline-offset-4 hover:underline"
            >
              Sign in
            </Link>
          </p>
        )}
      </div>
    </div>
  );
}
