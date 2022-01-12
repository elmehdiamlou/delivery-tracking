package com.example.slider;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.auth.User;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegisterFragment extends Fragment {

    TextInputLayout Email, Username, Role, Password;
    String selectedRole = "";
    Button btnRegister;
    FirebaseAuth firebaseAuth;
    FirebaseFirestore firebaseFirestore;
    String UserID, UserEmail, UserUsername, UserRole, UserPassword;

    @Override
    public ViewGroup onCreateView(LayoutInflater inflater, ViewGroup container,
                                  Bundle savedInstanceState) {

        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.fragment_register, container, false);
        AutoCompleteTextView eT = (AutoCompleteTextView) view.findViewById(R.id.selectionrole);
        String[] rolesArray = new String[]{"Delivery", "Admin"};
        ArrayAdapter<String> aAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_dropdown_item_1line, rolesArray);
        eT.setAdapter(aAdapter);

        eT.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedRole = (String) parent.getItemAtPosition(position);
            }
        });

        Email = view.findViewById(R.id.txtemail);
        Username = view.findViewById(R.id.txtusername);
        Role = view.findViewById(R.id.txtrole);
        Password = view.findViewById(R.id.txtpassword);
        btnRegister = view.findViewById(R.id.btnregister);

        firebaseAuth = FirebaseAuth.getInstance();

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isNetworkAvailable(getContext())) {
                    showToast(R.drawable.ic_network_check, "Please verify your network.");
                } else {
                    if (!emailValidation() | !usernameValidation() | !roleValidation() | !passwordValidation()) {
                        return;
                    } else {
                        UserEmail = Email.getEditText().getText().toString();
                        UserUsername = Username.getEditText().getText().toString();
                        UserRole = selectedRole;
                        UserPassword = Password.getEditText().getText().toString();

                        firebaseAuth.createUserWithEmailAndPassword(UserEmail, UserPassword)
                                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                        if (task.isSuccessful()) {
                                            firebaseFirestore = FirebaseFirestore.getInstance();
                                            UserID = firebaseAuth.getCurrentUser().getUid();
                                            Map<String,Object> UserData = new HashMap<>();
                                            UserData.put("Email", UserEmail);
                                            UserData.put("Username", UserUsername);
                                            UserData.put("Role", UserRole);
                                            firebaseFirestore.collection(UserID)
                                                    .document("userinfo")
                                                    .set(UserData)
                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            if(!task.isSuccessful()){
                                                                Log.d("Storing",task.getException().getMessage());
                                                            }
                                                        }
                                                    }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.d("Storing",e.getMessage());
                                                }
                                            });

                                            firebaseAuth.getCurrentUser().sendEmailVerification()
                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            if (task.isSuccessful()) {
                                                                showToast(R.drawable.ic_info, "User is registered successfully.\nPlease check your inbox for verification email.");
                                                            }
                                                        }
                                                    }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.d("Registration", task.getException().getMessage());
                                                }
                                            });
                                        }
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Email.setError(e.getMessage());
                            }
                        });
                    }
                }
            }
        });

        return view;
    }

    boolean emailValidation() {
        String email = Email.getEditText().getText().toString();
        if (email.isEmpty()) {
            Email.setError("Please enter a Email");
            return false;
        } else {
            String regex = "^[A-Za-z0-9+_.-]+@(.+)$";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(email);
            if (matcher.matches()) {
                Email.setError(null);
                return true;
            } else {
                Email.setError("Please enter a valid Email");
                return false;
            }
        }
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

    boolean roleValidation() {
        if (selectedRole.isEmpty()) {
            Role.setError("Please select a Role");
            return false;
        } else {
            Role.setError(null);
            return true;
        }
    }

    boolean passwordValidation() {
        String pass = Password.getEditText().getText().toString();
        if (pass.isEmpty()) {
            Password.setError("Please enter a Password");
            return false;
        } else if (pass.length() < 6 || pass.length() > 12) {
            Password.setError("Password must have minimum 6 characters and maximum 12");
            return false;
        } else {
            Password.setError(null);
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

    boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivity == null) {
            return false;
        } else {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            for (NetworkInfo networkInfo : info) {
                if (networkInfo.getState() == NetworkInfo.State.CONNECTED) {
                    return true;
                }
            }
        }
        return false;
    }
}