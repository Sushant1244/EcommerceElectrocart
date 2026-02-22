/**
 * Firebase Cloud Functions for Electrocart E-commerce App
 * 
 * These functions should be deployed to Firebase Cloud Functions.
 * 
 * Setup:
 * 1. Initialize Firebase Functions: firebase init functions
 * 2. Copy these functions to functions/index.js
 * 3. Deploy: firebase deploy --only functions
 */

const functions = require('firebase-functions');
const admin = require('firebase-admin');
const cors = require('cors')({ origin: true });

admin.initializeApp();

// ============================================
// ORDER MANAGEMENT FUNCTIONS
// ============================================

/**
 * Send order confirmation email when order is created
 */
exports.sendOrderConfirmation = functions.firestore
  .document('orders/{orderId}')
  .onCreate(async (snap, context) => {
    const order = snap.data();
    const orderId = context.params.orderId;

    try {
      // Get user details
      const userDoc = await admin.firestore().collection('users').doc(order.userId).get();
      const userData = userDoc.data();

      // Create notification
      await admin.firestore().collection('notifications').add({
        userId: order.userId,
        title: 'Order Confirmed',
        message: `Your order #${order.orderNumber} has been confirmed!`,
        type: 'order',
        isRead: false,
        createdAt: admin.firestore.FieldValue.serverTimestamp()
      });

      // Send push notification
      if (userData.fcmTokens && userData.fcmTokens.length > 0) {
        const payload = {
          notification: {
            title: 'Order Confirmed',
            body: `Your order #${order.orderNumber} has been confirmed!`
          },
          data: {
            type: 'order_status',
            orderId: orderId,
            status: 'confirmed'
          }
        };

        await admin.messaging().sendToDevice(userData.fcmTokens, payload);
      }

      console.log('Order confirmation sent successfully');
      return null;
    } catch (error) {
      console.error('Error sending order confirmation:', error);
      return null;
    }
  });

/**
 * Update inventory when order is placed
 */
exports.updateInventory = functions.firestore
  .document('orders/{orderId}')
  .onCreate(async (snap, context) => {
    const order = snap.data();

    try {
      // Update stock for each item
      const batch = admin.firestore().batch();

      for (const item of order.items) {
        const productRef = admin.firestore().collection('products').doc(item.productId);
        batch.update(productRef, {
          stock: admin.firestore.FieldValue.increment(-item.quantity)
        });
      }

      await batch.commit();
      console.log('Inventory updated successfully');
      return null;
    } catch (error) {
      console.error('Error updating inventory:', error);
      return null;
    }
  });

/**
 * Send order status update notification
 */
exports.onOrderStatusChange = functions.firestore
  .document('orders/{orderId}')
  .onUpdate(async (change, context) => {
    const newOrder = change.after.data();
    const previousOrder = change.before.data();
    const orderId = context.params.orderId;

    // Only send notification if status changed
    if (newOrder.status === previousOrder.status) {
      return null;
    }

    try {
      const statusMessages = {
        CONFIRMED: 'Your order has been confirmed!',
        PROCESSING: 'Your order is being processed.',
        SHIPPED: 'Your order has been shipped!',
        OUT_FOR_DELIVERY: 'Your order is out for delivery.',
        DELIVERED: 'Your order has been delivered!',
        CANCELLED: 'Your order has been cancelled.'
      };

      const message = statusMessages[newOrder.status] || 'Order status updated';

      // Create notification
      await admin.firestore().collection('notifications').add({
        userId: newOrder.userId,
        title: 'Order Update',
        message: message,
        type: 'order',
        isRead: false,
        createdAt: admin.firestore.FieldValue.serverTimestamp()
      });

      // Send push notification
      const userDoc = await admin.firestore().collection('users').doc(newOrder.userId).get();
      const userData = userDoc.data();

      if (userData.fcmTokens && userData.fcmTokens.length > 0) {
        const payload = {
          notification: {
            title: 'Order Update',
            body: message
          },
          data: {
            type: 'order_status',
            orderId: orderId,
            status: newOrder.status
          }
        };

        await admin.messaging().sendToDevice(userData.fcmTokens, payload);
      }

      return null;
    } catch (error) {
      console.error('Error sending order status notification:', error);
      return null;
    }
  });

/**
 * Cancel order and restore inventory
 */
