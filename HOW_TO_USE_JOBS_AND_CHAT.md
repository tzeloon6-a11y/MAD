# How to Create Jobs and Apply - User Guide

## 📋 Complete Workflow

### For RECRUITERS: Creating a Job Post

1. **Login as Recruiter**
   - Open the app
   - Login with recruiter credentials

2. **Navigate to Post Tab**
   - Tap the "Post" tab in bottom navigation (middle icon)

3. **Create Job Post**
   - You'll see radio buttons: "Job Post" and "Experience Post"
   - Select **"Job Post"** radio button
   - Enter:
     - **Title**: Job title (e.g., "Software Developer")
     - **Content**: Job description/requirements
     - **Media** (Optional): Add image/video
   - Tap **"Submit"** button

4. **Job Created!**
   - Job appears in Home tab for students to see
   - Students can now apply to your job

5. **View Applicants**
   - Go to Home tab
   - Tap on your own job post
   - You'll see list of applicants who applied
   - Each applicant shows:
     - Student name
     - Their pitch/initial message
     - "Start Chat" button

6. **Start Chat with Applicant**
   - Tap "Start Chat" on an applicant
   - This creates a chat room
   - Moves student's pitch into the chat
   - Updates application status to "ACCEPTED"
   - Opens chat screen for messaging

---

### For STUDENTS: Applying to Jobs

1. **Login as Student**
   - Open the app
   - Login with student credentials

2. **Browse Jobs**
   - Go to **Home** tab
   - See all available job posts
   - Each job shows:
     - Job title
     - Description
     - Recruiter name
     - Posted date

3. **View Job Details**
   - **Tap on any job post** to see full details
   - Opens JobDetailActivity with complete information

4. **Apply to Job**
   - On job details screen, tap **"Interested"** button
   - Enter your pitch/message (why you're interested)
   - Tap **"Submit"**
   - Application is saved with status "PENDING"

5. **Wait for Recruiter Response**
   - Recruiter reviews your application
   - If they click "Start Chat", you'll get a chat room
   - You can then message back and forth

6. **View Your Chats**
   - Go to **Chat** tab
   - See all your active conversations
   - Tap any chat to open and send messages

---

## 🔄 Complete Flow Diagram

```
RECRUITER SIDE:
1. Post Tab → Create Job Post → Submit
2. Home Tab → See your job → Tap it
3. View Applicants → Tap "Start Chat"
4. Chat created → Message with student

STUDENT SIDE:
1. Home Tab → Browse jobs → Tap a job
2. Job Details → Tap "Interested"
3. Enter pitch → Submit application
4. Wait for recruiter → Chat tab appears when accepted
5. Chat Tab → Open chat → Send messages
```

---

## 🎯 Key Features

### Job Creation (Recruiter)
- ✅ Create job posts from Post tab
- ✅ Select "Job Post" radio button
- ✅ Add title, description, optional media
- ✅ Jobs appear in Home tab

### Job Application (Student)
- ✅ Browse jobs in Home tab
- ✅ Click job to see details
- ✅ Apply with "Interested" button
- ✅ Enter pitch message
- ✅ Duplicate prevention (can't apply twice)

### Chat Creation (Recruiter)
- ✅ View applicants on job details page
- ✅ Click "Start Chat" to create chat
- ✅ Student's pitch moved to chat
- ✅ Application status → "ACCEPTED"

### Messaging (Both)
- ✅ Real-time chat with polling
- ✅ Send/receive messages
- ✅ Visual distinction (left/right bubbles)
- ✅ Auto-scroll to latest message

---

## ⚠️ Important Notes

1. **Job Posts vs Experience Posts**
   - Recruiters can create both
   - Students can only create Experience Posts
   - Make sure to select correct radio button

2. **Application Status**
   - "PENDING": Student applied, waiting for recruiter
   - "ACCEPTED": Recruiter started chat

3. **Chat Creation**
   - Only happens when recruiter clicks "Start Chat"
   - Student cannot initiate chat
   - Chat is created from Application

4. **Database Tables Needed**
   - `job_posts` - Stores job listings
   - `applications` - Stores student applications
   - `chats` - Stores chat conversations
   - `messages` - Stores individual messages

---

## 🐛 Troubleshooting

### Can't see jobs in Home tab?
- Check if `job_posts` table exists in Supabase
- Verify you're logged in
- Check internet connection

### Can't apply to job?
- Make sure you're logged in as Student
- Check if you already applied (button will say "Applied")
- Verify `applications` table exists

### Can't create job?
- Make sure you're logged in as Recruiter
- Select "Job Post" radio button (not Experience Post)
- Check `job_posts` table exists in Supabase

### Can't see applicants?
- Make sure you're the recruiter who posted the job
- Check if any students have applied
- Verify `applications` table exists

---

## 📝 Quick Reference

**Create Job**: Post Tab → Job Post → Fill form → Submit

**Apply to Job**: Home Tab → Tap job → Interested → Enter pitch → Submit

**Start Chat**: Home Tab → Your job → Applicants → Start Chat

**Send Message**: Chat Tab → Tap chat → Type message → Send

