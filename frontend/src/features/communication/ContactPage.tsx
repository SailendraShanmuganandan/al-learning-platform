import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAuth } from '../../contexts/AuthContext';
import { submitContact, type ContactMessageRequest } from '../../api/communicationApi';
import {
  Mail,
  Send,
  Loader2,
  CheckCircle2,
  AlertCircle,
  Phone,
  User as UserIcon,
} from 'lucide-react';
import { PageContainer } from '../../components/layout/PageContainer';
import { PageHeader } from '../../components/layout/PageHeader';

const phoneRegex = /^\+[1-9]\d{6,14}$|^$/;

const contactSchema = z.object({
  name: z
    .string()
    .trim()
    .min(2, 'Name must be at least 2 characters')
    .max(100, 'Name must not exceed 100 characters'),
  email: z
    .string()
    .trim()
    .email('Please enter a valid email address')
    .max(254, 'Email must not exceed 254 characters'),
  phone: z
    .string()
    .trim()
    .refine((val) => val === '' || phoneRegex.test(val), {
      message: 'Phone must be in E.164 format (e.g. +911234567890) or left empty',
    })
    .optional(),
  message: z
    .string()
    .trim()
    .min(10, 'Message must be at least 10 characters')
    .max(2000, 'Message must not exceed 2000 characters'),
  website: z.string().optional(),
});

type ContactFormData = z.infer<typeof contactSchema>;

