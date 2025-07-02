# Firebase Setup Guide

## Current Issue: PERMISSION_DENIED Error

The error "PERMISSION_DENIED: Missing or insufficient data" occurs because your Firestore security rules are too restrictive or not properly configured.

## Steps to Fix:

### 1. Update Firestore Security Rules

1. Go to your Firebase Console: https://console.firebase.google.com/
2. Select your project
3. Go to **Firestore Database** → **Rules**
4. Replace the existing rules with the following:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users collection - users can read/write their own data
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Fabrics collection - authenticated users can read, write if they have proper role
    match /fabrics/{fabricId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && 
        (get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role in ['admin', 'manager']);
    }
    
    // Cutting collection - authenticated users can read, write if they have proper role
    match /cutting/{cuttingId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && 
        (get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role in ['admin', 'manager', 'operator']);
    }
    
    // Lots collection - authenticated users can read, write if they have proper role
    match /lots/{lotId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && 
        (get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role in ['admin', 'manager', 'operator']);
    }
  }
}
```

5. Click **Publish**

### 2. Verify Firebase Configuration

1. Make sure your `google-services.json` file is up to date
2. Verify that Authentication is enabled in Firebase Console
3. Check that Firestore Database is created and enabled

### 3. Test Authentication

The app now includes improved error handling that will:
- Create a user document automatically if it doesn't exist
- Provide detailed logging for debugging
- Handle permission errors gracefully

### 4. Alternative: Temporary Open Rules (For Testing Only)

If you want to test quickly, you can temporarily use open rules (NOT recommended for production):

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if true;
    }
  }
}
```

**⚠️ WARNING: Only use this for testing. Never deploy with open rules in production!**

### 5. Check Logs

After updating the rules, check the Android Studio Logcat for detailed error messages. The improved code will show:
- Whether the user document exists
- Detailed error information
- Authentication flow status

## Common Issues and Solutions:

1. **Rules not published**: Make sure to click "Publish" after updating rules
2. **Wrong project**: Verify you're using the correct Firebase project
3. **Authentication not enabled**: Enable Email/Password authentication in Firebase Console
4. **Firestore not created**: Create the Firestore database if it doesn't exist

## Testing the Fix:

1. Update the security rules as shown above
2. Clean and rebuild your project
3. Try signing in again
4. Check Logcat for detailed logs

The app should now work properly with the updated security rules and improved error handling. 