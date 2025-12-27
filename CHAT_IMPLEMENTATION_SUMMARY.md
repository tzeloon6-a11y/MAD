# Chat Feature Implementation Summary

## Overview
Complete chat system implementation for CampusLink Jobs app with support for Student-Recruiter communication through job applications.

---

## 📁 Data Models

### 1. **ChatModel.java**
- **Purpose**: Represents a chat conversation between Student and Recruiter
- **Fields**:
  - `chatId` (String)
  - `jobId` (String)
  - `studentId` (String)
  - `recruiterId` (String)
  - `jobTitle` (String)
  - `lastMessage` (String)
  - `timestamp` (String)

### 2. **MessageModel.java**
- **Purpose**: Represents individual messages within a chat
- **Fields**:
  - `messageId` (String)
  - `senderId` (String)
  - `text` (String)
  - `timestamp` (String)

### 3. **ApplicationModel.java**
- **Purpose**: Represents job applications from Students
- **Fields**:
  - `applicationId` (String)
  - `studentId` (String)
  - `studentName` (String) - **Optimized**: Saved directly to avoid double queries
  - `recruiterId` (String)
  - `jobId` (String)
  - `status` (String) - Values: "PENDING", "ACCEPTED"
  - `initialMessage` (String) - Student's pitch
  - `timestamp` (String)

---

## 🎨 XML Layouts

### Chat List Views
1. **activity_chat_list.xml**
   - Main activity layout for viewing all chats
   - Contains RecyclerView for chat list
   - Header with "Messages" title

2. **item_chat_list.xml**
   - Single row item for chat list
   - Displays: Other user's name, Job title, Last message snippet, Timestamp
   - CardView with clickable design

### Chat Detail Views
3. **activity_chat_detail.xml**
   - Main chat screen layout
   - RecyclerView for messages (middle section)
   - Input area at bottom with EditText and Send button
   - Header toolbar

