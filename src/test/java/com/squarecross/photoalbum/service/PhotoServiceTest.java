package com.squarecross.photoalbum.service;

import com.squarecross.photoalbum.domain.Album;
import com.squarecross.photoalbum.domain.Photo;
import com.squarecross.photoalbum.dto.PhotoDto;
import com.squarecross.photoalbum.repository.AlbumRepository;
import com.squarecross.photoalbum.repository.PhotoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class PhotoServiceTest {

    @Autowired
    PhotoService photoService;

    @Autowired
    PhotoRepository photoRepository;

    @Autowired
    AlbumRepository albumRepository;

    @Test
    void getPhoto() {
        Album album = new Album();
        album.setAlbumName("테스트");
        Album savedAlbum = albumRepository.save(album);

        Photo photo = new Photo();
        photo.setFileName("새 사진");
        photo.setAlbum(savedAlbum);
        Photo savedPhoto = photoRepository.save(photo);

        PhotoDto resPhoto = photoService.getPhoto(savedPhoto.getPhotoId());
        assertEquals("새 사진", resPhoto.getFileName());
    }

    @Test
    void getPhotoException() {
        assertThrows(EntityNotFoundException.class, () -> photoService.getPhoto(99L));
    }
}