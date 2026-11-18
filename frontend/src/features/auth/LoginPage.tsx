import { useEffect } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { LoginForm } from './LoginForm';

export default function LoginPage() {
  const { login, isAuthenticated, user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  // Where to go after login — the protected page or returnUrl they originally tried to visit
  const searchParams = new URLSearchParams(location.search);
  const returnUrl = searchParams.get('returnUrl');
  const defaultDestination = user?.role === 'ADMIN' ? '/admin' : '/dashboard';
  const from: string =
    returnUrl ||
    (location.state as { from?: { pathname: string } })?.from?.pathname ||
    defaultDestination;

  // If already authenticated, redirect out of the login page immediately
  useEffect(() => {
    if (isAuthenticated) {
      navigate(from, { replace: true });
    }
  }, [isAuthenticated, navigate, from]);

  return (
    <div>
      <div className="mb-6 text-center">
        <h2 className="text-xl font-bold tracking-tight text-foreground">Sign in to your account</h2>
        <p className="text-xs text-muted-foreground mt-1">Enter your credentials to continue</p>
      </div>

      <LoginForm onSubmit={login} />

      <div className="mt-6 text-center text-xs text-muted-foreground">
        Don&apos;t have an account?{' '}
        <Link
          to="/register"
          className="font-semibold text-primary hover:underline"
        >
          Register here
        </Link>
      </div>
    </div>
  );
}
