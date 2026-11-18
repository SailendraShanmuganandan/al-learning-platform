import { apiFetch } from './authApi';

export interface DashboardSummary {
  users: {
    totalStudents: number;
    newStudentsLast30Days: number;
    unverifiedStudents: number;
  } | null;
  sales: {
    revenueToday: number;
    revenueLast7Days: number;
    revenueLast30Days: number;
    revenueAllTime: number;
    paidOrdersCount: number;
    awaitingPaymentCount: number;
  } | null;
  pendingActions: {
    ordersAwaitingFulfillment: number;
    paymentsNeedingReview: number;
    pendingClassRegistrations: number;
    unreadContactMessages: number;
    pendingQuestions?: number;
    openQuestionReports?: number;
    failedGenerationJobs?: number;
  } | null;
  catalog: {
    publishedNotes: number;
    publishedVideos: number;
    draftProducts: number;
    lowStockCount: number;
    lowStockItems: Array<{
      productId: number;
      name: string;
      stock: number;
    }>;
  } | null;
  classes: {
    activeClasses: number;
    pendingRegistrations: number;
    confirmedRegistrations: number;
  } | null;
  communication: {
    publishedAnnouncements: number;
    draftAnnouncements: number;
  } | null;
  recent: {
    orders: Array<{
      orderId: number;
      customerName: string;
      total: number;
      paymentStatus: string;
      createdAt: string;
    }>;
    classRegistrations: Array<{
      registrationId: number;
      studentName: string;
      className: string;
      status: string;
      createdAt: string;
    }>;
    contactMessages: Array<{
      messageId: number;
      senderName: string;
      status: string;
      createdAt: string;
    }>;
  } | null;
  sectionErrors: Record<string, string>;
  generatedAt: string;
}

export interface DailyRevenueEntry {
  date: string;
  revenue: number;
  paidOrderCount: number;
}

export interface RevenueSeries {
  entries: DailyRevenueEntry[];
  totalRevenue: number;
  totalPaidOrders: number;
}

export interface TopProduct {
  productId: number;
  name: string;
  type: string;
  quantitySold: number;
  revenue: number;
}

export async function getDashboardSummary(): Promise<DashboardSummary> {
  return apiFetch<DashboardSummary>('/api/admin/dashboard/summary');
}

export async function getDashboardRevenue(days = 30): Promise<RevenueSeries> {
  return apiFetch<RevenueSeries>(`/api/admin/dashboard/revenue?days=${days}`);
}

export async function getDashboardTopProducts(days = 30, limit = 5): Promise<TopProduct[]> {
  return apiFetch<TopProduct[]>(`/api/admin/dashboard/top-products?days=${days}&limit=${limit}`);
}
