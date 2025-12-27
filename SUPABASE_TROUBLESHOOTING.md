# Supabase Connection Troubleshooting Guide

## Current Issue: "Failed to load chats"

### Possible Causes:

1. **Table doesn't exist in Supabase**
   - Check if `chats` table exists in your Supabase database
   - Table name might be different (e.g., `chat`, `Chat`, `chat_rooms`)

2. **Column names don't match**
   - Expected columns: `chat_id`, `student_id`, `recruiter_id`, `job_id`, `job_title`, `last_message`, `timestamp`
   - Your table might use different names (e.g., `id` instead of `chat_id`)

3. **OR query syntax issue**
   - Supabase PostgREST might need different syntax
   - Try simpler queries first

4. **Network/Connection issue**
   - Check internet connection
   - Verify Supabase URL is correct: `https://spctlmijztfwmfnuqqta.supabase.co`
   - Check if API key is valid

5. **RLS (Row Level Security) policies**
   - Supabase might have RLS enabled blocking queries
   - Check RLS policies in Supabase dashboard

## How to Debug:

### Step 1: Check Logcat
When you run the app, check Logcat for:
- `ChatFragment: Loading chats from: [URL]`
- `ChatFragment: Supabase Error: [error details]`
- `ChatFragment: Network Error: [error details]`

### Step 2: Test Basic Connection
Try querying a simpler table first (like `users` or `job_posts`) to verify connection works.

### Step 3: Verify Table Structure
In Supabase Dashboard:
1. Go to Table Editor
2. Check if `chats` table exists
3. Verify column names match:
   - `chat_id` (or `id`)
   - `student_id`
   - `recruiter_id`
   - `job_id`
   - `job_title`
   - `last_message`
   - `timestamp`

### Step 4: Test Query Directly
Try this query in Supabase SQL Editor:
```sql
SELECT * FROM chats 
WHERE student_id = 'your-user-id' 
   OR recruiter_id = 'your-user-id'
ORDER BY timestamp DESC;
```

### Step 5: Check RLS Policies
1. Go to Authentication > Policies
2. Check if `chats` table has RLS enabled
3. If enabled, create a policy that allows SELECT for authenticated users

## Quick Fixes:

### Fix 1: If table name is different
Update in `ChatFragment.java`:
```java
String url = SupabaseConfig.SUPABASE_URL
        + "/rest/v1/YOUR_TABLE_NAME?select=*"
```

### Fix 2: If column names are different
Update the parsing code in `ChatFragment.java`:
```java
String chatId = obj.optString("id", ""); // if column is "id" not "chat_id"
```

### Fix 3: If OR query doesn't work
Use two separate queries and merge:
```java
// Query 1: student chats
// Query 2: recruiter chats
// Merge results
```

### Fix 4: Disable RLS temporarily (for testing)
In Supabase Dashboard:
1. Go to Table Editor > chats
2. Click on "RLS" tab
3. Temporarily disable RLS to test

## Example Chat Issue:

Example chats have IDs starting with "example-" and cannot send messages because they don't exist in the database. This is expected behavior - they're just for UI demonstration.

To create real chats:
1. Student applies to a job (creates Application)
2. Recruiter clicks "Start Chat" on the application
3. This creates a real chat in the database

## Next Steps:

1. Check Logcat output when running the app
2. Verify table exists in Supabase
3. Test with a simpler query first
4. Check RLS policies
5. Verify column names match

