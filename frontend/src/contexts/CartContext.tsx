import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
} from 'react';
import { useAuth } from './AuthContext';
import {
  getCart,
  addToCart as apiAddToCart,
  updateCartItem as apiUpdateCartItem,
  removeCartItem as apiRemoveCartItem,
  clearCart as apiClearCart,
  type CartResponse,
} from '../api/cartApi';

interface CartContextValue {
  cart: CartResponse | null;
  isLoading: boolean;
  itemCount: number;
  refreshCart: () => Promise<void>;
  addItem: (productId: number, quantity: number) => Promise<void>;
  updateQuantity: (itemId: number, quantity: number) => Promise<void>;
  removeItem: (itemId: number) => Promise<void>;
  clear: () => Promise<void>;
}

const CartContext = createContext<CartContextValue | null>(null);

export function CartProvider({ children }: { children: React.ReactNode }) {
  const { user, isAuthenticated } = useAuth();
  const [cart, setCart] = useState<CartResponse | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);

  const isStudent = isAuthenticated && user?.role === 'STUDENT';

  const refreshCart = useCallback(async () => {
    if (!isStudent) {
      setCart(null);
      return;
    }
    setIsLoading(true);
    try {
      const data = await getCart();
      setCart(data);
    } catch (err) {
      // Cart might fail if unauthorized or error
      console.error('Failed to load cart:', err);
    } finally {
      setIsLoading(false);
    }
  }, [isStudent]);

  useEffect(() => {
    if (isStudent) {
      refreshCart();
    } else {
      setCart(null);
    }
  }, [isStudent, refreshCart]);

  const addItem = useCallback(
    async (productId: number, quantity: number) => {
      const updated = await apiAddToCart({ productId, quantity });
      setCart(updated);
    },
    []
  );

  const updateQuantity = useCallback(
    async (itemId: number, quantity: number) => {
      const updated = await apiUpdateCartItem(itemId, { quantity });
      setCart(updated);
    },
    []
  );

  const removeItem = useCallback(
    async (itemId: number) => {
      await apiRemoveCartItem(itemId);
      await refreshCart();
    },
    [refreshCart]
  );

  const clear = useCallback(async () => {
    await apiClearCart();
    setCart({ cartId: cart?.cartId ?? null, items: [], itemCount: 0, subtotal: 0 });
  }, [cart?.cartId]);

  const itemCount = cart?.itemCount ?? 0;

  return (
    <CartContext.Provider
      value={{
        cart,
        isLoading,
        itemCount,
        refreshCart,
        addItem,
        updateQuantity,
        removeItem,
        clear,
      }}
    >
      {children}
    </CartContext.Provider>
  );
}

export function useCart() {
  const context = useContext(CartContext);
  if (!context) {
    throw new Error('useCart must be used within a CartProvider');
  }
  return context;
}
