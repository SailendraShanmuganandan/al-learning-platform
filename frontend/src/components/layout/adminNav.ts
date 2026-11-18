import {
  LayoutDashboard,
  Users,
  BookOpen,
  Package,
  HelpCircle,
  FileQuestion,
  CheckSquare,
  FolderTree,
  FileText,
  Sparkles,
  Flag,
  ShoppingBag,
  CreditCard,
  GraduationCap,
  Megaphone,
  MessageSquare,
  BarChart3,
  Cpu,
  MessagesSquare,
  Settings,
} from 'lucide-react';
import type { NavGroup } from './studentNav';

export const ADMIN_NAV_CONFIG: NavGroup[] = [
  {
    group: 'Overview',
    items: [
      { label: 'Dashboard', path: '/admin', icon: LayoutDashboard, exact: true },
    ],
  },
  {
    group: 'People',
    items: [
      { label: 'Users', path: '/admin/users', icon: Users },
    ],
  },
  {
    group: 'Curriculum',
    items: [
      { label: 'Subjects', path: '/admin/subjects', icon: BookOpen },
      { label: 'Products', path: '/admin/products', icon: Package },
    ],
  },
  {
    group: 'Assessment',
    items: [
      { label: 'Quizzes', path: '/admin/quizzes', icon: HelpCircle },
      { label: 'Question Bank', path: '/admin/question-bank', icon: FileQuestion },
      { label: 'Question Review', path: '/admin/question-review', icon: CheckSquare },
      { label: 'Chapter Topics', path: '/admin/chapter-topics', icon: FolderTree },
      { label: 'Chapter Documents', path: '/admin/chapter-documents', icon: FileText },
      { label: 'AI Question Generation', path: '/admin/ai-generation', icon: Sparkles },
      { label: 'Question Reports', path: '/admin/question-reports', icon: Flag },
    ],
  },
  {
    group: 'Sales',
    items: [
      { label: 'Orders', path: '/admin/orders', icon: ShoppingBag },
      { label: 'Payments', path: '/admin/payments', icon: CreditCard },
    ],
  },
  {
    group: 'Classes',
    items: [
      { label: 'Classes', path: '/admin/classes', icon: GraduationCap },
      { label: 'Class Registrations', path: '/admin/class-registrations', icon: Users },
    ],
  },
  {
    group: 'Communication',
    items: [
      { label: 'Announcements', path: '/admin/announcements', icon: Megaphone },
      { label: 'Contact Messages', path: '/admin/contact-messages', icon: MessageSquare },
    ],
  },
  {
    group: 'Insights',
    items: [
      { label: 'Learning Analytics', path: '/admin/analytics', icon: BarChart3 },
      { label: 'AI Usage', path: '/admin/ai-usage', icon: Cpu },
      { label: 'Chat Review', path: '/admin/chat-review', icon: MessagesSquare },
    ],
  },
  {
    group: 'System',
    items: [
      { label: 'Admin Settings', path: '/admin/settings', icon: Settings },
    ],
  },
];
