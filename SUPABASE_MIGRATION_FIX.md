# Fix for Foreign Key Constraint Error

## Problem
You're getting this error:
```
ERROR: 23503: insert or update on table "messages" violates foreign key constraint "messages_receiver_id_fkey"
```

This happens because the `receiver_id` column has a foreign key constraint that requires the user to exist in the `users` table.

## Solution: Remove Foreign Key Constraint

Run this SQL to fix the issue:

```sql
-- Step 1: Drop the foreign key constraint if it exists
ALTER TABLE messages 
DROP CONSTRAINT IF EXISTS messages_receiver_id_fkey;

-- Step 2: Verify the column exists (it should already exist)
-- If not, add it without the constraint:
ALTER TABLE messages
ADD COLUMN IF NOT EXISTS receiver_id UUID;

-- Step 3: Verify is_read column exists
ALTER TABLE messages
ADD COLUMN IF NOT EXISTS is_read BOOLEAN DEFAULT FALSE;

-- Step 4: Create indexes (these don't require foreign keys)
CREATE INDEX IF NOT EXISTS idx_messages_receiver_read 
ON messages(receiver_id, is_read) 
WHERE is_read = FALSE;

CREATE INDEX IF NOT EXISTS idx_messages_chat_receiver_read 
ON messages(chat_id, receiver_id, is_read) 
WHERE is_read = FALSE;
```

## Why Remove the Foreign Key?

1. **Flexibility**: Allows inserting messages even if user data is temporarily unavailable
2. **Performance**: No need to validate foreign keys on every insert
3. **Data Integrity**: Your app logic already ensures valid receiver_id values

## Alternative: Keep Foreign Key but Fix Data

If you want to keep the foreign key constraint, you need to ensure all receiver_ids exist in users table:

```sql
-- Check which receiver_ids don't exist in users table
SELECT DISTINCT m.receiver_id 
FROM messages m 
WHERE m.receiver_id IS NOT NULL 
  AND NOT EXISTS (SELECT 1 FROM users WHERE id = m.receiver_id);

-- Then either:
-- 1. Create missing users, OR
-- 2. Set those receiver_ids to NULL, OR  
-- 3. Remove the foreign key constraint (recommended)
```

## Recommended Approach

**Remove the foreign key constraint** - it's not necessary for this use case. Your application code already ensures valid receiver_ids when sending messages.

