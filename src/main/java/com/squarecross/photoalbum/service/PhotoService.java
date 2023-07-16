package com.squarecross.photoalbum.service;

import com.squarecross.photoalbum.Constants;
import com.squarecross.photoalbum.domain.Album;
import com.squarecross.photoalbum.domain.Photo;
import com.squarecross.photoalbum.dto.PhotoDto;
import com.squarecross.photoalbum.mapper.PhotoMapper;
import com.squarecross.photoalbum.repository.AlbumRepository;
import com.squarecross.photoalbum.repository.PhotoRepository;
import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.persistence.EntityNotFoundException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class PhotoService {
    private final String original_path = Constants.PATH_PREFIX + "/photos/original";
    private final String thumb_path = Constants.PATH_PREFIX + "/photos/thumb";
    @Autowired
    private PhotoRepository photoRepository;
    @Autowired
    private AlbumRepository albumRepository;

    public PhotoDto getPhoto(Long photoId) {
        Optional<Photo> photo = photoRepository.findById(photoId);
        if (photo.isPresent()) {
            return PhotoMapper.convertToDto(photo.get());
        } else {
            throw new EntityNotFoundException(String.format("사진 아이디 %d로 조회되지 않았습니다", photoId));
        }
    }

    public List<PhotoDto> getPhotos(Long albumId, String sort) {
        List<Photo> photos;
        if (Objects.equals("byName", sort)) {
            photos = photoRepository.findByAlbum_AlbumIdOrderByFileName(albumId);
        } else if (Objects.equals("byDate", sort)) {
            photos = photoRepository.findByAlbum_AlbumIdOrderByUploadedAt(albumId);
        } else {
            throw new IllegalArgumentException("알 수 없는 정렬 기준입니다.");
        }
        return PhotoMapper.convertToDtoList(photos);
    }

    public void movePhoto(Long fromAlbumId, Long toAlbumId, Long photoId) {
        Photo photo = photoRepository.findByAlbum_AlbumIdAndPhotoId(fromAlbumId, photoId).orElseThrow(() -> {
            throw new EntityNotFoundException("해당 앨범에 사진이 존재하지 않습니다");
        });
        Album toAlbum = albumRepository.findById(toAlbumId).orElseThrow(() -> {
            throw new EntityNotFoundException("앨범이 존재하지 않습니다");
        });

        moveFile(fromAlbumId, toAlbumId, photo.getFileName());
        photo.setAlbum(toAlbum);
        photoRepository.save(photo);
    }

    public void moveFile(Long fromAlbumId, Long toAlbumId, String fileName) {
        try {
            Path originPath = Paths.get(original_path + "/" + fromAlbumId + "/" + fileName);
            Path originPathToMove = Paths.get(original_path + "/" + toAlbumId + "/" + fileName);
            Files.move(originPath, originPathToMove);
            Path thumbPath = Paths.get(thumb_path + "/" + fromAlbumId + "/" + fileName);
            Path thumbPathToMove = Paths.get(thumb_path + "/" + toAlbumId + "/" + fileName);
            Files.move(thumbPath, thumbPathToMove);
        } catch (Exception e) {
            throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
        }
    }

    public PhotoDto savePhoto(MultipartFile file, Long albumId) {
        Optional<Album> album = albumRepository.findById(albumId);
        if (album.isEmpty()) {
            throw new EntityNotFoundException("앨범이 존재하지 않습니다");
        }
        String fileName = file.getOriginalFilename();
        int fileSize = (int) file.getSize();
        fileName = getNextFileName(fileName, albumId);
        saveFile(file, albumId, fileName);

        Photo photo = new Photo();
        photo.setOriginalUrl("/photos/original/" + albumId + "/" + fileName);
        photo.setThumbUrl("/photos/thumb/" + albumId + "/" + fileName);
        photo.setFileName(fileName);
        photo.setFileSize(fileSize);
        photo.setAlbum(album.get());
        Photo createdPhoto = photoRepository.save(photo);
        return PhotoMapper.convertToDto(createdPhoto);
    }

    public String getNextFileName(String fileName, Long albumId) {
        String fileNameNoExt = StringUtils.stripFilenameExtension(fileName);
        String ext = StringUtils.getFilenameExtension(fileName);

        Optional<Photo> photo = photoRepository.findByFileNameAndAlbum_AlbumId(fileName, albumId);

        int count = 2;
        while (photo.isPresent()) {
            fileName = String.format("%s (%d).%s", fileNameNoExt, count, ext);
            photo = photoRepository.findByFileNameAndAlbum_AlbumId(fileName, albumId);
            count++;
        }

        return fileName;
    }

    private void saveFile(MultipartFile file, Long albumId, String fileName) {
        try {
            if (!Objects.requireNonNull(file.getContentType()).startsWith("image")) {
                throw new IllegalArgumentException("No Image Extension");
            }
            String filePath = albumId + "/" + fileName;
            Files.copy(file.getInputStream(), Paths.get(original_path + "/" + filePath));
            File originalFile = new File(original_path + "/" + filePath);
            if (!originalFile.exists() || !originalFile.isFile()) {
                throw new RuntimeException("No file exists in " + filePath);
            }

            BufferedImage thumbImg = Scalr.resize(ImageIO.read(file.getInputStream()), Constants.THUMB_SIZE, Constants.THUMB_SIZE);
            File thumbFile = new File(thumb_path + "/" + filePath);
            if (!thumbFile.exists() || !thumbFile.isFile()) {
                throw new RuntimeException("No file exists in " + filePath);
            }
            String ext = StringUtils.getFilenameExtension(fileName);
            if (ext == null) {
                throw new IllegalArgumentException("No Extension");
            }
            ImageIO.write(thumbImg, ext, thumbFile);
        } catch (Exception e) {
            throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
        }
    }

    public File getImageFile(Long photoId) {
        Optional<Photo> photo = photoRepository.findById(photoId);
        if (photo.isEmpty()) {
            throw new EntityNotFoundException(String.format("사진 ID %d를 찾을 수 없습니다.", photoId));
        }
        return new File(Constants.PATH_PREFIX + photo.get().getOriginalUrl());
    }
}
