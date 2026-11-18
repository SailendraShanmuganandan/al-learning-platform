import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import { CartProvider } from './contexts/CartContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { AdminRoute } from './components/AdminRoute';

// Layout shells
import AuthLayout from './components/layout/AuthLayout';
import SiteLayout from './components/layout/SiteLayout';
import StudentLayout from './components/layout/StudentLayout';
import AdminLayout from './features/admin/AdminLayout';
import { ComingSoonPage } from './components/layout/ComingSoonPage';
import NotFoundPage from './components/layout/NotFoundPage';

// Auth pages
import LoginPage from './features/auth/LoginPage';
import RegisterPage from './features/auth/RegisterPage';
import VerifyEmailPage from './features/auth/VerifyEmailPage';
import ForgotPasswordPage from './features/auth/ForgotPasswordPage';
import ResetPasswordPage from './features/auth/ResetPasswordPage';

// Public pages
import HomePage from './features/home/HomePage';
import AboutPage from './features/about/AboutPage';
import SubjectsPage from './features/academic/SubjectsPage';
import SubjectDetailPage from './features/academic/SubjectDetailPage';
import ChapterDetailPage from './features/academic/ChapterDetailPage';
import ProductsPage from './features/catalog/ProductsPage';
import ProductDetailPage from './features/catalog/ProductDetailPage';
import ClassesPage from './features/classes/ClassesPage';
import ClassDetailPage from './features/classes/ClassDetailPage';
import AnnouncementsPage from './features/communication/AnnouncementsPage';
import AnnouncementDetailPage from './features/communication/AnnouncementDetailPage';
import ContactPage from './features/communication/ContactPage';

// Student / protected pages
import DashboardPage from './features/dashboard/DashboardPage';
import ProfilePage from './features/profile/ProfilePage';
import AddressesPage from './features/addresses/AddressesPage';
import CartPage from './features/cart/CartPage';
import CheckoutPage from './features/orders/CheckoutPage';
import OrdersPage from './features/orders/OrdersPage';
import OrderDetailPage from './features/orders/OrderDetailPage';
import OrderConfirmationPage from './features/orders/OrderConfirmationPage';
import MyClassesPage from './features/classes/MyClassesPage';
import MyVideosPage from './features/videos/MyVideosPage';
import MyVideoPlayerPage from './features/videos/MyVideoPlayerPage';

// Learning — student
import StudentLearningOverviewPage from './features/learning-student/pages/StudentLearningOverviewPage';
import StudentProgressPage from './features/learning-student/pages/StudentProgressPage';
import StudentMistakesPage from './features/learning-student/pages/StudentMistakesPage';
import StudentStudySessionStartPage from './features/learning-student/pages/StudentStudySessionStartPage';
import StudentStudySessionDetailPage from './features/learning-student/pages/StudentStudySessionDetailPage';
import StudentOverallQuizPage from './features/learning-student/pages/StudentOverallQuizPage';
import StudentDiagnosticPage from './features/learning-student/pages/StudentDiagnosticPage';
import StudentAssessmentPage from './features/learning-student/pages/StudentAssessmentPage';
import StudentAssessmentsHistoryPage from './features/learning-student/pages/StudentAssessmentsHistoryPage';
import StudentChapterQuizStartPage from './features/learning-student/pages/StudentChapterQuizStartPage';
import StudentMyAttemptsPage from './features/learning-student/pages/StudentMyAttemptsPage';
import StudentQuizAttemptPage from './features/learning-student/pages/StudentQuizAttemptPage';

