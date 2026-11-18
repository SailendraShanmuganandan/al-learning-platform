import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { getPublicSubjects, type SubjectResponse } from '../../api/subjectApi';
import { getPublicProducts, type PublicProductSummaryResponse } from '../../api/productApi';
import { getPublishedAnnouncements, type AnnouncementListItemResponse } from '../../api/communicationApi';
import { PageContainer } from '../../components/layout/PageContainer';
import {
  BookOpen,
  Megaphone,
  ArrowRight,
  GraduationCap,
  Sparkles,
  CheckCircle2,
  PlayCircle,
  FileText,
} from 'lucide-react';

export default function HomePage() {
  const { isAuthenticated, user } = useAuth();
  const [subjects, setSubjects] = useState<SubjectResponse[]>([]);
  const [products, setProducts] = useState<PublicProductSummaryResponse[]>([]);
  const [announcements, setAnnouncements] = useState<AnnouncementListItemResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadData() {
      setLoading(true);
      try {
        const [subjRes, prodRes, annRes] = await Promise.all([
          getPublicSubjects().catch(() => []),
          getPublicProducts({ page: 0, size: 4 }).catch(() => ({ content: [] })),
          getPublishedAnnouncements(0, 3).catch(() => ({ content: [] })),
        ]);
        setSubjects(subjRes);
        setProducts(prodRes.content || []);
        setAnnouncements(annRes.content || []);
      } finally {
        setLoading(false);
      }
    }
    loadData();
  }, []);

  return (
    <div className="space-y-12 pb-16">
      {/* Hero Section */}
      <section className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-primary/10 via-primary/5 to-background border border-border p-8 sm:p-12 lg:p-16 my-4 mx-4 sm:mx-6 lg:mx-8">
        <div className="max-w-3xl space-y-6">
          <div className="inline-flex items-center gap-2 rounded-full border border-primary/20 bg-primary/10 px-3 py-1 text-xs font-semibold text-primary">
            <Sparkles size={14} />
            <span>Sri Lanka's Premier A/L ICT Learning Platform</span>
          </div>

          <h1 className="text-3xl sm:text-4xl lg:text-5xl font-black tracking-tight text-foreground leading-[1.15]">
            Master G.C.E. Advanced Level ICT with Precision
          </h1>

          <p className="text-base sm:text-lg text-muted-foreground leading-relaxed max-w-2xl">
            Syllabus-aligned curriculum, expert video lessons, high-yield revision notes, topic quizzes,
            and an adaptive question bank crafted to guide you to an A grade.
          </p>

          <div className="flex flex-wrap items-center gap-3 pt-2">
            {isAuthenticated ? (
              <Link
                to={user?.role === 'ADMIN' ? '/admin' : '/dashboard'}
                className="inline-flex items-center gap-2 rounded-xl bg-primary px-6 py-3 text-sm font-semibold text-primary-foreground shadow-md hover:bg-primary/90 transition"
              >
                <span>Go to {user?.role === 'ADMIN' ? 'Admin Console' : 'My Dashboard'}</span>
                <ArrowRight size={16} />
              </Link>
            ) : (
              <>
                <Link
                  to="/register"
                  className="inline-flex items-center gap-2 rounded-xl bg-primary px-6 py-3 text-sm font-semibold text-primary-foreground shadow-md hover:bg-primary/90 transition"
                >
                  <span>Get Started Free</span>
                  <ArrowRight size={16} />
                </Link>
                <Link
                  to="/login"
                  className="inline-flex items-center gap-2 rounded-xl border border-border bg-card px-6 py-3 text-sm font-semibold text-foreground hover:bg-muted transition"
                >
                  <span>Sign In</span>
                </Link>
              </>
            )}

            <Link
              to="/subjects"
              className="inline-flex items-center gap-2 rounded-xl border border-border/80 px-5 py-3 text-sm font-medium text-muted-foreground hover:text-foreground hover:bg-muted/50 transition"
            >
              <BookOpen size={16} />
              <span>Explore Curriculum</span>
            </Link>
          </div>

          {/* Quick value props */}
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-4 pt-6 border-t border-border/60">
            <div className="flex items-center gap-2 text-xs font-medium text-foreground">
              <CheckCircle2 size={16} className="text-primary shrink-0" />
              <span>Full 2026/2027 Syllabus</span>
            </div>
            <div className="flex items-center gap-2 text-xs font-medium text-foreground">
              <CheckCircle2 size={16} className="text-primary shrink-0" />
              <span>Targeted MCQs &amp; Practicals</span>
            </div>
            <div className="flex items-center gap-2 text-xs font-medium text-foreground">
              <CheckCircle2 size={16} className="text-primary shrink-0" />
              <span>Mistake Analysis &amp; Tracking</span>
            </div>
          </div>
        </div>
      </section>

      <PageContainer>
        {/* Featured Subjects */}
        <section className="space-y-6">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl sm:text-2xl font-bold tracking-tight text-foreground">Subjects &amp; Modules</h2>
              <p className="text-xs sm:text-sm text-muted-foreground mt-1">Structured chapters with notes, quizzes and progress tracking</p>
            </div>
            <Link
              to="/subjects"
              className="inline-flex items-center gap-1.5 text-xs sm:text-sm font-semibold text-primary hover:underline"
            >
              <span>View all</span>
              <ArrowRight size={14} />
            </Link>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
            {subjects.length > 0 ? (
              subjects.slice(0, 3).map((sub) => (
                <Link
                  key={sub.subjectId}
                  to={`/subjects/${sub.subjectId}`}
                  className="group relative flex flex-col justify-between rounded-2xl border border-border bg-card p-6 shadow-xs hover:border-primary/50 hover:shadow-md transition"
                >
                  <div className="space-y-3">
                    <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10 text-primary group-hover:bg-primary group-hover:text-primary-foreground transition">
                      <GraduationCap size={20} />
                    </div>
                    <h3 className="font-bold text-foreground text-base group-hover:text-primary transition line-clamp-1">
                      {sub.name}
                    </h3>
                    <p className="text-xs text-muted-foreground line-clamp-2">
                      {sub.description || 'Comprehensive A/L ICT subject module.'}
                    </p>
                  </div>
                  <div className="mt-5 flex items-center justify-between border-t border-border/60 pt-3 text-xs text-muted-foreground">
                    <span>Syllabus Unit</span>
                    <span className="font-medium text-primary group-hover:translate-x-0.5 transition inline-flex items-center gap-1">
                      Explore <ArrowRight size={12} />
                    </span>
                  </div>
                </Link>
              ))
            ) : (
              <div className="col-span-full rounded-xl border border-dashed border-border p-8 text-center text-xs text-muted-foreground">
                {loading ? 'Loading subjects...' : 'Subjects will appear here soon.'}
              </div>
            )}
          </div>
        </section>

        {/* Featured Study Materials & Videos */}
        <section className="space-y-6 pt-6">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl sm:text-2xl font-bold tracking-tight text-foreground">Notes &amp; Video Lessons</h2>
              <p className="text-xs sm:text-sm text-muted-foreground mt-1">High-yield revision materials and video recordings</p>
            </div>
            <Link
              to="/products"
              className="inline-flex items-center gap-1.5 text-xs sm:text-sm font-semibold text-primary hover:underline"
            >
              <span>Browse Catalog</span>
              <ArrowRight size={14} />
            </Link>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
            {products.length > 0 ? (
              products.map((prod) => (
                <Link
                  key={prod.id}
                  to={`/products/${prod.slug}`}
                  className="group flex flex-col justify-between rounded-2xl border border-border bg-card p-5 shadow-xs hover:border-primary/50 hover:shadow-md transition"
                >
                  <div className="space-y-3">
                    <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-muted text-foreground">
                      {prod.type === 'VIDEO' ? <PlayCircle size={18} /> : <FileText size={18} />}
                    </div>
                    <div>
                      <span className="text-[10px] font-semibold uppercase tracking-wider text-primary">
                        {prod.type}
                      </span>
                      <h3 className="font-bold text-foreground text-sm group-hover:text-primary transition line-clamp-2 mt-0.5">
                        {prod.name}
                      </h3>
                    </div>
                  </div>
                  <div className="mt-4 pt-3 border-t border-border flex items-center justify-between text-xs">
                    <span className="font-bold text-foreground">
                      {prod.price === 0 ? 'Free' : `LKR ${prod.price.toLocaleString()}`}
                    </span>
                    <span className="text-primary font-medium group-hover:translate-x-0.5 transition inline-flex items-center gap-1">
                      View <ArrowRight size={11} />
                    </span>
                  </div>
                </Link>
              ))
            ) : (
              <div className="col-span-full rounded-xl border border-dashed border-border p-8 text-center text-xs text-muted-foreground">
                {loading ? 'Loading materials...' : 'Browse our catalog for study packs.'}
              </div>
            )}
          </div>
        </section>

        {/* Latest Announcements */}
        {announcements.length > 0 && (
          <section className="space-y-6 pt-6">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-xl sm:text-2xl font-bold tracking-tight text-foreground">Latest Announcements</h2>
                <p className="text-xs sm:text-sm text-muted-foreground mt-1">Updates on classes, exam tips, and schedules</p>
              </div>
              <Link
                to="/announcements"
                className="inline-flex items-center gap-1.5 text-xs sm:text-sm font-semibold text-primary hover:underline"
              >
                <span>All Updates</span>
                <ArrowRight size={14} />
              </Link>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
              {announcements.map((ann) => (
                <Link
                  key={ann.id}
                  to={`/announcements/${ann.id}`}
                  className="rounded-2xl border border-border bg-card p-5 shadow-xs hover:border-primary/50 transition flex flex-col justify-between"
                >
                  <div className="space-y-2">
                    <div className="flex items-center gap-1.5 text-[11px] font-medium text-muted-foreground">
                      <Megaphone size={12} className="text-primary" />
                      <span>Announcement</span>
                    </div>
                    <h3 className="font-bold text-foreground text-sm line-clamp-2 hover:text-primary transition">
                      {ann.title}
                    </h3>
                    <p className="text-xs text-muted-foreground line-clamp-3">
                      {ann.excerpt || ann.title}
                    </p>
                  </div>
                  <div className="mt-4 pt-2 text-[11px] text-primary font-semibold flex items-center gap-1">
                    Read announcement <ArrowRight size={11} />
                  </div>
                </Link>
              ))}
            </div>
          </section>
        )}
      </PageContainer>
    </div>
  );
}
