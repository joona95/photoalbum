package com.squarecross.photoalbum.service;

import com.squarecross.photoalbum.Constants;
import com.squarecross.photoalbum.domain.Album;
import com.squarecross.photoalbum.domain.Photo;
import com.squarecross.photoalbum.dto.AlbumDto;
import com.squarecross.photoalbum.mapper.AlbumMapper;
import com.squarecross.photoalbum.repository.AlbumRepository;
import com.squarecross.photoalbum.repository.PhotoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AlbumService {

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private PhotoRepository photoRepository;

    public AlbumDto getAlbum(Long albumId) {
        Optional<Album> album = albumRepository.findById(albumId);
        if (album.isPresent()) {
            AlbumDto albumDto = AlbumMapper.convertToDto(album.get());
            albumDto.setCount(photoRepository.countByAlbum_AlbumId(albumId));
            return albumDto;
        } else {
            throw new EntityNotFoundException(String.format("앨범 아이디 %d로 조회되지 않았습니다", albumId));
        }
    }

    public AlbumDto getAlbumByAlbumName(String albumName) {
        Optional<Album> album = albumRepository.findByAlbumName(albumName);
        if (album.isPresent()) {
            AlbumDto albumDto = AlbumMapper.convertToDto(album.get());
            albumDto.setCount(photoRepository.countByAlbum_AlbumId(albumDto.getAlbumId()));
            return albumDto;
        } else {
            throw new EntityNotFoundException(String.format("%s 라는 앨범명을 가진 앨범이 존재하지 않습니다.", albumName));
        }
    }

    public AlbumDto crateAlbum(AlbumDto albumDto) throws IOException {
        Album album = AlbumMapper.convertToModel(albumDto);
        albumRepository.save(album);
        createAlbumDirectories(album);
        return AlbumMapper.convertToDto(album);
    }

    public void createAlbumDirectories(Album album) throws IOException {
        Files.createDirectories(Paths.get(Constants.PATH_PREFIX + "/photos/original/" + album.getAlbumId()));
        Files.createDirectories(Paths.get(Constants.PATH_PREFIX + "/photos/thumb/" + album.getAlbumId()));
    }

    public List<AlbumDto> getAlbumList(String keyword, String sort, String order) {
        List<Album> albums;
        if (Objects.equals(sort, "byName") && Objects.equals(order, "desc")) {
            albums = albumRepository.findByAlbumNameContainingOrderByAlbumNameDesc(keyword);
        } else if (Objects.equals(sort, "byName") && Objects.equals(order, "asc")) {
            albums = albumRepository.findByAlbumNameContainingOrderByAlbumNameAsc(keyword);
        } else if (Objects.equals(sort, "byDate") && Objects.equals(order, "desc")) {
            albums = albumRepository.findByAlbumNameContainingOrderByCreatedAtDesc(keyword);
        } else if (Objects.equals(sort, "byDate") && Objects.equals(order, "asc")) {
            albums = albumRepository.findByAlbumNameContainingOrderByCreatedAtAsc(keyword);
        } else {
            throw new IllegalArgumentException("알 수 없는 정렬 기준입니다.");
        }
        List<AlbumDto> albumDtos = AlbumMapper.convertToDtoList(albums);

        for (AlbumDto albumDto : albumDtos) {
            List<Photo> top4 = photoRepository.findTop4ByAlbum_AlbumIdOrderByUploadedAtDesc(albumDto.getAlbumId());
            albumDto.setThumbUrls(top4.stream().map(Photo::getThumbUrl).map(c -> Constants.PATH_PREFIX + c).collect(Collectors.toList()));
        }

        return albumDtos;
    }

    public AlbumDto changeName(Long albumId, AlbumDto albumDto) {
        Optional<Album> album = albumRepository.findById(albumId);
        if (album.isEmpty()) {
            throw new NoSuchElementException(String.format("Album ID '%d'가 존재하지 않습니다.", albumId));
        }

        Album updatedAlbum = album.get();
        updatedAlbum.setAlbumName(albumDto.getAlbumName());
        Album savedAlbum = albumRepository.save(updatedAlbum);
        return AlbumMapper.convertToDto(savedAlbum);
    }
}
