# Authentication & Account Management Flows

Our application ensures maximum security and a seamless user experience. We use an OTP-based verification system along with strict Row Level Security (RLS) to protect user data. Below are the visual workflows for our authentication and account management systems.

---

# New User - Sign Up Flow
The registration process requires email verification before creating an account to prevent fake profiles and spam.
*   **Flow:** Enter Email ➔ Receive OTP ➔ Verify OTP ➔ Create Password ➔ Enter Profile Details (Name, DOB) ➔ Save to Database.

<img width="1452" height="636" alt="WhatsApp Image 2026-10-05 at 1 24 37 AM" src="https://github.com/user-attachments/assets/77c9a5ef-40c1-4e1f-8c82-3f049f16c612" />

---

# Existing User - Login Flow
A secure and quick login process that verifies user existence before proceeding to password validation.
*   **Flow:** Enter Email ➔ Database Check (Exists?) ➔ If Yes, Enter Password ➔ Login Successful ➔ Navigate to Dashboard.
*   *If the email is not registered, the user is prompted to sign up.*

<img width="1345" height="695" alt="WhatsApp Image 2026-10-05 at 1 26 09 AM" src="https://github.com/user-attachments/assets/7937ae71-fc73-4c4e-95eb-8cd18c1ab46c" />


---

# Forgot Password Flow
Users can securely reset their passwords using an OTP sent to their registered email address.
*   **Flow:** Click Forgot Password ➔ Enter Registered Email ➔ Receive & Verify OTP ➔ Create New Password ➔ Securely Login.

<img width="1375" height="607" alt="WhatsApp Image 2026-10-05 at 1 26 47 AM" src="https://github.com/user-attachments/assets/9033695f-a724-439f-aca3-9755bcac497c" />

---

# Account Security & Management
Once logged in, users have complete control over their account security via the **Settings** panel. We implement an extra layer of security (DOB + OTP Verification) for sensitive actions to prevent unauthorized changes.

*   **Change Password:** Enter Date of Birth (DOB) ➔ Verify Database Match ➔ Receive & Enter OTP ➔ Securely Update Password.
*   **Delete Account:** Confirm Deletion Warning ➔ Request OTP ➔ Verify OTP ➔ Permanently Delete/Deactivate Data ➔ Invalidate Session Token.

<img width="1642" height="2203" alt="Account Security and Management Flows" src="https://github.com/user-attachments/assets/7efa0357-6956-4238-ac49-2b9165ddc6c5" />


---
