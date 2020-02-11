package com.example.geofencing_1;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class Register extends AppCompatActivity {

    private Button CreateAccountButton;
    private EditText InputName, InputPhoneNumber, InputPassword, emergencyName, emergencyPhn, emergencyRelation, InputEmail;
    private ProgressDialog loadingBar;
    FirebaseAuth firebaseAuth;
    TextView alreadyregistered;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        CreateAccountButton = (Button) findViewById(R.id.register_btn);
        InputName = (EditText) findViewById(R.id.register_username_input);
        InputPassword = (EditText) findViewById(R.id.register_password_input);
        InputPhoneNumber = (EditText) findViewById(R.id.register_phone_number_input);
        emergencyName = (EditText)findViewById(R.id.emergency_name_input);
        emergencyPhn = (EditText)findViewById(R.id.emerncy_phone_number_input);
        emergencyRelation = (EditText)findViewById(R.id.emerency_relation);
        InputEmail =(EditText)findViewById(R.id.register_email_input);
        alreadyregistered = (TextView)findViewById(R.id.already_registered);

        loadingBar = new ProgressDialog(this);

        firebaseAuth = FirebaseAuth.getInstance();

        alreadyregistered.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(),Login.class));
            }
        });

        CreateAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                final String name = InputName.getText().toString();
                final String phone = InputPhoneNumber.getText().toString();
                final String password = InputPassword.getText().toString();
                final String email = InputEmail.getText().toString();
                final String eName = emergencyName.getText().toString();
                final String ePhone = emergencyPhn.getText().toString();
                final String eRelation = emergencyRelation.getText().toString();


                if (TextUtils.isEmpty(name))
                {
                    Toast.makeText(Register.this, "Please write your name...", Toast.LENGTH_SHORT).show();
                }
                else if (TextUtils.isEmpty(phone))
                {
                    Toast.makeText(Register.this, "Please write your phone number...", Toast.LENGTH_SHORT).show();
                }
                else if (TextUtils.isEmpty(email))
                {
                    Toast.makeText(Register.this, "Please write your email id..", Toast.LENGTH_SHORT).show();
                }
                else if (TextUtils.isEmpty(password))
                {
                    Toast.makeText(Register.this, "Please write your password...", Toast.LENGTH_SHORT).show();
                }
                else if (TextUtils.isEmpty(eName))
                {
                    Toast.makeText(Register.this, "Please fill Emergency Contact name...", Toast.LENGTH_SHORT).show();
                }
                else if (TextUtils.isEmpty(ePhone))
                {
                    Toast.makeText(Register.this, "Please fill Emergency Contact's Number...", Toast.LENGTH_SHORT).show();
                }
                else if (TextUtils.isEmpty(eRelation))
                {
                    Toast.makeText(Register.this, "How are you related ?", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    loadingBar.setTitle("Create Account");
                    loadingBar.setMessage("Please wait, while we are checking the credentials.");
                    loadingBar.setCanceledOnTouchOutside(false);
                    loadingBar.show();


                    firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                final User user = new User(
                                        name,email,
                                        phone,eName,
                                        ePhone,eRelation
                                );
                                FirebaseDatabase.getInstance().getReference("User")
                                        .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                        .setValue(user).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        Toast.makeText(Register.this, "Successfuly Registered", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(getApplicationContext(),Login.class));
                                        loadingBar.dismiss();

                                    }
                                });
                            } else {
                                Toast.makeText(Register.this, "Error", Toast.LENGTH_SHORT).show();
                                loadingBar.dismiss();
                            }
                        }
                    });

                }
            }
        });
    }
}
