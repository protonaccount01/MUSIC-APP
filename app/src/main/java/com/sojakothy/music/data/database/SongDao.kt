package com.sojakothy.music.data.database

import androidx.room.*
import com.sojakothy.music.data.models.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Query("SELECT * FROM songs ORDER BY addedToDbAt DESC")
    fun getAllSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE isDownloaded = 1 ORDER BY addedToDbAt DESC")
    fun getDownloadedSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY addedToDbAt DESC")
    fun getFavoriteSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongById(id: String): Song?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<Song>)

    @Update
    suspend fun updateSong(song: Song)

    @Delete
    suspend fun deleteSong(song: Song)

    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun deleteSongById(id: String)

    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE songs SET isDownloaded = :isDownloaded, downloadPath = :path WHERE id = :id")
    suspend fun updateDownloadStatus(id: String, isDownloaded: Boolean, path: String?)

    @Query("UPDATE songs SET streamUrl = :url WHERE id = :id")
    suspend fun updateStreamUrl(id: String, url: String?)

    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' ORDER BY addedToDbAt DESC")
    suspend fun searchLocalSongs(query: String): List<Song>

    @Query("SELECT COUNT(*) FROM songs WHERE isDownloaded = 1")
    suspend fun getDownloadedCount(): Int

    @Query("DELETE FROM songs WHERE isDownloaded = 0 AND isFavorite = 0")
    suspend fun clearCache()
}
