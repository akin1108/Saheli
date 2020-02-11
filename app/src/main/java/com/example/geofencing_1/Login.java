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
import com.google.firebase.database.DatabaseReference;

public class Login extends AppCompatActivity {

    private EditText InputEmail, InputPassword;
    private Button LoginButton;
    private FirebaseAuth lAuth;
    private DatabaseReference databaseReference;
    private TextView signup;
    private ProgressDialog lDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        InputEmail = (EditText)findViewById(R.id.login_email_input);
        InputPassword = (EditText)findViewById(R.id.login_password_input);
        LoginButton =(Button)findViewById(R.id.login_btn);
        signup = (TextView)findViewById(R.id.signUp);

        lDialog = new ProgressDialog(Login.this);
        lAuth = FirebaseAuth.getInstance();
        if(lAuth.getCurrentUser() != null){
            startActivity(new Intent(getApplicationContext(),MapsActivity.class));
        }

        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(),Register.class);
                startActivity(intent);
            }
        });

        LoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String lEmail = InputEmail.getText().toString().trim();
                String lPassword = InputPassword.getText().toString().trim();

                if(TextUtils.isEmpty(lEmail)){
                    InputEmail.setError("Enter email id");
                    return;
                }
                if(TextUtils.isEmpty(lPassword)){
                    InputPassword.setError("Enter password");
                    return;
                }

                lDialog.setMessage("Processing..");
                lDialog.show();

                lAuth.signInWithEmailAndPassword(lEmail,lPassword).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (task.isSuccessful()){
                            Toast.makeText(getApplicationContext(),"Logged in Successfuly",Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(Login.this,MapsActivity.class));
                            lDialog.dismiss();
                        }
                        else{
                            Toast.makeText(getApplicationContext(),"Error",Toast.LENGTH_SHORT).show();
                            lDialog.dismiss();
                        }
                    }
                });
            }
        });
    }
}
