package com.squarecross.photoalbum.repository;

import com.squarecross.photoalbum.domain.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {
    int countByAlbum_AlbumId(Long albumId);

    List<Photo> findTop4ByAlbum_AlbumIdOrderByUploadedAtDesc(Long albumId);

    Optional<Photo> findByFileNameAndAlbum_AlbumId(String photoName, Long albumId);

    List<Photo> findByAlbum_AlbumIdOrderByUploadedAt(Long albumId);

    List<Photo> findByAlbum_AlbumIdOrderByFileName(Long albumId);

    Optional<Photo> findByAlbum_AlbumIdAndPhotoId(Long albumId, Long photoId);
}
