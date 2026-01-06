# Supabase Database Migration for Read Messages Tracking

## Overview
This migration adds `receiver_id` and `is_read` columns to the `messages` table to track which messages have been read by which users.

## SQL Migration Script

Run this in your Supabase SQL Editor:

```sql
-- Step 1: Add receiver_id column (nullable for existing messages)
ALTER TABLE messages
ADD COLUMN IF NOT EXISTS receiver_id UUID REFERENCES users(id);

-- Step 2: Add is_read column (default false for new messages)
ALTER TABLE messages
ADD COLUMN IF NOT EXISTS is_read BOOLEAN DEFAULT FALSE;

-- Step 3: Update existing messages to set receiver_id based on chat participants
-- This sets receiver_id for messages where it's currently NULL
UPDATE messages m
SET receiver_id = CASE
    WHEN m.sender_id = c.student_id THEN c.recruiter_id
    WHEN m.sender_id = c.recruiter_id THEN c.student_id
    ELSE NULL
END
FROM chats c
WHERE m.chat_id = c.chat_id
  AND m.receiver_id IS NULL;

-- Step 4: Set is_read to false for all existing messages (they haven't been read yet)
UPDATE messages
SET is_read = FALSE
WHERE is_read IS NULL;

-- Step 5: Create index for better query performance
CREATE INDEX IF NOT EXISTS idx_messages_receiver_read 
ON messages(receiver_id, is_read) 
WHERE is_read = FALSE;

CREATE INDEX IF NOT EXISTS idx_messages_chat_receiver_read 
ON messages(chat_id, receiver_id, is_read) 
WHERE is_read = FALSE;
```

## What This Does

1. **Adds `receiver_id` column**: Tracks who should read each message
2. **Adds `is_read` column**: Boolean flag indicating if the message has been read
3. **Backfills existing messages**: Sets `receiver_id` for old messages based on chat participants
4. **Creates indexes**: Improves query performance for unread message queries

## After Migration

The app will now:
- Set `receiver_id` and `is_read=false` when sending new messages
- Mark messages as read (`is_read=true`) when opening a chat
- Count unread chats (not total messages) for the badge
- Show red dots next to chats with unread messages

## Testing

After running the migration:
1. Send a message from one account
2. Check that `receiver_id` is set correctly in the database
3. Open the chat from the receiver account
4. Verify that `is_read` changes to `true` for those messages
5. Check that the badge shows the number of chats with unread, not total messages

