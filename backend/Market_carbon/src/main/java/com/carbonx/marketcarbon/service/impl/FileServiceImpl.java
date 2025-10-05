package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.config.StorageProperties;
import com.carbonx.marketcarbon.exception.StorageException;
import com.carbonx.marketcarbon.exception.StorageFileNotFoundException;
import com.carbonx.marketcarbon.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@Service
public class FileServiceImpl implements FileService {

    private Path rootLocation;

    @Autowired
    public FileServiceImpl(StorageProperties properties) {
        // Check co file ko
        if(properties.getLocation().trim().length() == 0){
            throw new StorageException("File upload location cannot be empty");
        }
        this.rootLocation = Paths.get(properties.getLocation());
    }

    @Override
    public void init() {
        try {
            Files.createDirectories(rootLocation);
        }
        catch (IOException e) {
            throw new StorageException("Could not initialize storage", e);
        }
    }

    @Override
    public void storeTo(MultipartFile file , String customDir) {
        try{
            if(file.isEmpty()){
                throw new StorageException("File upload empty");
            }
            // file upload lên được lưu ngay trên ổ cứng luôn đặt dưới rootLocation
            Path dir = this.rootLocation.resolve(customDir).normalize();
            Files.createDirectories(dir);
            //Tạo toàn bộ cây thư mục nếu nó chưa tồn tại.
            //Java sẽ tự động tạo lần lượt:
            //upload/
            //  └── cva/
            //       └── 3/
            //            └── company/
            //                 └── 7/

            // đặt tên file an toàn & tránh trùng
            String safeName = Paths.get(file.getOriginalFilename()).getFileName().toString();
            String uniqueName = java.util.UUID.randomUUID() + "_" + safeName;

            Path destination = dir.resolve( // → Ghép đường dẫn gốc rootLocation (nơi lưu file, ví dụ upload-dir)
                            Paths.get(file.getOriginalFilename())) // với tên file mà user upload
                            .normalize() // normalize() loại bỏ các ký tự  ../
                            .toAbsolutePath(); //toAbsolutePath() để chuyển sang đường dẫn tuyệt đối

            if(!destination.startsWith(dir.toAbsolutePath())){
                //đoạn này sẽ phát hiện file muốn ghi ra ngoài thư mục upload-dir và chặn lại.
                throw new StorageException("Cannot store file outside current directory");
            }
            try(InputStream inputStream = file.getInputStream()) { // Mở luồng đọc file từ request
                    Files.copy(inputStream,
                            destination);// Nếu file trùng tên,sẽ  catch lỗi ngay , tránh ghi đè
            }
        }
        catch (IOException e) {
            throw new StorageException("Could not store file", e);
        }
    }


    @Override
    public Stream<Path> loadAll() {
        try{
                return Files.walk(this.rootLocation, 1) // Duyệt (walk) toàn bộ file trong thư mục rootLocation,
                        // sâu tối đa 1 cấp (1 nghĩa là chỉ duyệt file, không lặn xuống subfolder
                        .filter(path -> !path.equals(this.rootLocation)) // filter bỏ chính thư mục gốc (upload-dir) ra
                        .map(this.rootLocation::relativize); // relativize để trả về đường dẫn tương đối
        }catch (IOException e){
            throw new StorageException("Could not load all files", e);
        }
    }

    @Override
    public Path load(String filename) {
        return rootLocation.resolve(filename);
    }


    //tải ngược file từ server — tức là khi người dùng muốn “download” hoặc “xem” file đã upload trước đó.
    @Override
    public Resource loadAsResource(String filename) {
        try {
            Path file = this.rootLocation.resolve(filename).normalize(); // trả về đường dẫn đầy đủ tới file cần tải
            if (!file.startsWith(this.rootLocation)) {
                throw new StorageFileNotFoundException("Outside root");
            }
            Resource resource = new UrlResource(file.toUri()); // chuyển Path → URI, ví dụ file:///C:/upload-dir/avatar.png
            if (resource.exists() || resource.isReadable()) {
                return resource;
            }
            else {
                throw new StorageFileNotFoundException(
                        "Could not read file: " + filename);

            }
        }
        catch (MalformedURLException e) {
            throw new StorageFileNotFoundException("Could not read file: " + filename, e);
        }
    }

    @Override
    public void deleteAll() {
        FileSystemUtils.deleteRecursively(rootLocation.toFile());
    }
}
