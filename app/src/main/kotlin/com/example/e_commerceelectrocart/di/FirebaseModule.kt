package com.example.e_commerceelectrocart.di

import android.content.Context
import com.example.e_commerceelectrocart.firebase.AuthManager
import com.example.e_commerceelectrocart.firebase.FirebaseManager
import com.example.e_commerceelectrocart.repository.OrderRepository
import com.example.e_commerceelectrocart.repository.ProductRepository
import com.example.e_commerceelectrocart.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing Firebase-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseManager(
        @ApplicationContext context: Context
    ): FirebaseManager {
        return FirebaseManager(context)
    }

    @Provides
    @Singleton
    fun provideAuthManager(
        @ApplicationContext context: Context,
        firebaseManager: FirebaseManager
    ): AuthManager {
        return AuthManager(context, firebaseManager)
    }

    @Provides
    @Singleton
    fun provideProductRepository(
        firebaseManager: FirebaseManager
    ): ProductRepository {
        return ProductRepository(firebaseManager)
    }

    @Provides
    @Singleton
    fun provideOrderRepository(
        firebaseManager: FirebaseManager
    ): OrderRepository {
        return OrderRepository(firebaseManager)
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        firebaseManager: FirebaseManager
    ): UserRepository {
        return UserRepository(firebaseManager)
    }
}