export default function ContactPage() {
  const { user } = useAuth();
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    watch,
    formState: { errors },
  } = useForm<ContactFormData>({
    resolver: zodResolver(contactSchema),
    defaultValues: {
      name: user?.fullName || '',
      email: user?.email || '',
      phone: user?.phone || '',
      message: '',
      website: '',
    },
  });

  // If user loads after mount, prefill name and email
  useEffect(() => {
    if (user) {
      reset({
        name: user.fullName || '',
        email: user.email || '',
        phone: user.phone || '',
        message: '',
        website: '',
      });
    }
  }, [user, reset]);

  const messageText = watch('message') || '';

  async function onSubmit(data: ContactFormData) {
    setSubmitting(true);
    setErrorMessage(null);
    try {
      const payload: ContactMessageRequest = {
        name: data.name,
        email: data.email,
        phone: data.phone && data.phone.trim() !== '' ? data.phone.trim() : undefined,
        message: data.message,
        website: data.website,
      };
      await submitContact(payload);
      setSuccess(true);
      reset({
        name: user?.fullName || '',
        email: user?.email || '',
        phone: user?.phone || '',
        message: '',
        website: '',
      });
    } catch (err: unknown) {
      const anyErr = err as { status?: number; message?: string };
      if (anyErr?.status === 429) {
        setErrorMessage(
          'You have submitted too many messages recently. To prevent spam, please wait a while before trying again.'
        );
      } else {
        setErrorMessage(
          anyErr?.message || 'Failed to send your message. Please verify the information and try again.'
        );
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <PageContainer>
      <PageHeader title="Contact Us" />

      <div className="grid grid-cols-1 md:grid-cols-5 gap-10 items-start">
        {/* Left Column: Context & Support info */}
        <div className="md:col-span-2 space-y-6">
          <div className="inline-flex items-center gap-2 rounded-full border border-primary/20 bg-primary/10 px-3 py-1 text-xs font-semibold text-primary">
            <Mail size={13} />
            <span>Get In Touch</span>
          </div>

          <h1 className="text-3xl font-extrabold tracking-tight text-foreground">
            Contact Student Support
          </h1>

          <p className="text-xs sm:text-sm text-muted-foreground leading-relaxed">
            Have a question about class schedules, video lecture access, study materials, or tuition cancellations?
            Send us a message and our academic coordinators will assist you promptly.
          </p>

          <div className="space-y-4 pt-4 border-t border-border text-xs text-muted-foreground">
            <div className="flex items-center gap-3">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary/10 text-primary">
                <Mail size={14} />
              </div>
              <div>
                <p className="font-semibold text-foreground">Direct Email</p>
                <p>support@allearning.lk</p>
              </div>
            </div>

            <div className="flex items-center gap-3">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary/10 text-primary">
                <Phone size={14} />
              </div>
              <div>
                <p className="font-semibold text-foreground">Hotline</p>
                <p>+94 11 234 5678 (Mon–Sat, 8am–6pm)</p>
              </div>
            </div>
          </div>
        </div>

        {/* Right Column: Interactive Form */}
        <div className="md:col-span-3">
          <div className="bg-card rounded-3xl border border-border p-6 sm:p-8 shadow-sm">
            {success ? (
              <div className="text-center py-8 space-y-4 animate-in fade-in zoom-in-95 duration-200">
                <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-emerald-500/10 text-emerald-600 mx-auto">
                  <CheckCircle2 size={32} />
                </div>
                <h2 className="text-lg font-bold text-foreground">Message Sent Successfully!</h2>
                <p className="text-xs text-muted-foreground max-w-sm mx-auto leading-relaxed">
                  Thank you for reaching out. We have received your inquiry and our team will get back to you shortly.
                </p>
                <button
                  type="button"
                  onClick={() => setSuccess(false)}
                  className="mt-2 inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-primary text-primary-foreground text-xs font-semibold hover:opacity-90 transition"
                >
                  Send another message
                </button>
              </div>
            ) : (
              <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
                {errorMessage && (
                  <div
                    id="contact-error-banner"
                    className="p-3.5 rounded-2xl bg-destructive/10 border border-destructive/20 text-destructive text-xs flex items-start gap-2.5"
                  >
                    <AlertCircle size={16} className="shrink-0 mt-0.5" />
                    <span>{errorMessage}</span>
                  </div>
                )}

                {/* Honeypot field: invisible to human users, accessible to automated bots */}
                <div className="hidden" aria-hidden="true" style={{ display: 'none' }}>
                  <label htmlFor="website">Website</label>
                  <input
                    id="website"
                    type="text"
                    tabIndex={-1}
                    autoComplete="off"
                    {...register('website')}
                  />
                </div>

                {/* Name field */}
                <div>
                  <label htmlFor="contact-name" className="block text-xs font-semibold text-foreground mb-1.5">
                    Your Name <span className="text-destructive">*</span>
                  </label>
                  <div className="relative">
                    <UserIcon size={14} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-muted-foreground" />
                    <input
                      id="contact-name"
                      type="text"
                      placeholder="e.g. Sahan Perera"
                      {...register('name')}
                      className={`w-full rounded-xl border bg-background pl-9 pr-3.5 py-2 text-xs text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/40 ${
                        errors.name ? 'border-destructive' : 'border-border'
                      }`}
                    />
                  </div>
                  {errors.name && (
                    <p className="mt-1 text-[11px] text-destructive">{errors.name.message}</p>
                  )}
                </div>

                {/* Email field */}
                <div>
                  <label htmlFor="contact-email" className="block text-xs font-semibold text-foreground mb-1.5">
                    Email Address <span className="text-destructive">*</span>
                  </label>
                  <div className="relative">
                    <Mail size={14} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-muted-foreground" />
                    <input
                      id="contact-email"
                      type="email"
                      placeholder="name@example.com"
                      {...register('email')}
                      className={`w-full rounded-xl border bg-background pl-9 pr-3.5 py-2 text-xs text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/40 ${
                        errors.email ? 'border-destructive' : 'border-border'
                      }`}
                    />
                  </div>
                  {errors.email && (
                    <p className="mt-1 text-[11px] text-destructive">{errors.email.message}</p>
                  )}
                </div>

                {/* Phone field (optional) */}
                <div>
                  <label htmlFor="contact-phone" className="block text-xs font-semibold text-foreground mb-1.5">
                    Phone Number <span className="text-muted-foreground font-normal">(Optional, E.164 format)</span>
                  </label>
                  <div className="relative">
                    <Phone size={14} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-muted-foreground" />
                    <input
                      id="contact-phone"
                      type="tel"
                      placeholder="+94771234567"
                      {...register('phone')}
                      className={`w-full rounded-xl border bg-background pl-9 pr-3.5 py-2 text-xs text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/40 ${
                        errors.phone ? 'border-destructive' : 'border-border'
                      }`}
                    />
                  </div>
                  {errors.phone && (
                    <p className="mt-1 text-[11px] text-destructive">{errors.phone.message}</p>
                  )}
                </div>

                {/* Message field */}
                <div>
                  <div className="flex items-center justify-between mb-1.5">
                    <label htmlFor="contact-message" className="text-xs font-semibold text-foreground">
                      Your Message <span className="text-destructive">*</span>
                    </label>
                    <span className="text-[10px] text-muted-foreground">
                      {messageText.length} / 2000
                    </span>
                  </div>
                  <div className="relative">
                    <textarea
                      id="contact-message"
                      rows={4}
                      placeholder="Please write your inquiry, class cancellation request, or feedback here…"
                      {...register('message')}
                      className={`w-full rounded-xl border bg-background p-3 text-xs text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/40 ${
                        errors.message ? 'border-destructive' : 'border-border'
                      }`}
                    />
                  </div>
                  {errors.message && (
                    <p className="mt-1 text-[11px] text-destructive">{errors.message.message}</p>
                  )}
                </div>

                {/* Submit Button */}
                <button
                  type="submit"
                  id="btn-submit-contact"
                  disabled={submitting}
                  className="w-full mt-2 py-2.5 px-4 rounded-xl bg-primary text-primary-foreground text-xs font-semibold hover:opacity-90 disabled:opacity-50 transition flex items-center justify-center gap-1.5 shadow-sm"
                >
                  {submitting ? (
                    <>
                      <Loader2 size={14} className="animate-spin" />
                      <span>Sending message…</span>
                    </>
                  ) : (
                    <>
                      <Send size={14} />
                      <span>Send Message</span>
                    </>
                  )}
                </button>
              </form>
            )}
          </div>
        </div>
      </div>
    </PageContainer>
  );
}
