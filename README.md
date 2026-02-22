# Electrocart - E-commerce Mobile Application

A production-ready e-commerce mobile application built with Android (Kotlin) and Jetpack Compose, featuring Firebase integration for backend services.

## 📱 Features

### Authentication & User Management
- Email/Password authentication
- Google Sign-In integration
- Facebook Login integration
- Password reset functionality
- User profile management
- Role-based access control (Admin, User, Vendor)

### Product Management
- Product catalog with categories
- Search and filtering capabilities
- Product details with images and specifications
- Product reviews and ratings
- Featured products and flash sales
- Real-time inventory tracking

### Shopping Experience
- Shopping cart with quantity management
- Wishlist functionality
- Multiple address management
- Checkout process
- Order tracking
- Order history

### Admin Dashboard
- Sales analytics and reporting
- Product management (CRUD operations)
- Order management
- User management
- Category management

### Notifications & Analytics
- Push notifications for order updates
- Promotional notifications
- Firebase Analytics integration
- Crash reporting with Firebase Crashlytics

## 🏗️ Architecture

### Technology Stack
- **Language**: Kotlin 1.9+
- **UI Framework**: Jetpack Compose with Material 3
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 36

### Architecture Pattern
- **MVVM** (Model-View-ViewModel)
- **Clean Architecture** with separation of concerns
- **Hilt** for Dependency Injection
- **Kotlin Coroutines & Flow** for async operations
- **DataStore** for local preferences

### Project Structure
```
app/src/main/kotlin/com/example/e_commerceelectrocart/
├── firebase/          # Firebase managers (Auth, Storage, Analytics)
├── firestore/         # Firestore data models
├── repository/        # Data repositories (Product, Order, User)
├── di/                # Hilt dependency injection modules
├── service/           # Background services (FCM)
├── local/             # Local storage (DataStore)
├── ui/                # Compose UI theme
└── *.kt               # Activities and UI components
```

## 🔥 Firebase Services

### 1. Firebase Authentication
- Email/Password authentication
- Google Sign-In
- Facebook Login
- Email verification
- Password reset

### 2. Cloud Firestore (NoSQL)
- Real-time data synchronization
- Offline support
- Scalable database structure
- Collections: `users`, `products`, `orders`, `categories`, `addresses`, `notifications`, `reviews`

### 3. Firebase Realtime Database
- User profile data
- Real-time cart sync
- Chat/messaging

### 4. Firebase Storage
- Product images
- User profile photos
- Banner images
- Review images

### 5. Firebase Cloud Functions
- Order confirmation emails
- Inventory management
- Coupon validation
- Push notifications
- Scheduled cleanup tasks

### 6. Firebase Cloud Messaging
- Order status notifications
- Promotional notifications
- Custom notification channels

### 7. Firebase Analytics
- User behavior tracking
- Custom events
- User properties
- Conversion tracking

### 8. Firebase Crashlytics
- Crash reporting
- Non-fatal exception logging

## 🔒 Security

### Firestore Security Rules
- Role-based access control
- Users can only access their own data
- Admins have full access
- Data validation on write operations

### Storage Security Rules
- Image upload validation (size, type)
- User-specific profile access
- Admin-only product management

### Authentication Security
- Email verification required
- Password strength validation
- Session management
- FCM token management

## 📋 Firebase Security Rules Summary

```javascript
// Users - Own profile + Admin access
// Products - Public read, Admin write
// Orders - Owner + Admin access
// Addresses - Owner access only
// Cart - Owner access only
// Reviews - Authenticated users
```

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 11 or later
- Firebase project with enabled services

### Setup Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-repo/EcommerceElectrocart.git
   ```

2. **Create Firebase Project**
   - Go to Firebase Console
   - Create new project
   - Enable Authentication, Firestore, Realtime Database, Storage, Cloud Functions, Analytics, Crashlytics, Cloud Messaging

3. **Add Google Services**
   - Download `google-services.json` from Firebase Console
   - Place in `app/google-services.json`

4. **Configure OAuth Providers**
   - Set up Google Sign-In in Firebase Console
   - Set up Facebook Login in Firebase Console

5. **Build the project**
   ```bash
   ./gradlew assembleDebug
   ```

6. **Deploy Cloud Functions**
   ```bash
   cd functions
   npm install
   firebase deploy --only functions
   ```

7. **Deploy Security Rules**
   ```bash
   firebase deploy --only firestore:rules
   firebase deploy --only storage:rules
   ```

### Environment Variables
Create `local.properties`:
```properties
sdk.dir=/path/to/android/sdk
```

## 📂 Key Files

| File | Description |
|------|-------------|
| [`app/build.gradle.kts`](app/build.gradle.kts) | Dependencies and build configuration |
| [`app/src/main/kotlin/.../firebase/FirebaseManager.kt`](app/src/main/kotlin/com/example/e_commerceelectrocart/firebase/FirebaseManager.kt) | Central Firebase operations |
| [`app/src/main/kotlin/.../firebase/AuthManager.kt`](app/src/main/kotlin/com/example/e_commerceelectrocart/firebase/AuthManager.kt) | Authentication handling |
| [`app/src/main/kotlin/.../repository/ProductRepository.kt`](app/src/main/kotlin/com/example/e_commerceelectrocart/repository/ProductRepository.kt) | Product CRUD operations |
| [`app/src/main/kotlin/.../repository/OrderRepository.kt`](app/src/main/kotlin/com/example/e_commerceelectrocart/repository/OrderRepository.kt) | Order management |
| [`app/src/main/kotlin/.../firestore/Models.kt`](app/src/main/kotlin/com/example/e_commerceelectrocart/firestore/Models.kt) | Data models |
| [`app/src/main/assets/firestore.rules`](app/src/main/assets/firestore.rules) | Firestore security rules |
| [`app/src/main/assets/storage.rules`](app/src/main/assets/storage.rules) | Storage security rules |
| [`app/src/main/assets/cloud-functions.md`](app/src/main/assets/cloud-functions.md) | Cloud Functions code |

## 🔧 Configuration

### Admin Setup
Default admin email: `admin@electrocart.com`

To create an admin:
1. Register a new user
2. Manually update the user's role to `admin` in Firestore

### Payment Methods
Currently supports:
- Cash on Delivery (COD)
- Credit/Debit Cards (Ready for integration)
- UPI (Ready for integration)
- Net Banking (Ready for integration)

### Shipping
- Free shipping on orders above ₹499
- Standard shipping: ₹49

## 📊 Analytics Events

| Event | Description |
|-------|-------------|
| `sign_up` | User registration |
| `login` | User login |
| `logout` | User logout |
| `add_to_cart` | Product added to cart |
| `remove_from_cart` | Product removed from cart |
| `begin_checkout` | Checkout started |
| `purchase` | Order completed |
| `order_status_updated` | Order status changed |
| `product_viewed` | Product details viewed |
| `search` | Product search |

## 🔔 Push Notification Types

| Type | Channel | Description |
|------|---------|-------------|
| `order_status` | Order Updates | Order status changes |
| `promo` | Promotions | Deals and offers |
| `product` | General | Product updates |
| `chat` | General | New messages |

## 📱 Screenshots

The app includes the following screens:
- Login/Signup with social options
- Dashboard with categories and deals
- Product listing and details
- Shopping cart
- Checkout and payment
- Order tracking
- User profile
- Admin dashboard
- Analytics view

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- Firebase team for excellent documentation
- Jetpack Compose community
- Material Design guidelines

## 📞 Support

For issues and questions:
- Open an issue on GitHub
- Email: support@electrocart.com

---

**Version**: 1.0.0  
**Last Updated**: 2026-02-22
