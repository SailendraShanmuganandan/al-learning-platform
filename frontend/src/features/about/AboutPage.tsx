import { Link } from 'react-router-dom';
import { PageContainer } from '../../components/layout/PageContainer';
import { PageHeader } from '../../components/layout/PageHeader';
import {
  BookOpen,
  CheckCircle2,
  Brain,
  Video,
  ArrowRight,
  Sparkles,
} from 'lucide-react';

export default function AboutPage() {
  return (
    <PageContainer>
      <div className="space-y-12">
        <PageHeader
          title="About A/L ICT Learning Platform"
          description="A purpose-built digital learning and practice ecosystem engineered specifically for Sri Lankan G.C.E. Advanced Level ICT students."
        />

        {/* Mission Statement */}
        <div className="rounded-2xl border border-primary/20 bg-gradient-to-br from-primary/10 via-primary/5 to-card p-8">
          <div className="max-w-3xl space-y-4">
            <div className="inline-flex items-center gap-2 rounded-full bg-primary/10 px-3 py-1 text-xs font-semibold text-primary">
              <Sparkles size={14} />
              <span>Our Vision</span>
            </div>
            <h2 className="text-2xl font-bold tracking-tight text-foreground">
              Democratizing high-quality A/L ICT education across Sri Lanka
            </h2>
            <p className="text-sm text-muted-foreground leading-relaxed">
              The A/L ICT examination demands both conceptual mastery and rapid problem-solving skills across
              a broad syllabus—from computer architecture and networking to software engineering, Python programming,
              and database systems. Our platform provides structured learning, targeted practice, and continuous
              performance analytics to bridge every learning gap.
            </p>
          </div>
        </div>

        {/* Core Pillars */}
        <div className="space-y-6">
          <h2 className="text-xl font-bold tracking-tight text-foreground">What Makes Our Platform Different</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="rounded-xl border border-border bg-card p-6 space-y-3">
              <div className="h-10 w-10 rounded-lg bg-primary/10 text-primary flex items-center justify-center font-bold">
                <BookOpen size={20} />
              </div>
              <h3 className="font-bold text-base text-foreground">Syllabus-Aligned Structure</h3>
              <p className="text-xs text-muted-foreground leading-relaxed">
                Organized precisely by subject units and chapter breakdown aligned with the official NIE syllabus,
                ensuring complete coverage without wasted time.
              </p>
            </div>

            <div className="rounded-xl border border-border bg-card p-6 space-y-3">
              <div className="h-10 w-10 rounded-lg bg-primary/10 text-primary flex items-center justify-center font-bold">
                <Brain size={20} />
              </div>
              <h3 className="font-bold text-base text-foreground">Adaptive Practice & Mistake Bank</h3>
              <p className="text-xs text-muted-foreground leading-relaxed">
                Every quiz attempt feeds your personal mistake bank. Practice wrong answers repeatedly until you
                master difficult concepts.
              </p>
            </div>

            <div className="rounded-xl border border-border bg-card p-6 space-y-3">
              <div className="h-10 w-10 rounded-lg bg-primary/10 text-primary flex items-center justify-center font-bold">
                <Video size={20} />
              </div>
              <h3 className="font-bold text-base text-foreground">Integrated Notes & Classes</h3>
              <p className="text-xs text-muted-foreground leading-relaxed">
                Access curated revision guides, video lecture recordings, and register for physical and online
                classes directly in one seamless portal.
              </p>
            </div>
          </div>
        </div>

        {/* Feature Highlights List */}
        <div className="rounded-xl border border-border bg-card p-8 space-y-6">
          <h2 className="text-lg font-bold text-foreground">Platform Capabilities</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
            <div className="flex items-start gap-2.5">
              <CheckCircle2 size={16} className="text-primary shrink-0 mt-0.5" />
              <div>
                <strong className="text-foreground">Diagnostic Assessments:</strong> Benchmark your baseline knowledge across all A/L ICT units before exam revision.
              </div>
            </div>
            <div className="flex items-start gap-2.5">
              <CheckCircle2 size={16} className="text-primary shrink-0 mt-0.5" />
              <div>
                <strong className="text-foreground">Chapter-Level Quizzes:</strong> Instant automated scoring with detailed answer explanations.
              </div>
            </div>
            <div className="flex items-start gap-2.5">
              <CheckCircle2 size={16} className="text-primary shrink-0 mt-0.5" />
              <div>
                <strong className="text-foreground">Timed Mock Exams:</strong> Real exam simulations testing time management and syllabus breadth.
              </div>
            </div>
            <div className="flex items-start gap-2.5">
              <CheckCircle2 size={16} className="text-primary shrink-0 mt-0.5" />
              <div>
                <strong className="text-foreground">Secure Learning Portal:</strong> Protected video streaming and instant order checkout with slip upload or card.
              </div>
            </div>
          </div>
        </div>

        {/* CTA */}
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4 rounded-xl border border-border bg-muted/40 p-6">
          <div>
            <h3 className="font-bold text-sm text-foreground">Ready to accelerate your A/L ICT preparation?</h3>
            <p className="text-xs text-muted-foreground mt-0.5">Explore our curriculum or reach out to our teaching team.</p>
          </div>
          <div className="flex items-center gap-3">
            <Link
              to="/subjects"
              className="inline-flex items-center gap-1.5 rounded-lg bg-primary px-4 py-2 text-xs font-semibold text-primary-foreground hover:bg-primary/90 transition"
            >
              <span>Explore Subjects</span>
              <ArrowRight size={14} />
            </Link>
            <Link
              to="/contact"
              className="inline-flex items-center gap-1.5 rounded-lg border border-border bg-card px-4 py-2 text-xs font-semibold text-foreground hover:bg-muted transition"
            >
              <span>Contact Us</span>
            </Link>
          </div>
        </div>
      </div>
    </PageContainer>
  );
}
