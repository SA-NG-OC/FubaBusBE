package com.example.Fuba_BE.service;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    private static final String DEFAULT_AVATAR_URL = "https://i.pinimg.com/736x/61/85/c3/6185c30215db7423445ee74c02e729b6.jpg";
    private static final String FOLDER = "fuba-bus/avatars";

    /**
     * Upload image to Cloudinary
     * 
     * @param file MultipartFile to upload
     * @return URL of uploaded image
     */
    public String uploadImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return DEFAULT_AVATAR_URL;
        }

        try {
            Map<String, Object> uploadParams = ObjectUtils.asMap(
                    "folder", FOLDER,
                    "resource_type", "image",
                    "transformation", new com.cloudinary.Transformation()
                            .width(400).height(400).crop("fill").gravity("face"));

            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);
            String imageUrl = (String) uploadResult.get("secure_url");
            log.info("Image uploaded successfully: {}", imageUrl);
            return imageUrl;
        } catch (Exception e) {
            log.error("Failed to upload image to Cloudinary", e);
            throw new IOException("Failed to upload image: " + e.getMessage());
        }
    }

    /**
     * Delete image by URL
     * 
     * @param imageUrl Full Cloudinary URL
     * @return true if deleted successfully
     */
    public boolean deleteImageByUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty() || imageUrl.equals(DEFAULT_AVATAR_URL)) {
            log.info("Skipping delete for default avatar or null URL");
            return false;
        }

        try {
            // Extract public ID from URL
            // Example:
            // https://res.cloudinary.com/dwa3wh9yb/image/upload/v123456/fuba-bus/avatars/xyz.jpg
            // We need: fuba-bus/avatars/xyz
            Pattern pattern = Pattern.compile("/" + FOLDER + "/.+?(?:\\.(png|jpg|jpeg|gif|webp))$",
                    Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(imageUrl);

            if (!matcher.find()) {
                log.warn("Could not extract public ID from URL: {}", imageUrl);
                return false;
            }

            String publicId = matcher.group(0).substring(1); // Remove leading /
            publicId = publicId.replaceAll("\\.(png|jpg|jpeg|gif|webp)$", ""); // Remove extension

            log.info("Extracted public ID: {}", publicId);

            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            String resultStatus = (String) result.get("result");

            log.info("Delete result: {}", resultStatus);
            return "ok".equals(resultStatus);
        } catch (Exception e) {
            log.error("Failed to delete image from Cloudinary: {}", imageUrl, e);
            return false;
        }
    }

    /**
     * Get default avatar URL
     */
    public String getDefaultAvatarUrl() {
        return DEFAULT_AVATAR_URL;
    }
}
