import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { AdminRoute } from './components/AdminRoute';
import LoginPage from './features/auth/LoginPage';
import RegisterPage from './features/auth/RegisterPage';
import VerifyEmailPage from './features/auth/VerifyEmailPage';
import ForgotPasswordPage from './features/auth/ForgotPasswordPage';
import ResetPasswordPage from './features/auth/ResetPasswordPage';
import DashboardPage from './features/dashboard/DashboardPage';
import ProfilePage from './features/profile/ProfilePage';
import AddressesPage from './features/addresses/AddressesPage';
import SubjectsPage from './features/academic/SubjectsPage';
import SubjectDetailPage from './features/academic/SubjectDetailPage';
import ChapterDetailPage from './features/academic/ChapterDetailPage';
import ProductsPage from './features/catalog/ProductsPage';
import ProductDetailPage from './features/catalog/ProductDetailPage';
import AdminLayout from './features/admin/AdminLayout';
import AdminSubjectsPage from './features/admin/subjects/AdminSubjectsPage';
import AdminChaptersPage from './features/admin/chapters/AdminChaptersPage';
import AdminProductsPage from './features/admin/products/AdminProductsPage';

import { CartProvider } from './contexts/CartContext';
import CartPage from './features/cart/CartPage';
import CheckoutPage from './features/orders/CheckoutPage';
import OrderConfirmationPage from './features/orders/OrderConfirmationPage';
import OrdersPage from './features/orders/OrdersPage';
import OrderDetailPage from './features/orders/OrderDetailPage';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <CartProvider>
          <Routes>
            {/* Public routes */}
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/verify-email" element={<VerifyEmailPage />} />
            <Route path="/forgot-password" element={<ForgotPasswordPage />} />
            <Route path="/reset-password" element={<ResetPasswordPage />} />
            <Route path="/subjects" element={<SubjectsPage />} />
            <Route path="/subjects/:id" element={<SubjectDetailPage />} />
            <Route path="/chapters/:id" element={<ChapterDetailPage />} />
            {/* Public catalog */}
            <Route path="/products" element={<ProductsPage />} />
            <Route path="/products/:slug" element={<ProductDetailPage />} />

            {/* Protected routes (any authenticated user) */}
            <Route element={<ProtectedRoute />}>
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/profile" element={<ProfilePage />} />
              <Route path="/addresses" element={<AddressesPage />} />
              <Route path="/cart" element={<CartPage />} />
              <Route path="/checkout" element={<CheckoutPage />} />
              <Route path="/orders" element={<OrdersPage />} />
              <Route path="/orders/:orderId" element={<OrderDetailPage />} />
              <Route path="/orders/:orderId/confirmation" element={<OrderConfirmationPage />} />
            </Route>

          {/* Admin routes (ADMIN role only) */}
          <Route element={<AdminRoute />}>
            <Route path="/admin" element={<AdminLayout />}>
              <Route index element={<Navigate to="subjects" replace />} />
              <Route path="subjects" element={<AdminSubjectsPage />} />
              <Route path="subjects/:subjectId/chapters" element={<AdminChaptersPage />} />
              <Route path="products" element={<AdminProductsPage />} />
            </Route>
          </Route>

          {/* Default redirect */}
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
        </CartProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}