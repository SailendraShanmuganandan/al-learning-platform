import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import {
  getDashboardSummary,
  getDashboardRevenue,
  getDashboardTopProducts,
  type DashboardSummary,
  type RevenueSeries,
  type TopProduct,
} from '../../../api/adminDashboardApi';
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
} from 'recharts';
import {
  Users,
  TrendingUp,
  ShoppingBag,
  Clock,
  AlertTriangle,
  Mail,
  GraduationCap,
  ExternalLink,
  RefreshCw,
  AlertCircle,
  Package,
  ChevronRight,
  Sparkles,
  CheckCircle2,
  DollarSign,
  ArrowUpRight,
  FileQuestion,
  Flag,
} from 'lucide-react';

function formatLKR(amount?: number | null): string {
  if (amount == null) return 'LKR 0.00';
  return (
    'LKR ' +
    Number(amount).toLocaleString('en-US', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    })
  );
}

function formatDateTime(dateStr?: string | null): string {
  if (!dateStr) return '—';
  try {
    return new Date(dateStr).toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return dateStr;
  }
}

function formatDateShort(dateStr?: string | null): string {
  if (!dateStr) return '';
  try {
    const [year, month, day] = dateStr.split('-');
    if (month && day) {
      const d = new Date(parseInt(year, 10), parseInt(month, 10) - 1, parseInt(day, 10));
      return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
    }
    return dateStr;
  } catch {
    return dateStr;
  }
}

