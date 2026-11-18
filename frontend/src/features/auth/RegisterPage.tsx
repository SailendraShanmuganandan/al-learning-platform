import { useState } from 'react';
import { Link } from 'react-router-dom';
import { registerUser, type UserResponse } from '../../api/authApi';
import { RegisterForm } from './RegisterForm';
import { Mail, ArrowRight } from 'lucide-react';

export default function RegisterPage() {
  const [registeredUser, setRegisteredUser] = useState<UserResponse | null>(null);

  return (
    <div>
      <div className="mb-6 text-center">
        <h2 className="text-xl font-bold tracking-tight text-foreground">
          {registeredUser ? 'Account created' : 'Create your student account'}
        </h2>
        <p className="text-xs text-muted-foreground mt-1">
          {registeredUser
            ? 'Verify your email to begin your A/L ICT preparation'
            : 'Get started with quizzes, notes and practice tracking'}
        </p>
      </div>

      {registeredUser ? (
        <div className="flex flex-col items-center text-center gap-4 py-2">
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-primary/10 text-primary">
            <Mail className="h-8 w-8" />
          </div>
          <h3 className="text-base font-bold text-foreground">
            Check your email to verify your account
          </h3>
          <p className="text-xs text-muted-foreground leading-relaxed">
            We&apos;ve sent a verification link to{' '}
            <strong className="text-foreground">{registeredUser.email}</strong>.
            Click the link in that email to activate your account.
          </p>
          <div className="rounded-xl bg-muted/60 p-4 text-xs text-muted-foreground text-left w-full space-y-1">
            <p className="font-semibold text-foreground">Didn&apos;t receive it?</p>
            <p>Check your spam folder, or try signing in to resend the verification email.</p>
          </div>
          <Link
            to="/login"
            className="inline-flex items-center gap-2 text-xs font-semibold text-primary hover:underline mt-2"
          >
            <span>Proceed to Sign In</span>
            <ArrowRight size={14} />
          </Link>
        </div>
      ) : (
        <>
          <RegisterForm onSubmit={registerUser} onSuccess={(user) => setRegisteredUser(user)} />
          <div className="mt-6 text-center text-xs text-muted-foreground">
            Already have an account?{' '}
            <Link
              to="/login"
              className="font-semibold text-primary hover:underline"
            >
              Sign in
            </Link>
          </div>
        </>
      )}
    </div>
  );
}
