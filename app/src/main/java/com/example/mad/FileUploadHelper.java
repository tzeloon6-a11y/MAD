package com.example.mad;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FileUploadHelper {
    
    // Maximum file size: 5MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB in bytes
    
    // Allowed MIME types
    private static final String[] ALLOWED_MIME_TYPES = {
        "application/pdf",
        "image/jpeg",
        "image/png"
    };
    
    // Allowed file extensions
    private static final String[] ALLOWED_EXTENSIONS = {
        ".pdf", ".PDF",
        ".jpg", ".JPG", ".jpeg", ".JPEG",
        ".png", ".PNG"
    };
    
    public interface UploadCallback {
        void onSuccess(String fileUrl);
        void onError(String error);
    }
    
    /**
     * Uploads a file to Supabase Storage
     * @param context Activity context
     * @param fileUri URI of the selected file
     * @param userId User ID to create unique file name
     * @param callback Callback for success/error
     */
    public static void uploadResume(Context context, Uri fileUri, String userId, UploadCallback callback) {
        try {
            // Step 1: Validate MIME type
            String mimeType = context.getContentResolver().getType(fileUri);
            if (!isValidMimeType(mimeType)) {
                callback.onError("Invalid file type. Only PDF, JPEG, and PNG files are allowed.");
                return;
            }
            
            // Step 2: Check file size before reading
            long fileSize = getFileSize(context, fileUri);
            if (fileSize <= 0) {
                callback.onError("Could not determine file size");
                return;
            }
            if (fileSize > MAX_FILE_SIZE) {
                callback.onError("File size exceeds 5MB limit. Please choose a smaller file.");
                return;
            }
            
            // Step 3: Validate file extension
            String fileName = getFileName(context, fileUri);
            if (fileName != null && !isValidFileExtension(fileName)) {
                callback.onError("Invalid file extension. Only PDF, JPEG, and PNG files are allowed.");
                return;
            }
            
            // Step 4: Read file content (now safe since we've validated size)
            InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
            if (inputStream == null) {
                callback.onError("Could not read file");
                return;
            }
            
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[8192];
            int nRead;
            long totalRead = 0;
            
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                totalRead += nRead;
                // Double-check size during read (in case size check was wrong)
                if (totalRead > MAX_FILE_SIZE) {
                    inputStream.close();
                    buffer.close();
                    callback.onError("File size exceeds 5MB limit during upload.");
                    return;
                }
                buffer.write(data, 0, nRead);
            }
            byte[] fileBytes = buffer.toByteArray();
            inputStream.close();
            buffer.close();
            
            // Get file name and extension
            String fileName = getFileName(context, fileUri);
            if (fileName == null || fileName.isEmpty()) {
                // Generate unique file name
                String extension = getFileExtension(context, fileUri);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
                fileName = "resume_" + userId + "_" + sdf.format(new Date()) + extension;
            }
            
            // Upload to Supabase Storage
            uploadToSupabase(context, fileBytes, fileName, callback);
            
        } catch (Exception e) {
            e.printStackTrace();
            callback.onError("Error reading file: " + e.getMessage());
        }
    }
    
    private static void uploadToSupabase(Context context, byte[] fileBytes, String fileName, UploadCallback callback) {
        // Supabase Storage API endpoint
        // IMPORTANT: You need to:
        // 1. Create a storage bucket named "resumes" in your Supabase dashboard
        // 2. Set bucket to public or configure RLS policies
        // 3. Update bucketName below if you use a different name
        String bucketName = "resumes"; // Change this to your actual bucket name
        
        // URL encode the file name to handle special characters
        try {
            fileName = java.net.URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        String uploadUrl = SupabaseConfig.SUPABASE_URL + "/storage/v1/object/" + bucketName + "/" + fileName;
        
        StringRequest request = new StringRequest(
                Request.Method.POST,
                uploadUrl,
                response -> {
                    // Success - construct public URL
                    String publicUrl = SupabaseConfig.SUPABASE_URL + "/storage/v1/object/public/" + bucketName + "/" + fileName;
                    callback.onSuccess(publicUrl);
                },
                error -> {
                    String errorMsg = "Upload failed";
                    if (error.networkResponse != null) {
                        errorMsg += " - Status: " + error.networkResponse.statusCode;
                        if (error.networkResponse.data != null) {
                            try {
                                String errorBody = new String(error.networkResponse.data, "UTF-8");
                                errorMsg += " - " + errorBody;
                                Log.e("FileUpload", "Upload Error: " + errorBody);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    callback.onError(errorMsg);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("apikey", SupabaseConfig.SUPABASE_KEY);
                headers.put("Authorization", "Bearer " + SupabaseConfig.SUPABASE_KEY);
                headers.put("Content-Type", "application/octet-stream");
                headers.put("x-upsert", "true"); // Overwrite if exists
                return headers;
            }
            
            @Override
            public byte[] getBody() throws AuthFailureError {
                return fileBytes;
            }
        };
        
        ApiClient.getRequestQueue(context).add(request);
    }
    
    private static String getFileName(Context context, Uri uri) {
        String fileName = null;
        String scheme = uri.getScheme();
        
        if (scheme != null && scheme.equals("content")) {
            android.database.Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex);
                }
                cursor.close();
            }
        }
        
        if (fileName == null) {
            String path = uri.getPath();
            if (path != null) {
                int cut = path.lastIndexOf('/');
                if (cut != -1) {
                    fileName = path.substring(cut + 1);
                }
            }
        }
        
        return fileName;
    }
    
    private static String getFileExtension(Context context, Uri uri) {
        String fileName = getFileName(context, uri);
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf("."));
        }
        
        // Try to get from MIME type
        String mimeType = context.getContentResolver().getType(uri);
        if (mimeType != null) {
            if (mimeType.equals("application/pdf")) {
                return ".pdf";
            } else if (mimeType.equals("image/jpeg")) {
                return ".jpg";
            } else if (mimeType.equals("image/png")) {
                return ".png";
            }
        }
        
        return ".pdf"; // Default
    }
    
    /**
     * Validates if the MIME type is allowed
     */
    private static boolean isValidMimeType(String mimeType) {
        if (mimeType == null || mimeType.isEmpty()) {
            return false;
        }
        for (String allowed : ALLOWED_MIME_TYPES) {
            if (mimeType.equals(allowed)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Validates if the file extension is allowed
     */
    private static boolean isValidFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }
        String lowerFileName = fileName.toLowerCase();
        for (String ext : ALLOWED_EXTENSIONS) {
            if (lowerFileName.endsWith(ext.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Gets file size from URI
     * Handles different URI schemes (content://, file://, etc.)
     */
    private static long getFileSize(Context context, Uri uri) {
        try {
            String scheme = uri.getScheme();
            
            if (scheme != null && scheme.equals("content")) {
                // Content URI - use ContentResolver
                android.database.Cursor cursor = context.getContentResolver().query(
                    uri, 
                    new String[]{android.provider.OpenableColumns.SIZE}, 
                    null, null, null
                );
                if (cursor != null && cursor.moveToFirst()) {
                    int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                    if (sizeIndex != -1) {
                        long size = cursor.getLong(sizeIndex);
                        cursor.close();
                        return size;
                    }
                    cursor.close();
                }
            } else if (scheme != null && scheme.equals("file")) {
                // File URI - use File
                String path = uri.getPath();
                if (path != null) {
                    java.io.File file = new java.io.File(path);
                    if (file.exists()) {
                        return file.length();
                    }
                }
            }
            
            // Fallback: For some providers (like Google Drive), we may need to read to get size
            // But we'll limit this to avoid reading huge files
            try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                if (is != null) {
                    long size = 0;
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    // Only read up to MAX_FILE_SIZE + 1MB buffer to check size
                    long maxCheckSize = MAX_FILE_SIZE + (1024 * 1024);
                    while ((bytesRead = is.read(buffer)) != -1) {
                        size += bytesRead;
                        // If we've read more than max allowed, stop and return error size
                        if (size > maxCheckSize) {
                            return size; // Will trigger size error
                        }
                    }
                    return size;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return -1; // Unknown size
    }
}

