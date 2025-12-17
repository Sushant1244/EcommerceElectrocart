# E-commerce Electrocart: Android E-commerce Application

**E-commerce Electrocart** is a modern, full-featured e-commerce application for Android, built with the latest technologies. This project serves as a comprehensive example of how to build a real-world shopping app, from user authentication to product management and a complete checkout flow.

## Features

- **User Authentication**: Secure login and signup with email and password, including a separate, secure login for the admin panel.
- **Modern UI**: A clean, professional, and responsive user interface built entirely with **Jetpack Compose**.
- **Product Management**: A complete admin panel that allows for adding, editing, and deleting products.
- **Shopping Cart**: A fully functional shopping cart that persists across the app.
- **Checkout Flow**: A multi-step checkout process that includes shipping details and payment methods.
- **Admin Panel**: A secure, role-based admin panel for managing products, orders, and users.
- **Sales Analytics**: A dashboard for admins to view sales graphs and top-selling products.

## Tech Stack

- **Kotlin**: The official, modern language for Android development.
- **Jetpack Compose**: Android's modern, declarative UI toolkit.
- **Firebase**: Used for authentication, real-time database, and cloud storage.
- **Coil**: A lightweight and efficient image loading library.
- **Material Design 3**: The latest design system for creating beautiful and consistent UIs.

## Getting Started

To get this project up and running on your local machine, please follow these steps:

1.  **Clone the repository**:
    ```
    git clone https://github.com/your-username/E-commerceElectrocart.git
    ```
2.  **Open in Android Studio**: Open the project in the latest version of Android Studio.
3.  **Firebase Setup**: This project requires a Firebase backend. To connect it, you will need to:
    - Create a new project in the [Firebase Console](https://console.firebase.google.com/).
    - Add an Android app to your Firebase project with the package name `com.example.e_commerceelectrocart`.
    - Download the `google-services.json` file from your Firebase project settings and place it in the `app/` directory.
    - In the Firebase console, enable **Email/Password** authentication.
    - Set up the **Realtime Database** and use the following security rules to allow users to manage their own data:
        ```json
        {
          "rules": {
            "Users": {
              "$uid": {
                ".write": "$uid === auth.uid",
                ".read": "$uid === auth.uid"
              }
            }
          }
        }
        ```

4.  **Run the app**: Build and run the app on an emulator or a physical device.

## Admin Panel

To access the admin panel, you must first create an admin user in your Firebase project with the following credentials:

-   **Email**: `admin@electrocart.com`
-   **Password**: `admin123`

Once you've created this user, you can log in with these credentials to access the admin dashboard.

## Contributing

Contributions are welcome! If you have any ideas, suggestions, or bug reports, please feel free to open an issue or submit a pull request.
