import { describe, it, expect } from 'vitest';

describe('Partial Cart Checkout Logic', () => {
  it('should only remove ordered items from cart and retain unselected items', () => {
    let cart = [
      { bienTheId: 101, tenSanPham: 'Asus ROG Strix G16', quantity: 1, giaBan: 32490000 },
      { bienTheId: 102, tenSanPham: 'Asus Vivobook 15', quantity: 1, giaBan: 14990000 },
      { bienTheId: 103, tenSanPham: 'Lenovo Legion 5 Pro 16', quantity: 1, giaBan: 28490000 },
    ];

    let cartSelected = new Set([101]); // User only selected product 101 to checkout

    const handleOrderPlaced = (placedOrder) => {
      const orderedIds = new Set(
        placedOrder?.items?.map(i => i.bienTheId).filter(Boolean) || []
      );
      if (orderedIds.size === 0) {
        cartSelected.forEach(id => orderedIds.add(id));
      }

      cart = cart.filter(item => !orderedIds.has(item.bienTheId));
      cartSelected = new Set();
    };

    // User places order for item 101
    const placedOrder = {
      id: 999,
      maDonHang: 'DH999',
      items: [{ bienTheId: 101, tenSanPham: 'Asus ROG Strix G16', quantity: 1 }],
    };

    handleOrderPlaced(placedOrder);

    // Verify: item 101 was removed
    expect(cart.find(i => i.bienTheId === 101)).toBeUndefined();
    // Verify: item 102 and item 103 REMAIN in cart
    expect(cart).toHaveLength(2);
    expect(cart.map(i => i.bienTheId)).toEqual([102, 103]);
    // Verify: remaining items are NOT auto-selected (customer decides what to select)
    expect([...cartSelected]).toEqual([]);
  });

  it('should fall back to cartSelected if placedOrder does not have items list', () => {
    let cart = [
      { bienTheId: 201, tenSanPham: 'Product A', quantity: 2 },
      { bienTheId: 202, tenSanPham: 'Product B', quantity: 1 },
    ];

    let cartSelected = new Set([201]);

    const handleOrderPlaced = (placedOrder) => {
      const orderedIds = new Set(
        placedOrder?.items?.map(i => i.bienTheId).filter(Boolean) || []
      );
      if (orderedIds.size === 0) {
        cartSelected.forEach(id => orderedIds.add(id));
      }

      cart = cart.filter(item => !orderedIds.has(item.bienTheId));
      cartSelected = new Set();
    };

    handleOrderPlaced({});

    expect(cart).toHaveLength(1);
    expect(cart[0].bienTheId).toBe(202);
  });
});