exports.onOrderCancel = functions.firestore
  .document('orders/{orderId}')
  .onUpdate(async (change, context) => {
    const newOrder = change.after.data();
    const previousOrder = change.before.data();

    // Only process if status changed to CANCELLED
    if (newOrder.status !== 'CANCELLED' || previousOrder.status === 'CANCELLED') {
      return null;
    }

    try {
      // Restore inventory
      const batch = admin.firestore().batch();

      for (const item of newOrder.items) {
        const productRef = admin.firestore().collection('products').doc(item.productId);
        batch.update(productRef, {
          stock: admin.firestore.FieldValue.increment(item.quantity)
        });
      }

      await batch.commit();
      console.log('Inventory restored for cancelled order');
      return null;
    } catch (error) {
      console.error('Error restoring inventory:', error);
      return null;
    }
  });

// ============================================
// PRODUCT MANAGEMENT FUNCTIONS
// ============================================

/**
 * Update product count when category changes
 */
exports.updateCategoryProductCount = functions.firestore
  .document('products/{productId}')
  .onWrite(async (change, context) => {
    const product = change.after.exists ? change.after.data() : null;
    const previousProduct = change.before.exists ? change.before.data() : null;

    try {
      if (!product && previousProduct && previousProduct.categoryId) {
        // Product deleted - decrease count
        await admin.firestore()
          .collection('categories')
          .doc(previousProduct.categoryId)
          .update({
            productCount: admin.firestore.FieldValue.increment(-1)
          });
      } else if (product && !previousProduct) {
        // Product created - increase count
        await admin.firestore()
          .collection('categories')
          .doc(product.categoryId)
          .update({
            productCount: admin.firestore.FieldValue.increment(1)
          });
      } else if (product && previousProduct && product.categoryId !== previousProduct.categoryId) {
        // Category changed - decrease old, increase new
        const batch = admin.firestore().batch();
        
        batch.update(
          admin.firestore().collection('categories').doc(previousProduct.categoryId),
          { productCount: admin.firestore.FieldValue.increment(-1) }
        );
        
        batch.update(
          admin.firestore().collection('categories').doc(product.categoryId),
          { productCount: admin.firestore.FieldValue.increment(1) }
        );
        
        await batch.commit();
      }

      return null;
    } catch (error) {
      console.error('Error updating category product count:', error);
      return null;
    }
  });

// ============================================
// COUPON FUNCTIONS
// ============================================

/**
 * Validate and apply coupon code
 */
exports.validateCoupon = functions.https.onCall(async (data, context) => {
  const { couponCode, orderAmount, userId } = data;

  // Check authentication
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'User must be authenticated to use coupon'
    );
  }

  try {
    // Get coupon
    const couponDoc = await admin.firestore()
      .collection('coupons')
      .where('code', '==', couponCode.toUpperCase())
      .limit(1)
      .get();

    if (couponDoc.empty) {
      return { valid: false, message: 'Invalid coupon code' };
    }

    const coupon = couponDoc.docs[0].data();

    // Check if coupon is active
    if (!coupon.isActive) {
      return { valid: false, message: 'This coupon is no longer active' };
    }

    // Check usage limit
    if (coupon.usageLimit && coupon.usedCount >= coupon.usageLimit) {
      return { valid: false, message: 'This coupon has reached its usage limit' };
    }

    // Check minimum order amount
    if (orderAmount < coupon.minimumOrderAmount) {
      return { 
        valid: false, 
        message: `Minimum order amount of ₹${coupon.minimumOrderAmount} required` 
      };
    }

    // Check validity period
    const now = new Date();
    if (coupon.validFrom && now < coupon.validFrom.toDate()) {
      return { valid: false, message: 'This coupon is not yet valid' };
    }
    if (coupon.validUntil && now > coupon.validUntil.toDate()) {
      return { valid: false, message: 'This coupon has expired' };
    }

    // Calculate discount
    let discount = 0;
    if (coupon.discountType === 'percentage') {
      discount = (orderAmount * coupon.discountValue) / 100;
      if (coupon.maximumDiscount) {
        discount = Math.min(discount, coupon.maximumDiscount);
      }
    } else {
      discount = coupon.discountValue;
    }

    return {
      valid: true,
      discount: discount,
      message: 'Coupon applied successfully'
    };
  } catch (error) {
    console.error('Error validating coupon:', error);
    throw new functions.https.HttpsError(
      'internal',
      'Error validating coupon'
    );
  }
});

