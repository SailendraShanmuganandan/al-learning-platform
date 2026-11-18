import { Link } from 'react-router-dom';
import { useAuth } from '../../../contexts/AuthContext';
import { LEARNING_ROUTES } from '../routes';
import { Sparkles, ArrowRight, X, AlertCircle } from 'lucide-react';

interface OnboardingOfferModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSkip: () => void;
}

export function OnboardingOfferModal({
  isOpen,
  onClose,
  onSkip,
}: OnboardingOfferModalProps) {
  const { user } = useAuth();
  if (!isOpen) return null;

  const isVerified = user?.isEmailVerified ?? user?.is_email_verified ?? false;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 animate-in fade-in duration-150">
      <div className="bg-card rounded-3xl border border-border p-6 md:p-8 max-w-lg w-full shadow-2xl animate-in zoom-in-95 duration-150 space-y-6 relative">
        <button
          type="button"
          onClick={onClose}
          className="absolute top-5 right-5 p-1 rounded-lg text-muted-foreground hover:bg-muted transition"
          aria-label="Close dialog"
        >
          <X size={18} />
        </button>

        <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-primary/10 text-primary">
          <Sparkles size={24} />
        </div>

        <div className="space-y-2">
          <h2 className="text-xl font-black text-foreground tracking-tight">
            Personalise Your Learning Journey
          </h2>
          <p className="text-xs text-muted-foreground leading-relaxed">
            Take a quick, 10-question initial diagnostic assessment to map your current knowledge.
            Our recommendation engine will automatically highlight your weak areas and suggest tailored study plans.
          </p>
        </div>

        {!isVerified ? (
          <div className="p-3.5 rounded-2xl border border-amber-500/30 bg-amber-500/10 text-xs text-amber-800 dark:text-amber-200 flex items-start gap-2.5">
            <AlertCircle size={16} className="text-amber-600 shrink-0 mt-0.5" />
            <div>
              <span className="font-semibold text-foreground">Email verification required: </span>
              Please verify your email address to unlock official diagnostic assessments. You can verify from your dashboard.
            </div>
          </div>
        ) : null}

        <div className="pt-2 flex flex-col sm:flex-row items-center justify-between gap-3">
          <button
            type="button"
            onClick={onSkip}
            className="w-full sm:w-auto px-4 py-2.5 rounded-xl border border-border text-xs font-semibold text-muted-foreground hover:bg-muted transition text-center"
          >
            Skip for now
          </button>

          {isVerified ? (
            <Link
              to={LEARNING_ROUTES.DIAGNOSTIC_START}
              onClick={onClose}
              className="w-full sm:w-auto px-5 py-2.5 rounded-xl bg-primary text-primary-foreground text-xs font-bold hover:bg-primary/90 transition shadow-xs flex items-center justify-center gap-2"
            >
              <span>Start Diagnostic Assessment</span>
              <ArrowRight size={14} />
            </Link>
          ) : (
            <Link
              to="/dashboard"
              onClick={onClose}
              className="w-full sm:w-auto px-5 py-2.5 rounded-xl bg-amber-600 hover:bg-amber-700 text-white text-xs font-bold transition shadow-xs flex items-center justify-center gap-2"
            >
              <span>Go to Dashboard to Verify</span>
              <ArrowRight size={14} />
            </Link>
          )}
        </div>
      </div>
    </div>
  );
}

