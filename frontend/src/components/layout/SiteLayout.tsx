import { useAuth } from '../../contexts/AuthContext';
import StudentLayout from './StudentLayout';
import PublicLayout from './PublicLayout';

/**
 * SiteLayout picks the outer shell based on authentication state:
 * - Guest: PublicLayout (sticky top navbar + footer)
 * - Logged-in STUDENT: StudentLayout (sidebar + slim topbar)
 * - Logged-in ADMIN: PublicLayout (with "Admin Console" badge in navbar)
 */
export default function SiteLayout() {
  const { isAuthenticated, user } = useAuth();

  if (isAuthenticated && user?.role === 'STUDENT') {
    return <StudentLayout />;
  }

  return <PublicLayout />;
}
