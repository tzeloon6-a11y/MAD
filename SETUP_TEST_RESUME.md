# How to Create and Add a Test Resume to Your Android Device

## Quick Method 1: Use the HTML File (Easiest)

1. **Open `test_resume.html` in your browser**
   - Double-click the file or open it in Chrome/Firefox

2. **Print to PDF**
   - Press `Ctrl+P` (Windows/Linux) or `Cmd+P` (Mac)
   - Select "Save as PDF" as the destination
   - Click "Save"
   - Name it `test_resume.pdf`

3. **Transfer to Device**
   - See transfer methods below

## Quick Method 2: Use Online Tools (No Installation)

1. **Go to Canva** (https://www.canva.com/create/resumes/)
   - Sign up for free account
   - Choose a resume template
   - Fill in with test data:
     - Name: Test Student
     - Email: test@example.com
     - Phone: +1234567890
     - Education: Test University
     - Skills: Java, Android Development
   - Click "Download" → Choose "PDF"
   - Save the file

2. **Transfer to Device** (see methods below)

## Quick Method 3: Create Simple Image Resume

1. **Create a simple image** using any image editor:
   - Paint (Windows)
   - Preview (Mac)
   - Any online image editor
   
2. **Add text:**
   ```
   TEST STUDENT RESUME
   
   Email: test@example.com
   Phone: +1234567890
   
   EDUCATION
   Test University - Computer Science
   
   SKILLS
   Java, Android Development
   ```

3. **Save as JPEG or PNG**

4. **Transfer to Device**

## Transfer Methods to Android Device

### Method A: ADB Push (Recommended for Developers)

1. **Enable USB Debugging on your Android device:**
   - Go to Settings → About Phone
   - Tap "Build Number" 7 times to enable Developer Options
   - Go to Settings → Developer Options
   - Enable "USB Debugging"

2. **Connect device via USB**

3. **Run these commands:**
   ```bash
   # Verify device is connected
   adb devices
   
   # Push PDF to Downloads folder
   adb push test_resume.pdf /sdcard/Download/test_resume.pdf
   
   # Or push to Documents
   adb push test_resume.pdf /sdcard/Documents/test_resume.pdf
   ```

4. **On your device:**
   - Open Files app
   - Go to Downloads or Documents
   - You'll see `test_resume.pdf`

### Method B: Email to Yourself

1. **Email the PDF to yourself**
   - Attach `test_resume.pdf` to an email
   - Send to your own email address

2. **On your Android device:**
   - Open Gmail or email app
   - Open the email
   - Tap the attachment
   - Tap "Download" or "Save"
   - File will be in Downloads folder

### Method C: Google Drive / Dropbox

1. **Upload PDF to cloud storage:**
   - Upload `test_resume.pdf` to Google Drive or Dropbox

2. **On your Android device:**
   - Open Google Drive or Dropbox app
   - Find the file
   - Tap "Download" or "Make Available Offline"
   - File will be in Downloads folder

### Method D: USB File Transfer

1. **Connect device via USB**

2. **Enable File Transfer mode:**
   - On device, when USB notification appears
   - Select "File Transfer" or "MTP"

3. **On your computer:**
   - Open File Explorer (Windows) or Finder (Mac)
   - Find your device
   - Navigate to Downloads or Documents folder
   - Copy `test_resume.pdf` to that folder

## Testing the Upload

Once the file is on your device:

1. **Open your app**
2. **Go to Registration screen**
3. **Fill in registration details**
4. **Tap "Upload Resume (PDF, JPEG, PNG)"**
5. **Navigate to Downloads or Documents folder**
6. **Select `test_resume.pdf`**
7. **Verify:**
   - Button shows "Uploading..."
   - Then shows "Resume Uploaded ✓"
   - Status message appears

## Alternative: Create Resume Directly on Device

You can also create a resume directly on your Android device:

1. **Use Google Docs app:**
   - Create new document
   - Add resume content
   - File → Download → PDF

2. **Use a PDF creator app:**
   - Install "PDF Creator" or similar from Play Store
   - Create resume
   - Save as PDF

3. **Take a screenshot:**
   - Create resume in any app
   - Take screenshot
   - Save as JPEG/PNG
   - Use that for testing

## Quick Test Checklist

- [ ] Resume file created (PDF, JPEG, or PNG)
- [ ] File transferred to device
- [ ] File visible in Downloads or Documents folder
- [ ] Can select file in file picker
- [ ] Upload works in registration
- [ ] Upload works in edit profile
- [ ] Resume URL saved in database

## Troubleshooting

**File not showing in file picker?**
- Make sure it's in Downloads, Documents, or Pictures folder
- Check file permissions
- Try using a different file manager

**Upload fails?**
- Check file size (must be < 5MB)
- Check file type (PDF, JPEG, or PNG only)
- Verify Supabase Storage bucket is configured
- Check network connection

**Need a smaller file for testing?**
- Use a simple text-based PDF
- Or use a small JPEG image (take a photo of text)

