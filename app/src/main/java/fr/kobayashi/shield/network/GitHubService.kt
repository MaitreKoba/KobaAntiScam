package fr.kobayashi.shield.network

import retrofit2.http.GET
import retrofit2.http.Path

interface GitHubService {
    
    data class Release(
        val tag_name: String? = null,
        val name: String? = null,
        val body: String? = null,
        val assets: List<Asset> = emptyList()
    )
    
    data class Asset(
        val name: String? = null,
        val browser_download_url: String? = null
    )
    
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Release
}