export default function AdminDashboardPage() {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [revenueSeries, setRevenueSeries] = useState<RevenueSeries | null>(null);
  const [topProducts, setTopProducts] = useState<TopProduct[]>([]);

  const [loadingSummary, setLoadingSummary] = useState(true);
  const [loadingRevenue, setLoadingRevenue] = useState(true);
  const [loadingTopProducts, setLoadingTopProducts] = useState(true);

  const [summaryError, setSummaryError] = useState<string | null>(null);
  const [revenueError, setRevenueError] = useState<string | null>(null);
  const [topProductsError, setTopProductsError] = useState<string | null>(null);

  const [chartDays, setChartDays] = useState<number>(30);
  const [topProductsDays, setTopProductsDays] = useState<number>(30);
  const [lastRefreshed, setLastRefreshed] = useState<Date>(new Date());
  const [refreshing, setRefreshing] = useState(false);

  const loadSummary = useCallback(async () => {
    setLoadingSummary(true);
    setSummaryError(null);
    try {
      const data = await getDashboardSummary();
      setSummary(data);
      setLastRefreshed(new Date());
    } catch (err: unknown) {
      const e = err as { message?: string };
      setSummaryError(e.message || 'Failed to load dashboard summary');
    } finally {
      setLoadingSummary(false);
    }
  }, []);

  const loadRevenue = useCallback(async (days: number) => {
    setLoadingRevenue(true);
    setRevenueError(null);
    try {
      const data = await getDashboardRevenue(days);
      setRevenueSeries(data);
    } catch (err: unknown) {
      const e = err as { message?: string };
      setRevenueError(e.message || 'Failed to load revenue series');
    } finally {
      setLoadingRevenue(false);
    }
  }, []);

  const loadTopProducts = useCallback(async (days: number) => {
    setLoadingTopProducts(true);
    setTopProductsError(null);
    try {
      const data = await getDashboardTopProducts(days, 5);
      setTopProducts(data);
    } catch (err: unknown) {
      const e = err as { message?: string };
      setTopProductsError(e.message || 'Failed to load top products');
    } finally {
      setLoadingTopProducts(false);
    }
  }, []);

  useEffect(() => {
    loadSummary();
  }, [loadSummary]);

  useEffect(() => {
    loadRevenue(chartDays);
  }, [chartDays, loadRevenue]);

  useEffect(() => {
    loadTopProducts(topProductsDays);
  }, [topProductsDays, loadTopProducts]);

  async function handleRefreshAll() {
    setRefreshing(true);
    await Promise.all([
      loadSummary(),
      loadRevenue(chartDays),
      loadTopProducts(topProductsDays),
    ]);
    setRefreshing(false);
  }

  return (
    <div className="space-y-8 max-w-7xl mx-auto pb-12">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-2 border-b border-border">
        <div>
          <div className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-primary">
            <Sparkles size={14} />
            <span>Platform Overview</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-foreground tracking-tight mt-1">
            Admin Dashboard
          </h1>
          <p className="text-xs sm:text-sm text-muted-foreground mt-0.5">
            Key metrics, revenue trends, low stock alerts, and urgent pending actions.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <div className="text-right hidden sm:block">
            <span className="text-[11px] text-muted-foreground block">Last updated</span>
            <span className="text-xs font-medium text-foreground">
              {lastRefreshed.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
            </span>
          </div>

          <button
            type="button"
            onClick={handleRefreshAll}
            disabled={refreshing}
            id="dashboard-refresh-btn"
            className="flex items-center gap-1.5 rounded-xl border border-border bg-card px-3.5 py-2 text-xs font-semibold text-foreground shadow-sm hover:bg-muted transition disabled:opacity-50"
          >
            <RefreshCw size={13} className={refreshing ? 'animate-spin text-primary' : ''} />
            <span>{refreshing ? 'Refreshing…' : 'Refresh'}</span>
          </button>
        </div>
      </div>

      {/* Global Error Notice if Summary Failed completely */}
      {summaryError && (
        <div className="rounded-2xl border border-destructive/30 bg-destructive/10 p-4 flex items-center justify-between text-xs text-destructive">
          <div className="flex items-center gap-2">
            <AlertCircle size={16} />
            <span>{summaryError}</span>
          </div>
          <button
            type="button"
            onClick={loadSummary}
            className="font-semibold underline hover:no-underline"
          >
            Retry
          </button>
        </div>
      )}

      {/* ── SECTION 1: PRIMARY METRICS CARDS ── */}
      <section className="space-y-4">
        <h2 className="text-xs font-bold uppercase tracking-wider text-muted-foreground">
          Core Metrics
        </h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {/* Total Students Card */}
          <div className="rounded-2xl border border-border bg-card p-5 shadow-sm relative overflow-hidden">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-muted-foreground">Total Students</span>
              <div className="h-8 w-8 rounded-lg bg-primary/10 text-primary flex items-center justify-center">
                <Users size={16} />
              </div>
            </div>
            {loadingSummary ? (
              <div className="h-14 animate-pulse bg-muted/60 rounded-lg mt-3" />
            ) : summary?.users ? (
              <div className="mt-2">
                <div className="text-2xl sm:text-3xl font-extrabold text-foreground tracking-tight">
                  {summary.users.totalStudents.toLocaleString()}
                </div>
                <div className="flex items-center gap-2 mt-2 text-[11px] text-muted-foreground">
                  <span className="inline-flex items-center gap-0.5 text-emerald-600 dark:text-emerald-400 font-semibold">
                    <TrendingUp size={11} />
                    +{summary.users.newStudentsLast30Days}
                  </span>
                  <span>new in 30 days</span>
                  {summary.users.unverifiedStudents > 0 && (
                    <span className="ml-auto text-amber-600 dark:text-amber-400">
                      {summary.users.unverifiedStudents} unverified
                    </span>
                  )}
                </div>
              </div>
            ) : (
              <div className="mt-3 text-xs text-muted-foreground flex items-center justify-between">
                <span>Couldn&apos;t load user metrics</span>
                <button type="button" onClick={loadSummary} className="text-primary font-semibold">Retry</button>
              </div>
            )}
          </div>

          {/* Revenue Overview Card */}
          <div className="rounded-2xl border border-border bg-card p-5 shadow-sm relative overflow-hidden">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-muted-foreground">Revenue (30 Days)</span>
              <div className="h-8 w-8 rounded-lg bg-emerald-500/10 text-emerald-600 flex items-center justify-center">
                <DollarSign size={16} />
              </div>
            </div>
            {loadingSummary ? (
              <div className="h-14 animate-pulse bg-muted/60 rounded-lg mt-3" />
            ) : summary?.sales ? (
              <div className="mt-2">
                <div className="text-2xl sm:text-3xl font-extrabold text-foreground tracking-tight">
                  {formatLKR(summary.sales.revenueLast30Days)}
                </div>
                <div className="flex items-center justify-between mt-2 text-[11px] text-muted-foreground">
                  <span>Today: <strong className="text-foreground">{formatLKR(summary.sales.revenueToday)}</strong></span>
                  <span>7D: <strong className="text-foreground">{formatLKR(summary.sales.revenueLast7Days)}</strong></span>
                </div>
              </div>
            ) : (
              <div className="mt-3 text-xs text-muted-foreground flex items-center justify-between">
                <span>Couldn&apos;t load sales metrics</span>
                <button type="button" onClick={loadSummary} className="text-primary font-semibold">Retry</button>
              </div>
            )}
          </div>

          {/* Paid Orders Card */}
          <div className="rounded-2xl border border-border bg-card p-5 shadow-sm relative overflow-hidden">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-muted-foreground">Paid Orders</span>
              <div className="h-8 w-8 rounded-lg bg-blue-500/10 text-blue-600 flex items-center justify-center">
                <ShoppingBag size={16} />
              </div>
            </div>
            {loadingSummary ? (
              <div className="h-14 animate-pulse bg-muted/60 rounded-lg mt-3" />
            ) : summary?.sales ? (
              <div className="mt-2">
                <div className="text-2xl sm:text-3xl font-extrabold text-foreground tracking-tight">
                  {summary.sales.paidOrdersCount.toLocaleString()}
                </div>
                <div className="flex items-center justify-between mt-2 text-[11px] text-muted-foreground">
                  <span>
                    Awaiting payment: <strong className="text-foreground">{summary.sales.awaitingPaymentCount}</strong>
                  </span>
                  <span>All-time: <strong className="text-foreground">{formatLKR(summary.sales.revenueAllTime)}</strong></span>
                </div>
              </div>
            ) : (
              <div className="mt-3 text-xs text-muted-foreground flex items-center justify-between">
                <span>Couldn&apos;t load order metrics</span>
                <button type="button" onClick={loadSummary} className="text-primary font-semibold">Retry</button>
              </div>
            )}
          </div>
        </div>
      </section>

      {/* ── SECTION 2: PENDING OPERATIONAL ACTIONS ── */}
      <section className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-xs font-bold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5">
            <Clock size={13} />
            <span>Pending Operational Actions</span>
          </h2>
          <span className="text-[11px] text-muted-foreground">Click card to jump to filtered console</span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {/* 1. Orders Awaiting Fulfillment */}
          <Link
            to="/admin/orders?paymentStatus=PAID&fulfillmentStatus=PENDING"
            id="pending-action-fulfillment"
            className="rounded-2xl border border-border bg-card p-4 shadow-sm hover:border-primary/50 hover:shadow-md transition group flex flex-col justify-between"
          >
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-muted-foreground group-hover:text-foreground transition">
                Awaiting Fulfillment
              </span>
              <div className="h-7 w-7 rounded-lg bg-amber-500/10 text-amber-600 flex items-center justify-center">
                <ShoppingBag size={14} />
              </div>
            </div>
            <div className="mt-3 flex items-baseline justify-between">
              <span className="text-2xl font-black text-foreground">
                {loadingSummary ? '…' : (summary?.pendingActions?.ordersAwaitingFulfillment ?? 0)}
              </span>
              <span className="text-[11px] text-primary flex items-center gap-0.5 group-hover:underline">
                View orders <ChevronRight size={12} />
              </span>
            </div>
          </Link>

          {/* 2. Payments Needing Review */}
          <Link
            to="/admin/payments?needsReview=true"
            id="pending-action-payments"
            className="rounded-2xl border border-border bg-card p-4 shadow-sm hover:border-primary/50 hover:shadow-md transition group flex flex-col justify-between"
          >
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-muted-foreground group-hover:text-foreground transition">
                Payments Review
              </span>
              <div className="h-7 w-7 rounded-lg bg-destructive/10 text-destructive flex items-center justify-center">
                <AlertTriangle size={14} />
              </div>
            </div>
            <div className="mt-3 flex items-baseline justify-between">
              <span className="text-2xl font-black text-foreground">
                {loadingSummary ? '…' : (summary?.pendingActions?.paymentsNeedingReview ?? 0)}
              </span>
              <span className="text-[11px] text-primary flex items-center gap-0.5 group-hover:underline">
                Review <ChevronRight size={12} />
              </span>
            </div>
          </Link>

          {/* 3. Pending Class Registrations */}
          <Link
            to="/admin/class-registrations?status=PENDING"
            id="pending-action-registrations"
            className="rounded-2xl border border-border bg-card p-4 shadow-sm hover:border-primary/50 hover:shadow-md transition group flex flex-col justify-between"
          >
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-muted-foreground group-hover:text-foreground transition">
                Pending Registrations
              </span>
              <div className="h-7 w-7 rounded-lg bg-indigo-500/10 text-indigo-600 flex items-center justify-center">
                <GraduationCap size={14} />
              </div>
            </div>
            <div className="mt-3 flex items-baseline justify-between">
              <span className="text-2xl font-black text-foreground">
                {loadingSummary ? '…' : (summary?.pendingActions?.pendingClassRegistrations ?? 0)}
              </span>
              <span className="text-[11px] text-primary flex items-center gap-0.5 group-hover:underline">
                Approve <ChevronRight size={12} />
              </span>
            </div>
          </Link>

          {/* 4. Unread Contact Messages */}
          <Link
            to="/admin/contact-messages?status=UNREAD"
            id="pending-action-messages"
            className="rounded-2xl border border-border bg-card p-4 shadow-sm hover:border-primary/50 hover:shadow-md transition group flex flex-col justify-between"
          >
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-muted-foreground group-hover:text-foreground transition">
                Unread Messages
              </span>
              <div className="h-7 w-7 rounded-lg bg-blue-500/10 text-blue-600 flex items-center justify-center">
                <Mail size={14} />
              </div>
            </div>
            <div className="mt-3 flex items-baseline justify-between">
              <span className="text-2xl font-black text-foreground">
                {loadingSummary ? '…' : (summary?.pendingActions?.unreadContactMessages ?? 0)}
              </span>
              <span className="text-[11px] text-primary flex items-center gap-0.5 group-hover:underline">
                Inbox <ChevronRight size={12} />
              </span>
            </div>
          </Link>

          {/* 5. Questions Awaiting Approval */}
          <Link
            to="/admin/question-bank?status=PENDING"
            id="pending-action-questions"
            className="rounded-2xl border border-border bg-card p-4 shadow-sm hover:border-primary/50 hover:shadow-md transition group flex flex-col justify-between"
          >
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-muted-foreground group-hover:text-foreground transition">
                Questions Awaiting Approval
              </span>
              <div className="h-7 w-7 rounded-lg bg-amber-500/10 text-amber-600 flex items-center justify-center">
                <FileQuestion size={14} />
              </div>
            </div>
            <div className="mt-3 flex items-baseline justify-between">
              <span className="text-2xl font-black text-foreground">
                {loadingSummary ? '…' : (summary?.pendingActions?.pendingQuestions ?? 0)}
              </span>
              <span className="text-[11px] text-primary flex items-center gap-0.5 group-hover:underline">
                Review questions <ChevronRight size={12} />
              </span>
            </div>
          </Link>

          {/* 6. Open Question Reports */}
          <Link
            to="/admin/question-reports?status=OPEN"
            id="pending-action-reports"
            className="rounded-2xl border border-border bg-card p-4 shadow-sm hover:border-primary/50 hover:shadow-md transition group flex flex-col justify-between"
          >
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-muted-foreground group-hover:text-foreground transition">
                Open Question Reports
              </span>
              <div className="h-7 w-7 rounded-lg bg-rose-500/10 text-rose-600 flex items-center justify-center">
                <Flag size={14} />
              </div>
            </div>
            <div className="mt-3 flex items-baseline justify-between">
              <span className="text-2xl font-black text-foreground">
                {loadingSummary ? '…' : (summary?.pendingActions?.openQuestionReports ?? 0)}
              </span>
              <span className="text-[11px] text-primary flex items-center gap-0.5 group-hover:underline">
                View reports <ChevronRight size={12} />
              </span>
            </div>
          </Link>

          {/* 7. Failed AI Generation Jobs (only shown when > 0) */}
          {(summary?.pendingActions?.failedGenerationJobs ?? 0) > 0 && (
            <div
              id="pending-action-failed-generation"
              title="Available with AI generation"
              className="rounded-2xl border border-dashed border-destructive/40 bg-card/60 p-4 shadow-sm flex flex-col justify-between cursor-not-allowed opacity-90"
            >
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-muted-foreground">
                  Failed Generation Jobs
                </span>
                <div className="h-7 w-7 rounded-lg bg-destructive/10 text-destructive flex items-center justify-center">
                  <Sparkles size={14} />
                </div>
              </div>
              <div className="mt-3 flex items-baseline justify-between">
                <span className="text-2xl font-black text-destructive">
                  {summary?.pendingActions?.failedGenerationJobs ?? 0}
                </span>
                <span className="text-[11px] text-muted-foreground italic">
                  Available with AI generation
                </span>
              </div>
            </div>
          )}
        </div>
      </section>

      {/* ── SECTION 3: REVENUE CHART & TOP PRODUCTS ── */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Revenue Chart (2 cols) */}
        <div className="lg:col-span-2 rounded-2xl border border-border bg-card p-6 shadow-sm flex flex-col justify-between">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mb-6">
            <div>
              <h3 className="text-sm font-bold text-foreground">Revenue Trend</h3>
              <p className="text-xs text-muted-foreground mt-0.5">
                Total for period:{' '}
                <strong className="text-foreground">
                  {formatLKR(revenueSeries?.totalRevenue)}
                </strong>{' '}
                across {revenueSeries?.totalPaidOrders ?? 0} paid order(s)
              </p>
            </div>

            {/* Days range selector: 7, 30, 90 */}
            <div className="inline-flex rounded-xl bg-muted p-1 border border-border/60 self-start sm:self-auto">
              {[7, 30, 90].map(days => (
                <button
                  key={days}
                  type="button"
                  onClick={() => setChartDays(days)}
                  className={`rounded-lg px-2.5 py-1 text-xs font-semibold transition ${
                    chartDays === days
                      ? 'bg-card text-foreground shadow-sm'
                      : 'text-muted-foreground hover:text-foreground'
                  }`}
                >
                  {days}D
                </button>
              ))}
            </div>
          </div>

          {loadingRevenue ? (
            <div className="h-64 animate-pulse bg-muted/40 rounded-xl flex items-center justify-center text-xs text-muted-foreground">
              Loading revenue chart…
            </div>
          ) : revenueError ? (
            <div className="h-64 rounded-xl border border-dashed border-border flex flex-col items-center justify-center gap-2 text-xs text-muted-foreground">
              <span>{revenueError}</span>
              <button
                type="button"
                onClick={() => loadRevenue(chartDays)}
                className="text-primary font-semibold underline"
              >
                Retry
              </button>
            </div>
          ) : !revenueSeries?.entries || revenueSeries.entries.length === 0 ? (
            <div className="h-64 rounded-xl border border-dashed border-border flex items-center justify-center text-xs text-muted-foreground">
              No sales in this period
            </div>
          ) : (
            <div className="h-64 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart
                  data={revenueSeries.entries}
                  margin={{ top: 10, right: 10, left: 0, bottom: 0 }}
                >
                  <defs>
                    <linearGradient id="revenueGrad" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="var(--color-primary)" stopOpacity={0.3} />
                      <stop offset="95%" stopColor="var(--color-primary)" stopOpacity={0.0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" opacity={0.15} vertical={false} />
                  <XAxis
                    dataKey="date"
                    tickFormatter={formatDateShort}
                    tick={{ fontSize: 11, fill: 'currentColor' }}
                    className="text-muted-foreground"
                    axisLine={false}
                    tickLine={false}
                  />
                  <YAxis
                    tickFormatter={val => `${val / 1000}k`}
                    tick={{ fontSize: 11, fill: 'currentColor' }}
                    className="text-muted-foreground"
                    axisLine={false}
                    tickLine={false}
                    width={40}
                  />
                  <Tooltip
                    content={({ active, payload }) => {
                      if (active && payload && payload.length) {
                        const data = payload[0].payload as { date: string; revenue: number; paidOrderCount: number };
                        return (
                          <div className="rounded-xl border border-border bg-popover p-3 shadow-lg text-xs space-y-1">
                            <p className="font-semibold text-foreground">{formatDateShort(data.date)}</p>
                            <p className="text-primary font-bold">{formatLKR(data.revenue)}</p>
                            <p className="text-muted-foreground text-[11px]">{data.paidOrderCount} order(s)</p>
                          </div>
                        );
                      }
                      return null;
                    }}
                  />
                  <Area
                    type="monotone"
                    dataKey="revenue"
                    stroke="var(--color-primary)"
                    strokeWidth={2}
                    fillOpacity={1}
                    fill="url(#revenueGrad)"
                  />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>

        {/* Top Products Table (1 col) */}
        <div className="rounded-2xl border border-border bg-card p-6 shadow-sm flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-sm font-bold text-foreground">Top Products</h3>
              <div className="inline-flex rounded-xl bg-muted p-1 border border-border/60">
                {[7, 30, 90].map(days => (
                  <button
                    key={days}
                    type="button"
                    onClick={() => setTopProductsDays(days)}
                    className={`rounded-lg px-2 py-0.5 text-[11px] font-semibold transition ${
                      topProductsDays === days
                        ? 'bg-card text-foreground shadow-sm'
                        : 'text-muted-foreground hover:text-foreground'
                    }`}
                  >
                    {days}D
                  </button>
                ))}
              </div>
            </div>

            {loadingTopProducts ? (
              <div className="space-y-3">
                {[...Array(4)].map((_, i) => (
                  <div key={i} className="h-10 animate-pulse bg-muted/50 rounded-xl" />
                ))}
              </div>
            ) : topProductsError ? (
              <div className="py-8 text-center text-xs text-muted-foreground">
                <p>{topProductsError}</p>
                <button
                  type="button"
                  onClick={() => loadTopProducts(topProductsDays)}
                  className="mt-2 text-primary font-semibold underline"
                >
                  Retry
                </button>
              </div>
            ) : topProducts.length === 0 ? (
              <div className="py-12 text-center text-xs text-muted-foreground border border-dashed border-border rounded-xl">
                No products sold in this period
              </div>
            ) : (
              <div className="divide-y divide-border/60 text-xs">
                {topProducts.map(p => (
                  <div key={p.productId} className="py-2.5 flex items-center justify-between gap-3">
                    <div className="min-w-0 flex-1">
                      <p className="font-semibold text-foreground truncate">{p.name}</p>
                      <div className="flex items-center gap-2 mt-0.5 text-[11px] text-muted-foreground">
                        <span className="rounded bg-muted px-1.5 py-0.2 font-medium">
                          {p.type === 'PHYSICAL_NOTE' ? 'Notes' : 'Video'}
                        </span>
                        <span>{p.quantitySold} sold</span>
                      </div>
                    </div>
                    <div className="font-bold text-foreground whitespace-nowrap">
                      {formatLKR(p.revenue)}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="pt-4 border-t border-border/60 mt-4">
            <Link
              to="/admin/products"
              className="text-xs text-primary font-semibold flex items-center gap-1 hover:underline justify-center"
            >
              Manage Catalog <ArrowUpRight size={13} />
            </Link>
          </div>
        </div>
      </div>

      {/* ── SECTION 4: LOW STOCK & INVENTORY ALERT ── */}
      <section className="rounded-2xl border border-border bg-card p-6 shadow-sm">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 mb-4 pb-3 border-b border-border/60">
          <div>
            <div className="flex items-center gap-2">
              <h3 className="text-sm font-bold text-foreground">Low Stock Inventory</h3>
              {summary?.catalog && summary.catalog.lowStockCount > 0 && (
                <span className="rounded-full bg-amber-500/15 text-amber-700 dark:text-amber-400 font-extrabold px-2 py-0.5 text-[11px]">
                  {summary.catalog.lowStockCount} item(s) low
                </span>
              )}
            </div>
            <p className="text-xs text-muted-foreground mt-0.5">
              Published physical notes at or below stock threshold (&le; 5). Lowest stock first.
            </p>
          </div>

          <Link
            to="/admin/products"
            className="text-xs text-primary font-semibold flex items-center gap-1 hover:underline shrink-0"
          >
            All Products <ChevronRight size={13} />
          </Link>
        </div>

        {loadingSummary ? (
          <div className="space-y-3">
            {[...Array(3)].map((_, i) => (
              <div key={i} className="h-10 animate-pulse bg-muted/50 rounded-xl" />
            ))}
          </div>
        ) : !summary?.catalog?.lowStockItems || summary.catalog.lowStockItems.length === 0 ? (
          <div className="py-6 text-center text-xs text-emerald-600 dark:text-emerald-400 flex items-center justify-center gap-2">
            <CheckCircle2 size={16} />
            <span>All published notes are sufficiently stocked. No low-stock items.</span>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
            {summary.catalog.lowStockItems.map(item => (
              <div
                key={item.productId}
                className="rounded-xl border border-border/70 p-3.5 bg-muted/20 flex items-center justify-between gap-3"
              >
                <div className="min-w-0">
                  <h4 className="font-semibold text-xs text-foreground truncate" title={item.name}>
                    {item.name}
                  </h4>
                  <p className="text-[11px] text-muted-foreground mt-0.5">ID: #{item.productId}</p>
                </div>
                <div className="flex items-center gap-2">
                  <span
                    className={`rounded-lg px-2 py-1 text-xs font-bold ${
                      item.stock === 0
                        ? 'bg-destructive/15 text-destructive'
                        : 'bg-amber-500/15 text-amber-700 dark:text-amber-300'
                    }`}
                  >
                    {item.stock} left
                  </span>
                  <Link
                    to="/admin/products"
                    className="p-1 text-muted-foreground hover:text-foreground rounded-md hover:bg-muted"
                    title="Edit stock in Catalog"
                  >
                    <ExternalLink size={14} />
                  </Link>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* ── SECTION 5: THREE RECENT PANELS (Orders, Class Registrations, Contact Messages) ── */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Recent Orders Panel */}
        <div className="rounded-2xl border border-border bg-card p-5 shadow-sm flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between pb-3 border-b border-border/60 mb-3">
              <h3 className="text-xs font-bold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5">
                <ShoppingBag size={14} />
                <span>Recent Orders</span>
              </h3>
              <Link to="/admin/orders" className="text-xs text-primary font-semibold hover:underline">
                View all
              </Link>
            </div>

            {loadingSummary ? (
              <div className="space-y-3">
                {[...Array(4)].map((_, i) => (
                  <div key={i} className="h-12 animate-pulse bg-muted/50 rounded-xl" />
                ))}
              </div>
            ) : !summary?.recent?.orders || summary.recent.orders.length === 0 ? (
              <div className="py-8 text-center text-xs text-muted-foreground">
                No orders placed yet
              </div>
            ) : (
              <div className="divide-y divide-border/60">
                {summary.recent.orders.map(o => (
                  <Link
                    key={o.orderId}
                    to={`/admin/orders/${o.orderId}`}
                    className="py-2.5 flex items-center justify-between group hover:bg-muted/40 px-2 rounded-lg transition"
                  >
                    <div className="min-w-0 pr-2">
                      <p className="font-semibold text-xs text-foreground group-hover:text-primary transition truncate">
                        #{o.orderId} &bull; {o.customerName}
                      </p>
                      <p className="text-[11px] text-muted-foreground mt-0.5">
                        {formatDateTime(o.createdAt)}
                      </p>
                    </div>
                    <div className="text-right shrink-0">
                      <span className="font-bold text-xs text-foreground block">
                        {formatLKR(o.total)}
                      </span>
                      <span
                        className={`text-[10px] font-semibold uppercase px-1.5 py-0.2 rounded-full ${
                          o.paymentStatus === 'PAID'
                            ? 'bg-emerald-500/10 text-emerald-600'
                            : 'bg-muted text-muted-foreground'
                        }`}
                      >
                        {o.paymentStatus}
                      </span>
                    </div>
                  </Link>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Recent Class Registrations Panel */}
        <div className="rounded-2xl border border-border bg-card p-5 shadow-sm flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between pb-3 border-b border-border/60 mb-3">
              <h3 className="text-xs font-bold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5">
                <GraduationCap size={14} />
                <span>Class Registrations</span>
              </h3>
              <Link to="/admin/class-registrations" className="text-xs text-primary font-semibold hover:underline">
                View all
              </Link>
            </div>

            {loadingSummary ? (
              <div className="space-y-3">
                {[...Array(4)].map((_, i) => (
                  <div key={i} className="h-12 animate-pulse bg-muted/50 rounded-xl" />
                ))}
              </div>
            ) : !summary?.recent?.classRegistrations || summary.recent.classRegistrations.length === 0 ? (
              <div className="py-8 text-center text-xs text-muted-foreground">
                No class registrations yet
              </div>
            ) : (
              <div className="divide-y divide-border/60">
                {summary.recent.classRegistrations.map(r => (
                  <Link
                    key={r.registrationId}
                    to="/admin/class-registrations"
                    className="py-2.5 flex items-center justify-between group hover:bg-muted/40 px-2 rounded-lg transition"
                  >
                    <div className="min-w-0 pr-2">
                      <p className="font-semibold text-xs text-foreground group-hover:text-primary transition truncate">
                        {r.studentName}
                      </p>
                      <p className="text-[11px] text-muted-foreground truncate mt-0.5">
                        {r.className}
                      </p>
                    </div>
                    <div className="text-right shrink-0">
                      <span
                        className={`text-[10px] font-semibold uppercase px-2 py-0.5 rounded-full ${
                          r.status === 'CONFIRMED'
                            ? 'bg-emerald-500/10 text-emerald-600'
                            : r.status === 'PENDING'
                            ? 'bg-amber-500/10 text-amber-600'
                            : 'bg-muted text-muted-foreground'
                        }`}
                      >
                        {r.status}
                      </span>
                      <p className="text-[10px] text-muted-foreground mt-0.5">
                        {formatDateTime(r.createdAt)}
                      </p>
                    </div>
                  </Link>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Recent Contact Messages Panel (Strictly sender name, status, created_at - NO message text) */}
        <div className="rounded-2xl border border-border bg-card p-5 shadow-sm flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between pb-3 border-b border-border/60 mb-3">
              <h3 className="text-xs font-bold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5">
                <Mail size={14} />
                <span>Contact Inquiries</span>
              </h3>
              <Link to="/admin/contact-messages" className="text-xs text-primary font-semibold hover:underline">
                View all
              </Link>
            </div>

            {loadingSummary ? (
              <div className="space-y-3">
                {[...Array(4)].map((_, i) => (
                  <div key={i} className="h-12 animate-pulse bg-muted/50 rounded-xl" />
                ))}
              </div>
            ) : !summary?.recent?.contactMessages || summary.recent.contactMessages.length === 0 ? (
              <div className="py-8 text-center text-xs text-muted-foreground">
                No contact messages yet
              </div>
            ) : (
              <div className="divide-y divide-border/60">
                {summary.recent.contactMessages.map(m => (
                  <Link
                    key={m.messageId}
                    to="/admin/contact-messages"
                    className="py-2.5 flex items-center justify-between group hover:bg-muted/40 px-2 rounded-lg transition"
                  >
                    <div className="min-w-0 pr-2">
                      <p className="font-semibold text-xs text-foreground group-hover:text-primary transition truncate">
                        {m.senderName}
                      </p>
                      <p className="text-[11px] text-muted-foreground mt-0.5">
                        {formatDateTime(m.createdAt)}
                      </p>
                    </div>
                    <div className="text-right shrink-0">
                      <span
                        className={`text-[10px] font-semibold uppercase px-2 py-0.5 rounded-full ${
                          m.status === 'UNREAD'
                            ? 'bg-blue-500/15 text-blue-600 font-bold'
                            : 'bg-muted text-muted-foreground'
                        }`}
                      >
                        {m.status}
                      </span>
                    </div>
                  </Link>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* ── SECTION 6: DOMAIN INVENTORY SUMMARY CARDS ── */}
      <section className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* Catalog Summary */}
        <div className="rounded-2xl border border-border bg-card p-4 shadow-sm flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 rounded-xl bg-primary/10 text-primary flex items-center justify-center">
              <Package size={18} />
            </div>
            <div>
              <h4 className="text-xs font-semibold text-muted-foreground">Catalog Content</h4>
              <p className="text-sm font-bold text-foreground">
                {summary?.catalog?.publishedNotes ?? 0} Notes &bull; {summary?.catalog?.publishedVideos ?? 0} Videos
              </p>
              <p className="text-[11px] text-muted-foreground mt-0.5">
                {summary?.catalog?.draftProducts ?? 0} draft product(s)
              </p>
            </div>
          </div>
          <Link
            to="/admin/products"
            className="p-1.5 text-muted-foreground hover:text-foreground rounded-lg hover:bg-muted"
            title="Manage Catalog"
          >
            <ChevronRight size={16} />
          </Link>
        </div>

        {/* Classes Summary */}
        <div className="rounded-2xl border border-border bg-card p-4 shadow-sm flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 rounded-xl bg-indigo-500/10 text-indigo-600 flex items-center justify-center">
              <GraduationCap size={18} />
            </div>
            <div>
              <h4 className="text-xs font-semibold text-muted-foreground">Classes &amp; Tuition</h4>
              <p className="text-sm font-bold text-foreground">
                {summary?.classes?.activeClasses ?? 0} Active Classes
              </p>
              <p className="text-[11px] text-muted-foreground mt-0.5">
                {summary?.classes?.confirmedRegistrations ?? 0} confirmed student(s)
              </p>
            </div>
          </div>
          <Link
            to="/admin/classes"
            className="p-1.5 text-muted-foreground hover:text-foreground rounded-lg hover:bg-muted"
            title="Manage Classes"
          >
            <ChevronRight size={16} />
          </Link>
        </div>

        {/* Communication Summary */}
        <div className="rounded-2xl border border-border bg-card p-4 shadow-sm flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 rounded-xl bg-sky-500/10 text-sky-600 flex items-center justify-center">
              <Mail size={18} />
            </div>
            <div>
              <h4 className="text-xs font-semibold text-muted-foreground">Announcements</h4>
              <p className="text-sm font-bold text-foreground">
                {summary?.communication?.publishedAnnouncements ?? 0} Published
              </p>
              <p className="text-[11px] text-muted-foreground mt-0.5">
                {summary?.communication?.draftAnnouncements ?? 0} draft announcement(s)
              </p>
            </div>
          </div>
          <Link
            to="/admin/announcements"
            className="p-1.5 text-muted-foreground hover:text-foreground rounded-lg hover:bg-muted"
            title="Manage Announcements"
          >
            <ChevronRight size={16} />
          </Link>
        </div>
      </section>
    </div>
  );
}
