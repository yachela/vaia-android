package com.vaia.data.api

import com.vaia.domain.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface VaiaApiService {

    // Auth endpoints
    @POST("register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<AuthTokens>>

    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<LoginResponseData>>

    @POST("logout")
    suspend fun logout(): Response<ApiResponse<Unit>>

    @GET("user")
    suspend fun getCurrentUser(): Response<ApiResponse<User>>

    @PUT("user")
    suspend fun updateCurrentUser(@Body request: UpdateUserProfileRequest): Response<ApiResponse<User>>

    @Multipart
    @POST("user/avatar")
    suspend fun uploadAvatar(@Part avatar: MultipartBody.Part): Response<ApiResponse<User>>

    // Trip endpoints
    @GET("trips")
    suspend fun getTrips(@Query("page") page: Int = 1): Response<PaginatedResponse<Trip>>

    @GET("trips/{tripId}")
    suspend fun getTrip(@Path("tripId") tripId: String): Response<ApiResponse<Trip>>

    @POST("trips")
    suspend fun createTrip(@Body trip: CreateTripRequest): Response<ApiResponse<Trip>>

    @PUT("trips/{tripId}")
    suspend fun updateTrip(@Path("tripId") tripId: String, @Body trip: UpdateTripRequest): Response<ApiResponse<Trip>>

    @DELETE("trips/{tripId}")
    suspend fun deleteTrip(@Path("tripId") tripId: String): Response<ApiResponse<Unit>>

    @Streaming
    @GET("trips/{tripId}/export/itinerary.pdf")
    suspend fun exportItineraryPdf(@Path("tripId") tripId: String): Response<ResponseBody>

    @Streaming
    @GET("trips/{tripId}/export/expenses.csv")
    suspend fun exportExpensesCsv(@Path("tripId") tripId: String): Response<ResponseBody>

    @POST("trips/{tripId}/suggestions")
    suspend fun getActivitySuggestions(@Path("tripId") tripId: String): Response<SuggestionsResponse>

    // Activity endpoints
    @GET("trips/{tripId}/activities")
    suspend fun getActivities(@Path("tripId") tripId: String): Response<PaginatedResponse<Activity>>

    @GET("trips/{tripId}/activities/{activityId}")
    suspend fun getActivity(@Path("tripId") tripId: String, @Path("activityId") activityId: String): Response<ApiResponse<Activity>>

    @POST("trips/{tripId}/activities")
    suspend fun createActivity(@Path("tripId") tripId: String, @Body activity: CreateActivityRequest): Response<ApiResponse<Activity>>

    @PUT("trips/{tripId}/activities/{activityId}")
    suspend fun updateActivity(@Path("tripId") tripId: String, @Path("activityId") activityId: String, @Body activity: UpdateActivityRequest): Response<ApiResponse<Activity>>

    @DELETE("trips/{tripId}/activities/{activityId}")
    suspend fun deleteActivity(@Path("tripId") tripId: String, @Path("activityId") activityId: String): Response<ApiResponse<Unit>>

    // Expense endpoints
    @GET("trips/{tripId}/expenses")
    suspend fun getExpenses(@Path("tripId") tripId: String): Response<PaginatedResponse<Expense>>

    @GET("trips/{tripId}/expenses/{expenseId}")
    suspend fun getExpense(@Path("tripId") tripId: String, @Path("expenseId") expenseId: String): Response<ApiResponse<Expense>>

    @Multipart
    @POST("trips/{tripId}/expenses")
    suspend fun createExpense(
        @Path("tripId") tripId: String,
        @Part("amount") amount: RequestBody,
        @Part("description") description: RequestBody,
        @Part("date") date: RequestBody,
        @Part("category") category: RequestBody,
        @Part receiptImage: MultipartBody.Part?
    ): Response<ApiResponse<Expense>>

    @Multipart
    @PUT("trips/{tripId}/expenses/{expenseId}")
    suspend fun updateExpense(
        @Path("tripId") tripId: String,
        @Path("expenseId") expenseId: String,
        @Part("amount") amount: RequestBody,
        @Part("description") description: RequestBody,
        @Part("date") date: RequestBody,
        @Part("category") category: RequestBody,
        @Part receiptImage: MultipartBody.Part?
    ): Response<ApiResponse<Expense>>

    @DELETE("trips/{tripId}/expenses/{expenseId}")
    suspend fun deleteExpense(@Path("tripId") tripId: String, @Path("expenseId") expenseId: String): Response<ApiResponse<Unit>>

    @Streaming
    @GET("trips/{tripId}/expenses/{expenseId}/receipt")
    suspend fun downloadReceipt(@Path("tripId") tripId: String, @Path("expenseId") expenseId: String): Response<ResponseBody>

    // Document endpoints
    @GET("trips/{tripId}/documents")
    suspend fun getDocuments(@Path("tripId") tripId: String): Response<ApiResponse<List<Document>>>

    @Multipart
    @POST("trips/{tripId}/documents")
    suspend fun uploadDocument(
        @Path("tripId") tripId: String,
        @Part document: MultipartBody.Part,
        @Part("description") description: RequestBody?,
        @Part("category") category: RequestBody?
    ): Response<ApiResponse<Document>>

    @DELETE("documents/{documentId}")
    suspend fun deleteDocument(@Path("documentId") documentId: String): Response<ApiResponse<Unit>>

    // Document Checklist endpoints
    @GET("trips/{tripId}/checklist")
    suspend fun getDocumentChecklist(@Path("tripId") tripId: String): Response<ApiResponse<TripDocumentChecklist>>

    @POST("trips/{tripId}/checklist/items")
    suspend fun addChecklistItem(
        @Path("tripId") tripId: String,
        @Body request: AddChecklistItemRequest
    ): Response<ApiResponse<ChecklistItem>>

    @PATCH("checklist/items/{itemId}/complete")
    suspend fun toggleChecklistItemComplete(
        @Path("itemId") itemId: String,
        @Body request: ToggleCompleteRequest
    ): Response<ApiResponse<ChecklistItem>>

    @DELETE("checklist/items/{itemId}")
    suspend fun deleteChecklistItem(@Path("itemId") itemId: String): Response<ApiResponse<Unit>>

    @Multipart
    @POST("checklist/items/{itemId}/documents")
    suspend fun uploadChecklistDocument(
        @Path("itemId") itemId: String,
        @Part document: MultipartBody.Part
    ): Response<ApiResponse<ChecklistDocument>>

    @POST("checklist/items/{itemId}/documents/from-drive")
    suspend fun importFromGoogleDrive(
        @Path("itemId") itemId: String,
        @Body request: ImportFromDriveRequest
    ): Response<ApiResponse<ChecklistDocument>>

    @GET("checklist/documents/{documentId}/preview")
    suspend fun previewChecklistDocument(@Path("documentId") documentId: String): Response<ApiResponse<DocumentPreviewResponse>>

    @DELETE("checklist/documents/{documentId}")
    suspend fun deleteChecklistDocument(@Path("documentId") documentId: String): Response<ApiResponse<Unit>>

    // Packing List endpoints
    @GET("trips/{tripId}/packing-list")
    suspend fun getPackingList(@Path("tripId") tripId: String): Response<ApiResponse<PackingList>>

    @POST("trips/{tripId}/packing-list/generate")
    suspend fun generatePackingList(@Path("tripId") tripId: String): Response<ApiResponse<PackingList>>

    @POST("trips/{tripId}/packing-list/weather-suggestions")
    suspend fun getWeatherSuggestions(@Path("tripId") tripId: String): Response<ApiResponse<WeatherSuggestionsResponse>>

    @POST("trips/{tripId}/packing-list/items")
    suspend fun addPackingItem(
        @Path("tripId") tripId: String,
        @Body request: AddPackingItemRequest
    ): Response<ApiResponse<PackingItemResponse>>

    @PATCH("packing-list/items/{itemId}/toggle")
    suspend fun togglePackingItem(@Path("itemId") itemId: String): Response<ApiResponse<PackingItemResponse>>

    @DELETE("packing-list/items/{itemId}")
    suspend fun deletePackingItem(@Path("itemId") itemId: String): Response<Unit>
}
