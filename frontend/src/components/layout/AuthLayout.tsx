import { Link, Outlet } from 'react-router-dom';

export default function AuthLayout() {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-background px-4 py-12 relative overflow-hidden">
      {/* Background decorative gradient */}
      <div
        aria-hidden
        className="pointer-events-none fixed inset-0 -z-10 overflow-hidden"
      >
        <div className="absolute -top-40 -right-40 h-[600px] w-[600px] rounded-full bg-primary/5 blur-3xl" />
        <div className="absolute -bottom-40 -left-40 h-[500px] w-[500px] rounded-full bg-primary/5 blur-3xl" />
      </div>

      <div className="w-full max-w-md">
        {/* Wordmark on top */}
        <div className="mb-8 flex flex-col items-center gap-2 text-center">
          <Link to="/" className="flex flex-col items-center gap-2 group">
            <div
              className="flex h-12 w-12 items-center justify-center rounded-2xl bg-primary text-primary-foreground text-xl font-bold shadow-lg transition-transform group-hover:scale-105"
              aria-hidden
            >
              AL
            </div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">
              AL Learning Platform
            </h1>
          </Link>
        </div>

        {/* Centered card with child outlet */}
        <div className="rounded-2xl border border-border bg-card p-8 shadow-sm">
          <Outlet />
        </div>
      </div>
    </div>
  );
}
