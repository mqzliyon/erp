package com.dazzling.erp.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.dazzling.erp.MainActivity;
import com.dazzling.erp.R;
import com.dazzling.erp.models.User;
import com.dazzling.erp.services.FirebaseAuthService;
import com.google.android.material.button.MaterialButton;

public class ProfileFragment extends Fragment {
    private TextView nameText, emailText, roleText;
    private MaterialButton editProfileBtn, changePasswordBtn, logoutBtn;
    private User currentUser;
    private FirebaseAuthService authService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        nameText = view.findViewById(R.id.profile_name);
        emailText = view.findViewById(R.id.profile_email);
        roleText = view.findViewById(R.id.profile_role);
        editProfileBtn = view.findViewById(R.id.btn_edit_profile);
        changePasswordBtn = view.findViewById(R.id.btn_change_password);
        logoutBtn = view.findViewById(R.id.btn_logout);

        authService = new FirebaseAuthService();
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            currentUser = activity.getCurrentUser();
        }
        updateProfileUI();
        setupListeners();
        return view;
    }

    private void updateProfileUI() {
        if (currentUser != null) {
            nameText.setText(currentUser.getDisplayName());
            emailText.setText(currentUser.getEmail());
            roleText.setText(currentUser.getRole());
        }
    }

    private void setupListeners() {
        editProfileBtn.setOnClickListener(v -> showEditProfileDialog());
        changePasswordBtn.setOnClickListener(v -> showChangePasswordDialog());
        logoutBtn.setOnClickListener(v -> {
            authService.signOut();
            if (getActivity() != null) {
                Intent intent = new Intent(getActivity(), com.dazzling.erp.ui.auth.LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                getActivity().finish();
            }
        });
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Edit Name");
        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(currentUser != null ? currentUser.getDisplayName() : "");
        builder.setView(input);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty() && currentUser != null) {
                // Update in Firestore
                authService.updateUserName(currentUser.getUid(), newName, success -> {
                    if (success) {
                        currentUser.setDisplayName(newName);
                        updateProfileUI();
                        Toast.makeText(getContext(), "Name updated", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Failed to update name", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Change Password");
        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("New Password");
        builder.setView(input);
        builder.setPositiveButton("Change", (dialog, which) -> {
            String newPassword = input.getText().toString().trim();
            if (!newPassword.isEmpty() && newPassword.length() >= 6 && currentUser != null) {
                authService.changePassword(currentUser.getEmail(), newPassword, success -> {
                    if (success) {
                        Toast.makeText(getContext(), "Password changed", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Failed to change password", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(getContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
} 