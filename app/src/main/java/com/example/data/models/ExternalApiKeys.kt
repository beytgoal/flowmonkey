package com.example.data.models

data class UserProfile(
    val isLoggedIn: Boolean = true,
    val isGLoggedIn: Boolean = true,
    val userName: String = "Creator Google User",
    val userEmail: String = "cpktemon@gmail.com",
    val photoUrl: String? = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100&auto=format&fit=crop&q=80",
    val firebaseUid: String = "fb_auth_usr_7781920",
    val firebaseAuthStatus: String = "Terautentikasi via Firebase (Google OAuth 2.0)",
    val isCustomGeminiKeyActive: Boolean = true
)

data class MultiModelApiKeys(
    val googleGeminiApiKey: String = "",
    val openAiSoraApiKey: String = "",
    val anthropicClaudeApiKey: String = "",
    val kimiAiDirectorApiKey: String = "",
    val runwayGen3ApiKey: String = "",
    val lumaDreamMachineApiKey: String = ""
)

object SupportedAiModels {
    val models = listOf(
        "Google Video 3.1 Pro Recommended",
        "Google Video 3.1 Fast Preview",
        "OpenAI Sora (ChatGPT API)",
        "Anthropic Claude 3.5 Sonnet (Director)",
        "Kimi AI Director 2.0",
        "Runway Gen-3 Alpha",
        "Luma Dream Machine"
    )
}
