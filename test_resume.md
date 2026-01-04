# Mock Resume for Testing

## Option 1: Create a Simple PDF Resume

You can create a simple PDF resume using one of these methods:

### Method A: Using Online Tools
1. Go to https://www.canva.com/create/resumes/ (free)
2. Create a simple resume with:
   - Name: "Test Student"
   - Email: "test@example.com"
   - Phone: "+1234567890"
   - Education: "Test University"
   - Skills: "Java, Android Development"
3. Download as PDF
4. Transfer to your device

### Method B: Using Google Docs
1. Open Google Docs
2. Create a new document
3. Add basic resume content:
   - Name, Contact Info
   - Education
   - Experience
   - Skills
4. File → Download → PDF
5. Transfer to your device

### Method C: Create a Simple Text File and Convert
Create a simple text file and use an online converter or print-to-PDF

## Option 2: Use an Image (JPEG/PNG)

Take a screenshot or create a simple image:
1. Create a simple resume image using any image editor
2. Save as JPEG or PNG
3. Transfer to device

## Transferring to Device

### Method 1: ADB Push (Recommended for Testing)
```bash
# Connect your device via USB with USB debugging enabled
adb devices  # Verify device is connected

# Push the file to Downloads folder
adb push test_resume.pdf /sdcard/Download/test_resume.pdf

# Or push to Documents
adb push test_resume.pdf /sdcard/Documents/test_resume.pdf
```

### Method 2: Email/Cloud Storage
1. Email the PDF to yourself
2. Open email on device
3. Download attachment
4. File will be in Downloads folder

### Method 3: USB File Transfer
1. Connect device via USB
2. Enable File Transfer mode
3. Copy PDF to device's Download or Documents folder

### Method 4: Google Drive/Dropbox
1. Upload PDF to Google Drive or Dropbox
2. Open on device
3. Download to device

## Quick Test PDF Creation Script

I'll create a simple script to help you generate a test PDF.

