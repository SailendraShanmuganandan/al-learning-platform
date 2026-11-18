import { Link } from 'react-router-dom';
import { HelpCircle, ArrowLeft, Home } from 'lucide-react';

export default function NotFoundPage() {
  return (
    <div className="min-h-screen bg-background flex flex-col items-center justify-center px-4">
      <div className="w-full max-w-md text-center space-y-6">
        <div className="mx-auto flex h-20 w-20 items-center justify-center rounded-3xl bg-primary/10 text-primary">
          <HelpCircle size={40} />
        </div>

        <div className="space-y-2">
          <h1 className="text-3xl font-extrabold tracking-tight text-foreground">
            404 - Page Not Found
          </h1>
          <p className="text-sm text-muted-foreground leading-relaxed">
            The page you are looking for does not exist, has been removed, or has moved to another address.
          </p>
        </div>

        <div className="flex items-center justify-center gap-3 pt-2">
          <Link
            to="/"
            className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-xs font-semibold text-primary-foreground shadow-xs hover:bg-primary/90 transition"
          >
            <Home size={14} />
            <span>Go to Home</span>
          </Link>
          <Link
            to="/dashboard"
            className="inline-flex items-center gap-2 rounded-xl border border-border px-4 py-2.5 text-xs font-semibold text-foreground hover:bg-muted transition"
          >
            <ArrowLeft size={14} />
            <span>Dashboard</span>
          </Link>
        </div>
      </div>
    </div>
  );
}