// Admin pages
import AdminDashboardPage from './features/admin/dashboard/AdminDashboardPage';
import AdminSubjectsPage from './features/admin/subjects/AdminSubjectsPage';
import AdminChaptersPage from './features/admin/chapters/AdminChaptersPage';
import AdminProductsPage from './features/admin/products/AdminProductsPage';
import AdminOrdersPage from './features/admin/orders/AdminOrdersPage';
import AdminOrderDetailPage from './features/admin/orders/AdminOrderDetailPage';
import AdminPaymentsPage from './features/admin/payments/AdminPaymentsPage';
import AdminClassesPage from './features/admin/classes/AdminClassesPage';
import AdminClassRegistrationsPage from './features/admin/classes/AdminClassRegistrationsPage';
import AdminAnnouncementsPage from './features/admin/communication/AdminAnnouncementsPage';
import AdminContactMessagesPage from './features/admin/communication/AdminContactMessagesPage';
import AdminQuizzesPage from './features/learning-admin/pages/AdminQuizzesPage';
import AdminQuestionBankPage from './features/learning-admin/pages/AdminQuestionBankPage';
import AdminQuestionFormPage from './features/learning-admin/pages/AdminQuestionFormPage';
import AdminQuestionReportsPage from './features/learning-admin/pages/AdminQuestionReportsPage';
import AdminLearningOverviewPage from './features/learning-admin/pages/AdminLearningOverviewPage';
import AdminChapterAnalyticsPage from './features/learning-admin/pages/AdminChapterAnalyticsPage';
import AdminStudentLearningPage from './features/learning-admin/pages/AdminStudentLearningPage';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <CartProvider>
          <Routes>
            {/* Auth routes — no navbar, just logo + card */}
            <Route element={<AuthLayout />}>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
              <Route path="/verify-email" element={<VerifyEmailPage />} />
              <Route path="/forgot-password" element={<ForgotPasswordPage />} />
              <Route path="/reset-password" element={<ResetPasswordPage />} />
            </Route>

            {/* Public / shared routes — SiteLayout picks the right shell */}
            <Route element={<SiteLayout />}>
              <Route path="/" element={<HomePage />} />
              <Route path="/subjects" element={<SubjectsPage />} />
              <Route path="/subjects/:id" element={<SubjectDetailPage />} />
              <Route path="/chapters/:id" element={<ChapterDetailPage />} />
              <Route path="/products" element={<ProductsPage />} />
              <Route path="/products/:slug" element={<ProductDetailPage />} />
              <Route path="/classes" element={<ClassesPage />} />
              <Route path="/classes/:id" element={<ClassDetailPage />} />
              <Route path="/announcements" element={<AnnouncementsPage />} />
              <Route path="/announcements/:id" element={<AnnouncementDetailPage />} />
              <Route path="/contact" element={<ContactPage />} />
              <Route path="/about" element={<AboutPage />} />
            </Route>

            {/* Protected student routes */}
            <Route element={<ProtectedRoute />}>
              <Route element={<StudentLayout />}>
                {/* Dashboard */}
                <Route path="/dashboard" element={<DashboardPage />} />

                {/* Profile & Account */}
                <Route path="/profile" element={<ProfilePage />} />
                <Route path="/addresses" element={<AddressesPage />} />
                <Route path="/account-settings" element={<ComingSoonPage />} />

                {/* Shopping */}
                <Route path="/cart" element={<CartPage />} />
                <Route path="/checkout" element={<CheckoutPage />} />
                <Route path="/orders" element={<OrdersPage />} />
                <Route path="/orders/:orderId" element={<OrderDetailPage />} />
                <Route path="/orders/:orderId/confirmation" element={<OrderConfirmationPage />} />

                {/* Classes */}
                <Route path="/my/classes" element={<MyClassesPage />} />

                {/* Videos */}
                <Route path="/my-videos" element={<MyVideosPage />} />
                <Route path="/my-videos/:productId" element={<MyVideoPlayerPage />} />

                {/* Learning module — new sidebar URLs */}
                <Route path="/learning-overview" element={<StudentLearningOverviewPage />} />
                <Route path="/syllabus-progress" element={<StudentProgressPage />} />
                <Route path="/my-mistakes" element={<StudentMistakesPage />} />
                <Route path="/study-session" element={<StudentStudySessionStartPage />} />
                <Route path="/learning/study/:sessionId" element={<StudentStudySessionDetailPage />} />
                <Route path="/learning/assessments" element={<StudentAssessmentsHistoryPage />} />
                <Route path="/overall-quiz" element={<StudentOverallQuizPage />} />
                <Route path="/diagnostic" element={<StudentDiagnosticPage />} />
                <Route path="/assessments/:assessmentId" element={<StudentAssessmentPage />} />
                <Route path="/attempts/:attemptId" element={<StudentQuizAttemptPage />} />
                <Route path="/my-quizzes" element={<StudentMyAttemptsPage />} />
                <Route path="/quizzes/:quizId/my-attempts" element={<StudentMyAttemptsPage />} />

                {/* Legacy learning URLs - keep working */}
                <Route path="/learning" element={<StudentLearningOverviewPage />} />
                <Route path="/learning/progress" element={<StudentProgressPage />} />
                <Route path="/learning/mistakes" element={<StudentMistakesPage />} />
                <Route path="/learning/study" element={<StudentStudySessionStartPage />} />
                <Route path="/learning/assessments/overall" element={<StudentOverallQuizPage />} />
                <Route path="/learning/assessments/diagnostic" element={<StudentDiagnosticPage />} />

                {/* ComingSoon placeholders */}
                <Route path="/chat-history" element={<ComingSoonPage />} />
              </Route>
            </Route>

            {/* Quiz start - accessible publicly but rendered in SiteLayout */}
            <Route element={<SiteLayout />}>
              <Route path="/quizzes/:quizId" element={<StudentChapterQuizStartPage />} />
            </Route>

            {/* Admin routes */}
            <Route element={<AdminRoute />}>
              <Route element={<AdminLayout />}>
                <Route path="/admin" index element={<AdminDashboardPage />} />
                <Route path="/admin/subjects" element={<AdminSubjectsPage />} />
                <Route path="/admin/subjects/:subjectId/chapters" element={<AdminChaptersPage />} />
                <Route path="/admin/products" element={<AdminProductsPage />} />
                <Route path="/admin/orders" element={<AdminOrdersPage />} />
                <Route path="/admin/orders/:id" element={<AdminOrderDetailPage />} />
                <Route path="/admin/payments" element={<AdminPaymentsPage />} />
                <Route path="/admin/classes" element={<AdminClassesPage />} />
                <Route path="/admin/class-registrations" element={<AdminClassRegistrationsPage />} />
                <Route path="/admin/announcements" element={<AdminAnnouncementsPage />} />
                <Route path="/admin/contact-messages" element={<AdminContactMessagesPage />} />
                <Route path="/admin/quizzes" element={<AdminQuizzesPage />} />
                <Route path="/admin/question-bank" element={<AdminQuestionBankPage />} />
                <Route path="/admin/question-bank/new" element={<AdminQuestionFormPage />} />
                <Route path="/admin/question-bank/:id" element={<AdminQuestionFormPage />} />
                <Route path="/admin/question-reports" element={<AdminQuestionReportsPage />} />
                <Route path="/admin/analytics" element={<AdminLearningOverviewPage />} />
                <Route path="/admin/learning-analytics" element={<AdminLearningOverviewPage />} />
                <Route path="/admin/learning-analytics/chapters/:chapterId" element={<AdminChapterAnalyticsPage />} />
                <Route path="/admin/learning-analytics/students/:userId" element={<AdminStudentLearningPage />} />
                {/* ComingSoon admin placeholders */}
                <Route path="/admin/users" element={<ComingSoonPage />} />
                <Route path="/admin/question-review" element={<ComingSoonPage />} />
                <Route path="/admin/chapter-topics" element={<ComingSoonPage />} />
                <Route path="/admin/chapter-documents" element={<ComingSoonPage />} />
                <Route path="/admin/ai-generation" element={<ComingSoonPage />} />
                <Route path="/admin/ai-usage" element={<ComingSoonPage />} />
                <Route path="/admin/chat-review" element={<ComingSoonPage />} />
                <Route path="/admin/settings" element={<ComingSoonPage />} />
              </Route>
            </Route>

            {/* 404 */}
            <Route path="*" element={<NotFoundPage />} />
          </Routes>
        </CartProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}
