package com.squarecross.photoalbum.service;

import com.squarecross.photoalbum.Constants;
import com.squarecross.photoalbum.domain.Album;
import com.squarecross.photoalbum.domain.Photo;
import com.squarecross.photoalbum.dto.AlbumDto;
import com.squarecross.photoalbum.repository.AlbumRepository;
import com.squarecross.photoalbum.repository.PhotoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AlbumServiceTest {

    @Autowired
    AlbumRepository albumRepository;

    @Autowired
    PhotoRepository photoRepository;

    @Autowired
    AlbumService albumService;

    @Test
    void getAlbum() {
        Album album = new Album();
        album.setAlbumName("테스트");
        Album savedAlbum = albumRepository.save(album);

        AlbumDto resAlbum = albumService.getAlbum(savedAlbum.getAlbumId());
        assertEquals("테스트", resAlbum.getAlbumName());
    }

    @Test
    void getAlbumException() {
        assertThrows(EntityNotFoundException.class, () -> albumService.getAlbum(99L));
    }

    @Test
    void getAlbumByAlbumName() {
        Album album = new Album();
        album.setAlbumName("테스트");
        Album savedAlbum = albumRepository.save(album);

        AlbumDto resAlbum = albumService.getAlbumByAlbumName("테스트");
        assertEquals(savedAlbum.getAlbumId(), resAlbum.getAlbumId());
    }

    @Test
    void getAlbumByAlbumNameException() {
        assertThrows(EntityNotFoundException.class, () -> albumService.getAlbumByAlbumName("없음"));
    }

    @Test
    void testPhotoCount() {
        Album album = new Album();
        album.setAlbumName("테스트");
        Album savedAlbum = albumRepository.save(album);

        Photo photo1 = new Photo();
        photo1.setFileName("사진1");
        photo1.setAlbum(savedAlbum);
        photoRepository.save(photo1);
        Photo photo2 = new Photo();
        photo2.setFileName("사진2");
        photo2.setAlbum(savedAlbum);
        photoRepository.save(photo2);

        AlbumDto resAlbum = albumService.getAlbum(savedAlbum.getAlbumId());

        assertEquals(resAlbum.getCount(), 2);
    }

    @Test
    void testAlbumCreate() throws IOException {
        AlbumDto albumDto = new AlbumDto();
        albumDto.setAlbumName("테스트 앨범");

        AlbumDto savedAlbum = albumService.crateAlbum(albumDto);

        assertEquals(albumDto.getAlbumName(), savedAlbum.getAlbumName());
        assertTrue(Files.exists(Paths.get(Constants.PATH_PREFIX + "/photos/original/" + savedAlbum.getAlbumId())));
        assertTrue(Files.exists(Paths.get(Constants.PATH_PREFIX + "/photos/thumb/" + savedAlbum.getAlbumId())));

        Files.deleteIfExists(Paths.get(Constants.PATH_PREFIX + "/photos/original/" + savedAlbum.getAlbumId()));
        Files.deleteIfExists(Paths.get(Constants.PATH_PREFIX + "/photos/thumb/" + savedAlbum.getAlbumId()));
    }
}