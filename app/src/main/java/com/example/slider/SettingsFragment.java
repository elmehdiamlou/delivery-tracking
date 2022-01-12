package com.example.slider;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class SettingsFragment extends Fragment {

    TextInputLayout Email, Username, Role;
    Button btnSave, btnResetPassword, btnLogout;
    FirebaseAuth firebaseAuth;
    FirebaseFirestore firebaseFirestore;
    String UserID, UserUsername;
    EditText txtresetPassword;

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        Email = view.findViewById(R.id.txtshowemail);
        Username = view.findViewById(R.id.txteditusername);
        Role = view.findViewById(R.id.txtshowrole);
        btnSave = view.findViewById(R.id.btnSave);
        btnResetPassword = view.findViewById(R.id.btnResetPassword);
        btnLogout = view.findViewById(R.id.btnLogout);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        UserID = firebaseAuth.getCurrentUser().getUid();

        firebaseFirestore.collection(UserID)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()){
                            for (DocumentSnapshot documentSnapshot: task.getResult()){
                                Email.getEditText().setText(documentSnapshot.getString("Email"));
                                Username.getEditText().setText(documentSnapshot.getString("Username"));
                                Role.getEditText().setText(documentSnapshot.getString("Role"));
                            }
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("Settings",e.getMessage());
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!usernameValidation()){
                    return;
                } else {
                    UserUsername = Username.getEditText().getText().toString();
                    Map<String,Object> UserData = new HashMap<>();
                    UserData.put("Username", UserUsername);
                    firebaseFirestore.collection(UserID)
                            .document("userinfo")
                            .update(UserData)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful()){
                                        showToast(R.drawable.ic_update, "Username is updated successfully.");
                                    }else {
                                        Log.d("Settings",task.getException().getMessage());
                                    }
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d("Settings",e.getMessage());
                        }
                    });
                }
            }
        });

        final FirebaseUser user = firebaseAuth.getCurrentUser();

        btnResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                txtresetPassword = new EditText(view.getContext());
                final AlertDialog.Builder resetPasswordDialog = new AlertDialog.Builder(view.getContext());
                resetPasswordDialog.setTitle("Reset Password");
                resetPasswordDialog.setView(txtresetPassword);
                resetPasswordDialog.setPositiveButton(Html.fromHtml("<font color='"+getResources().getColor(R.color.green_2)+"'>Save</font>"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        if(passwordValidation()) {
                            String newPassword = txtresetPassword.getText().toString();
                            user.updatePassword(newPassword).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    showToast(R.drawable.ic_lock, "Password is updated successfully.");
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.d("ResetPassword",e.getMessage());
                                }
                            });
                        }
                    }
                });
                resetPasswordDialog.setNegativeButton(Html.fromHtml("<font color='"+getResources().getColor(R.color.orange_2)+"'>Cancel</font>"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        dialog.cancel();
                    }
                });
                resetPasswordDialog.create().show();
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(getContext(), MainActivity.class));
            }
        });

        return view;
    }

    boolean usernameValidation() {
        String username = Username.getEditText().getText().toString();
        if (username.isEmpty()) {
            Username.setError("Please enter a Username");
            return false;
        } else {
            Username.setError(null);
            return true;
        }
    }

    boolean passwordValidation() {
        String pass = txtresetPassword.getText().toString();
        if (pass.isEmpty()) {
            showToast(R.drawable.ic_lock, "Please enter a Password.");
            return false;
        } else if (pass.length() < 6 || pass.length() > 12) {
            showToast(R.drawable.ic_lock, "Password must have minimum 6 characters and maximum 12.");
            return false;
        } else {
            return true;
        }
    }

    public void showToast(int icon, String text) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.toast_layout, (ViewGroup) getView().findViewById(R.id.toast_root));
        Toast toast = new Toast(getContext());
        toast.setGravity(Gravity.TOP, 0, 0);
        ((ImageView) layout.findViewById(R.id.toast_image)).setImageDrawable(getResources().getDrawable(icon));
        ((TextView) layout.findViewById(R.id.toast_text)).setText(text);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.show();
    }
}