4. **item_message_left.xml**
   - Message bubble for messages from other person
   - Left-aligned, gray background (#E0E0E0)
   - Rounded corners (CardView)
   - Shows message text and timestamp

5. **item_message_right.xml**
   - Message bubble for user's own messages
   - Right-aligned, blue background (app_blue)
   - Rounded corners (CardView)
   - White text for contrast

### Application Views
6. **item_applicant.xml**
   - Layout for applicant list items (Recruiter view)
   - Shows: Student name, Initial message (pitch), "Start Chat" button
   - CardView design

---

## 🏗️ Activities

### 1. **ChatListActivity.java**
- **Purpose**: Displays list of all active conversations
- **Features**:
  - Queries Supabase `chats` table
  - Filters chats where current user is participant (student OR recruiter)
  - Uses OR filter: `(student_id=eq.userId OR recruiter_id=eq.userId)`
  - Orders by timestamp (descending)
  - Click on chat item → Opens ChatDetailActivity with chatId
  - Refreshes on `onResume()`

### 2. **ChatDetailActivity.java**
- **Purpose**: Real-time messaging screen
- **Features**:
  - Receives `chatId` from Intent
  - Loads messages from Supabase `messages` table filtered by `chat_id`
  - **Real-time polling**: Polls every 2 seconds for new messages
  - **Send message**: POSTs new messages to `messages` table
  - **Updates chat**: PATCHes parent Chat document (`last_message`, `timestamp`)
  - Auto-scrolls to bottom when new messages arrive
  - Cleans up polling handlers on destroy
  - MessageAdapter chooses left/right layout based on `senderId`

### 3. **JobDetailActivity.java** (Enhanced)
- **Purpose**: Job details view with role-based functionality
- **Student View**:
  - Shows "Interested" button
  - Duplicate check before applying
  - Dialog for pitch input
  - Saves application with student name
- **Recruiter View**:
  - Shows applicants list (RecyclerView)
  - Filters applications by `jobId` and `status='PENDING'`
  - Displays student name and initial message
  - "Start Chat" button for each applicant

---

## 🔌 Adapters

### 1. **ChatListAdapter.java**
- **Purpose**: Binds ChatModel data to RecyclerView
- **Features**:
  - Displays job title, last message (truncated), timestamp
  - Determines other user's name (Student/Recruiter) based on current user ID
  - Handles item clicks to navigate to ChatDetailActivity
  - Formats timestamps

### 2. **MessageAdapter.java**
- **Purpose**: Binds MessageModel data to message RecyclerView
- **Features**:
  - **View Type Selection**: 
    - `VIEW_TYPE_MESSAGE_RIGHT` if `senderId == currentUserId` (my messages)
    - `VIEW_TYPE_MESSAGE_LEFT` if `senderId != currentUserId` (other person's messages)
  - Uses `item_message_right.xml` for own messages
  - Uses `item_message_left.xml` for other person's messages
  - Formats timestamps (ISO → HH:MM)

### 3. **ApplicantAdapter.java**
- **Purpose**: Binds ApplicationModel data to applicants RecyclerView
- **Features**:
  - Displays student name and initial message
  - "Start Chat" button with complete business logic
  - **Critical Method**: `startChat()` - Handles chat creation workflow

---

## 🛠️ Helper Classes

### **ChatHelper.java**
- **Purpose**: Utility class for chat-related operations
- **Key Method**: `startChatWithStudent()`
  - Creates new Chat record
  - Adds initial message to Messages collection
  - Navigates to ChatDetailActivity
  - **Duplicate Prevention**: Checks if chat already exists before creating

---

## 🔄 Business Logic Flow

### Student Application Flow
1. Student views job → JobDetailActivity
2. Clicks "Interested" button
3. **Duplicate Check**: Queries Applications table (`student_id + job_id`)
4. If duplicate → Toast + disable button
5. If new → Show dialog for pitch
6. Save Application with:
   - `student_id`, `student_name` (optimized)
   - `recruiter_id`, `job_id`
   - `status`: "PENDING"
   - `initial_message`: pitch text
   - `timestamp`

### Recruiter Chat Creation Flow
1. Recruiter views job → Sees applicants list
2. Clicks "Start Chat" on applicant
3. **Check Existing Chat**: Queries Chats table by `application_id`
4. If exists → Navigate to existing chat
5. If new → Create Chat document:
   - `student_id`, `recruiter_id`, `job_id`
   - `job_title`, `application_id`
   - `last_message`: "Chat started"
   - `timestamp`
6. **Transfer Message**: Move `initial_message` from Application to Messages collection
7. **Update Status**: Change Application `status` to "ACCEPTED"
8. Navigate to ChatDetailActivity with new `chatId`

### Real-time Messaging Flow
1. User opens ChatDetailActivity
2. Loads messages from `messages` table (filtered by `chat_id`)
3. Polling starts (every 2 seconds)
4. User sends message:
   - POST to `messages` table
   - PATCH Chat document (`last_message`, `timestamp`)
   - Reload messages
5. Messages display with left/right layout based on `senderId`

---

## 🔒 Business Rules Implemented

### ✅ Duplicate Prevention
- **Applications**: Checks before saving (Student can't apply twice)
- **Chats**: Checks before creating (Prevents duplicate chats for same application)

### ✅ Data Consistency
- Application status updated to "ACCEPTED" when chat is created
- Chat's `last_message` and `timestamp` updated on each new message
- Student name saved directly in Applications (avoids double queries)

### ✅ Privacy & Security
- Chat list query filters by current user participation
- Only shows chats where user is student OR recruiter
- Uses OR filter: `(student_id=eq.userId OR recruiter_id=eq.userId)`

### ✅ Role-Based UI
- Students see "Interested" button on jobs
- Recruiters see applicants list with "Start Chat" buttons
- Different views based on user role

---

## 📊 Database Schema (Supabase Tables)

### `chats` Table
- `chat_id` (Primary Key)
- `student_id`
- `recruiter_id`
- `job_id`
- `job_title`
- `application_id` (Links to Applications)
- `last_message`
- `timestamp`

### `messages` Table
- `message_id` (Primary Key)
- `chat_id` (Foreign Key → chats)
- `sender_id`
- `text`
- `timestamp`

### `applications` Table
- `application_id` (Primary Key)
- `student_id`
- `student_name` (Optimized: saved directly)
- `recruiter_id`
- `job_id`
- `status` ("PENDING" | "ACCEPTED")
- `initial_message`
- `timestamp`

---

## 🎯 Key Features

1. **Real-time Updates**: Polling mechanism (2-second intervals)
2. **Message Differentiation**: Visual distinction between sent/received messages
3. **Application-to-Chat Bridge**: Seamless transition from application to chat
4. **Optimized Queries**: Student name saved directly to avoid joins
5. **Error Handling**: Graceful error handling throughout
6. **UI Feedback**: Button states, toasts, loading indicators

---

## 📝 Files Created/Modified

### Created Files:
- `ChatModel.java`
- `MessageModel.java`
- `ApplicationModel.java`
- `ChatListActivity.java`
- `ChatDetailActivity.java`
- `ChatListAdapter.java`
- `MessageAdapter.java`
- `ApplicantAdapter.java`
- `ChatHelper.java`
- `activity_chat_list.xml`
- `item_chat_list.xml`
- `activity_chat_detail.xml`
- `item_message_left.xml`
- `item_message_right.xml`
- `item_applicant.xml`

### Modified Files:
- `JobDetailActivity.java` (Added recruiter view and applicant handling)
- `activity_job_detail.xml` (Added RecyclerView for applicants)
- `AndroidManifest.xml` (Registered new activities)

---

## 🚀 Usage Examples

### Navigate to Chat List
```java
Intent intent = new Intent(context, ChatListActivity.class);
startActivity(intent);
```

### Navigate to Chat Detail
```java
Intent intent = new Intent(context, ChatDetailActivity.class);
intent.putExtra("chatId", chatId);
startActivity(intent);
```

### Start Chat from Application
```java
ApplicantAdapter.startChat(context, application, jobTitle, recruiterId);
```

### Student Applies to Job
```java
// In JobDetailActivity - handled automatically when "Interested" clicked
// Creates Application with status "PENDING"
```

---

## ✅ Testing Checklist

- [ ] Student can apply to job (duplicate prevention works)
- [ ] Recruiter sees applicants list
- [ ] "Start Chat" creates chat and transfers message
- [ ] Application status updates to "ACCEPTED"
- [ ] Chat list shows only user's chats
- [ ] Messages display correctly (left/right)
- [ ] Real-time polling works
- [ ] Send message updates chat document
- [ ] Navigation flows work correctly

---

## 📌 Notes

- Uses Supabase REST API (not Firestore) with Volley
- All timestamps in ISO format: `yyyy-MM-dd'T'HH:mm:ss`
- Polling interval: 2 seconds (configurable)
- Message truncation: 50 chars for chat list, 100 chars for applicants
- Error handling: Silent failures during polling to avoid toast spam

---

**Last Updated**: Complete implementation with all business rules enforced.

