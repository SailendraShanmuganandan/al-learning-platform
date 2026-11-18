import type React from 'react';
import {
  LayoutDashboard,
  Compass,
  BookOpen,
  BarChart3,
  Library,
  Sparkles,
  HelpCircle,
  History,
  Award,
  Clock,
  MessageSquare,
  ShoppingBag,
  Package,
  GraduationCap,
  CheckCircle2,
  Megaphone,
  Mail,
} from 'lucide-react';

export interface NavItem {
  label: string;
  path: string;
  icon: React.ElementType;
  badge?: string;
  exact?: boolean;
}

export interface NavGroup {
  group: string;
  items: NavItem[];
}

export const STUDENT_NAV_CONFIG: NavGroup[] = [
  {
    group: 'Learn',
    items: [
      { label: 'Dashboard', path: '/dashboard', icon: LayoutDashboard, exact: true },
      { label: 'Learning Overview', path: '/learning-overview', icon: Compass },
      { label: 'Subjects', path: '/subjects', icon: BookOpen },
      { label: 'Syllabus Progress', path: '/syllabus-progress', icon: BarChart3 },
      { label: 'My Videos', path: '/my-videos', icon: Library },
    ],
  },
  {
    group: 'Practice',
    items: [
      { label: 'Diagnostic Assessment', path: '/diagnostic', icon: Sparkles },
      { label: 'Overall Quiz', path: '/overall-quiz', icon: HelpCircle },
      { label: 'My Quiz Attempts', path: '/my-quizzes', icon: History },
      { label: 'My Mistakes', path: '/my-mistakes', icon: Award },
      { label: 'Focused Study Session', path: '/study-session', icon: Clock },
      { label: 'Chapter AI Chat History', path: '/chat-history', icon: MessageSquare },
    ],
  },
  {
    group: 'Shop',
    items: [
      { label: 'Notes & Videos', path: '/products', icon: ShoppingBag },
      { label: 'My Orders', path: '/orders', icon: Package },
    ],
  },
  {
    group: 'Classes',
    items: [
      { label: 'Browse Classes', path: '/classes', icon: GraduationCap },
      { label: 'My Class Registrations', path: '/my-class-registrations', icon: CheckCircle2 },
    ],
  },
  {
    group: 'Updates',
    items: [
      { label: 'Announcements', path: '/announcements', icon: Megaphone },
      { label: 'Contact', path: '/contact', icon: Mail },
    ],
  },
];
