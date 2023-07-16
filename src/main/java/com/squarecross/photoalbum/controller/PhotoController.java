package com.squarecross.photoalbum.controller;

import com.squarecross.photoalbum.dto.PhotoDto;
import com.squarecross.photoalbum.service.PhotoService;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/albums/{albumId}/photos")
public class PhotoController {
    @Autowired
    private PhotoService photoService;

    @RequestMapping(value = "/{photoId}", method = RequestMethod.GET)
    public ResponseEntity<PhotoDto> getPhoto(@PathVariable("photoId") final long photoId) {
        PhotoDto photo = photoService.getPhoto(photoId);
        return new ResponseEntity<>(photo, HttpStatus.OK);
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    public ResponseEntity<List<PhotoDto>> uploadPhotos(@PathVariable("albumId") final long albumId,
                                                       @RequestParam("photos") MultipartFile[] files) {
        List<PhotoDto> photos = new ArrayList<>();
        for (MultipartFile file : files) {
            PhotoDto photoDto = photoService.savePhoto(file, albumId);
            photos.add(photoDto);
        }
        return new ResponseEntity<>(photos, HttpStatus.OK);
    }

    @RequestMapping(value = "/download", method = RequestMethod.GET)
    public void downloadPhotos(@RequestParam("photoIds") Long[] photoIds, HttpServletResponse response) {
        try {
            if (photoIds.length == 1) {
                File file = photoService.getImageFile(photoIds[0]);
                OutputStream outputStream = response.getOutputStream();
                IOUtils.copy(new FileInputStream(file), outputStream);
                outputStream.close();
            } else {
                ZipOutputStream outputStream = new ZipOutputStream(response.getOutputStream());
                for (Long photoId : photoIds) {
                    File file = photoService.getImageFile(photoId);
                    ZipEntry ze = new ZipEntry(file.getName());
                    outputStream.putNextEntry(ze);
                    IOUtils.copy(new FileInputStream(file), outputStream);
                    /*
                    FileInputStream inputStream = new FileInputStream(file);
                    byte buffer[] = new byte[1024];
                    int len;
                    while((len = inputStream.read(buffer, 0, 1024)) != -1) {
                        outputStream.write(buffer, 0, len);
                    }
                    inputStream.close();
                     */
                    outputStream.closeEntry();
                }
                outputStream.close();;
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Error");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<List<PhotoDto>> getPhotos(@PathVariable("albumId") final long albumId,
                                                    @RequestParam(value="sort", required = false, defaultValue = "byDate") final String sort) {
        List<PhotoDto> photos = photoService.getPhotos(albumId, sort);
        return new ResponseEntity<>(photos, HttpStatus.OK);
    }
}