// ============================================
// NOTIFICATION FUNCTIONS
// ============================================

/**
 * Send promotional notification to all users
 */
exports.sendPromoNotification = functions.https.onCall(async (data, context) => {
  // Check if admin
  const userDoc = await admin.firestore()
    .collection('users')
    .doc(context.auth.uid)
    .get();
  
  if (userDoc.data().role !== 'admin') {
    throw new functions.https.HttpsError(
      'permission-denied',
      'Only admins can send promotional notifications'
    );
  }

  const { title, body, topic } = data;

  try {
    // Send to topic
    const payload = {
      notification: { title, body },
      data: { type: 'promo' }
    };

    if (topic) {
      await admin.messaging().sendToTopic(topic, payload);
    } else {
      // Send to all users (use with caution)
      const allTokens = [];
      const usersSnapshot = await admin.firestore()
        .collection('users')
        .get();
      
      for (const userDoc of usersSnapshot.docs) {
        const userData = userDoc.data();
        if (userData.fcmTokens) {
          allTokens.push(...userData.fcmTokens);
        }
      }

      // Send in batches of 500
      for (let i = 0; i < allTokens.length; i += 500) {
        const tokenBatch = allTokens.slice(i, i + 500);
        await admin.messaging().sendToDevice(tokenBatch, payload);
      }
    }

    return { success: true };
  } catch (error) {
    console.error('Error sending promo notification:', error);
    throw new functions.https.HttpsError('internal', 'Error sending notification');
  }
});

// ============================================
// ANALYTICS FUNCTIONS
// ============================================

/**
 * Track custom analytics event
 */
exports.trackEvent = functions.https.onCall(async (data, context) => {
  const { eventName, properties } = data;

  try {
    // Log to Analytics
    if (context.auth) {
      await admin.analytics().logEvent({
        name: eventName,
        params: {
          ...properties,
          user_id: context.auth.uid
        }
      });
    }

    return { success: true };
  } catch (error) {
    console.error('Error tracking event:', error);
    return { success: false };
  }
});

// ============================================
// USER MANAGEMENT FUNCTIONS
// ============================================

/**
 * Delete user account and all associated data
 */
exports.deleteUserAccount = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'User must be authenticated'
    );
  }

  const userId = context.auth.uid;

  try {
    // Delete user data (orders, cart, addresses, etc.)
    const batch = admin.firestore().batch();

    // Delete orders
    const ordersSnapshot = await admin.firestore()
      .collection('orders')
      .where('userId', '==', userId)
      .get();
    ordersSnapshot.forEach(doc => batch.delete(doc.ref));

    // Delete addresses
    const addressesSnapshot = await admin.firestore()
      .collection('addresses')
      .where('userId', '==', userId)
      .get();
    addressesSnapshot.forEach(doc => batch.delete(doc.ref));

    // Delete cart
    const cartDoc = admin.firestore().collection('carts').doc(userId);
    const cartItemsSnapshot = await cartDoc.collection('items').get();
    cartItemsSnapshot.forEach(doc => batch.delete(doc.ref));

    await batch.commit();

    // Delete auth user
    await admin.auth().deleteUser(userId);

    return { success: true };
  } catch (error) {
    console.error('Error deleting user account:', error);
    throw new functions.https.HttpsError(
      'internal',
      'Error deleting account'
    );
  }
});

// ============================================
// SCHEDULED FUNCTIONS
// ============================================

/**
 * Daily cleanup - remove expired flash sales
 */
exports.dailyCleanup = functions.pubsub
  .schedule('0 0 * * *')  // Daily at midnight
  .timeZone('Asia/Kolkata')
  .onRun(async (context) => {
    try {
      const now = admin.firestore.Timestamp.now();
      
      // Update expired flash sales
      const expiredFlashSales = await admin.firestore()
        .collection('products')
        .where('isFlashSale', '==', true)
        .where('flashSaleEndTime', '<', now)
        .get();

      const batch = admin.firestore().batch();
      expiredFlashSales.forEach(doc => {
        batch.update(doc.ref, {
          isFlashSale: false,
          flashSaleEndTime: null
        });
      });

      await batch.commit();
      console.log(`Cleaned up ${expiredFlashSales.size} expired flash sales`);
      
      return null;
    } catch (error) {
      console.error('Error in daily cleanup:', error);
      return null;
    }
  });
