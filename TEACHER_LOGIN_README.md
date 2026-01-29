# Teacher Login GUI Application

## Overview
This is a Tkinter-based Teacher Login form with validation and visual feedback capabilities.

## Features

### 1. Credential Validation
- Validates teacher ID and password against hardcoded credentials
- Credentials:
  - ID: `teacher123`
  - Password: `password123`

### 2. User Feedback
- **Success Message**: Displays "Login successful" in green when credentials are valid
- **Error Messages**:
  - "Please enter ID and Password" in red when fields are empty
  - "Invalid ID or Password" in red when credentials don't match

### 3. Visual Indicators
- **Tick Mark Canvas**: Shows real-time visual feedback on ID field
  - **Red Circle**: ID field is empty
  - **Green Circle**: ID field has content
  - Updates automatically as user types

### 4. UI Components
- Teacher ID entry field
- Password entry field (masked with asterisks)
- Login button with green background
- Message label for feedback
- Contact information footer

## Usage

### Running the Application
```bash
python3 teacher_login.py
```

### Testing Credentials
- **Valid Login**:
  - ID: `teacher123`
  - Password: `password123`

- **Invalid Login**: Any other combination will show an error

## Implementation Details

### Key Functions

1. **`login()`**: Validates credentials and provides feedback
   - Strips whitespace from inputs
   - Checks for empty fields
   - Compares against hardcoded valid credentials
   - Displays appropriate success/error messages

2. **`show_message(text, color)`**: Displays feedback messages
   - Updates the message label with text and color

3. **`update_tick_mark(event=None)`**: Updates visual indicator
   - Draws colored circles on canvas
   - Green when ID field has content
   - Red when ID field is empty

### Event Bindings
- `<KeyRelease>` on ID field triggers `update_tick_mark()`
- Login button click triggers `login()`

## Future Enhancements
- Replace hardcoded credentials with database/backend authentication
- Add password strength requirements
- Implement "Remember Me" functionality
- Add "Forgot Password" feature
- Implement session management
- Add logging for security audit trails

## Security Notes
- Current implementation uses simple string comparison
- Passwords are not hashed (suitable only for demonstration)
- For production use, integrate with proper authentication system
- Consider implementing rate limiting for login attempts
