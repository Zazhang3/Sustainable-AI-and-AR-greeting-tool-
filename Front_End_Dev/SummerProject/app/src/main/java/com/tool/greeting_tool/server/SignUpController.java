package com.tool.greeting_tool.server;

import static com.tool.greeting_tool.common.utils.FormatCheckerUtil.checkPassword;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.tool.greeting_tool.R;
import com.tool.greeting_tool.common.constant.ErrorMessage;
import com.tool.greeting_tool.common.constant.KeySet;
import com.tool.greeting_tool.common.constant.TAGConstant;
import com.tool.greeting_tool.common.constant.URLConstant;
import com.tool.greeting_tool.common.utils.JsonUtil;
import com.tool.greeting_tool.common.utils.SharedPreferencesUtil;
import com.tool.greeting_tool.pojo.vo.UserVO;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SignUpController extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText passwordEditText;
    private EditText rePasswordEditText;
    private EditText emailEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        usernameEditText = findViewById(R.id.id_signup_id);
        passwordEditText = findViewById(R.id.id_signup_password);
        rePasswordEditText = findViewById(R.id.id_signup_password_re);
        emailEditText = findViewById(R.id.email_set);
        ImageButton backButton = findViewById(R.id.id_back_signup);
        ImageButton SignUp = findViewById(R.id.id_signup_button);

        SignUp.setOnClickListener(v -> {

            String username = usernameEditText.getText().toString();
            String password = passwordEditText.getText().toString();
            String email = emailEditText.getText().toString();
            if (checkPassword(password)) {
                if (password.equals(rePasswordEditText.getText().toString())) {
                    if (!username.isEmpty()) {
                        signUp(username,password,email);
                    } else {
                        Toast.makeText(SignUpController.this, ErrorMessage.USERNAME_EMPTY_ERROR, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(SignUpController.this, ErrorMessage.INCONSISTENT_PASSWORD, Toast.LENGTH_SHORT).show();
                }
            }else {
                Toast.makeText(SignUpController.this, ErrorMessage.INVALID_PASSWORD, Toast.LENGTH_SHORT).show();
            }
        });

        backButton.setOnClickListener(v -> onBackPressed());
    }

    /**
     * Sign up with username and password
     * @param username : the username
     * @param password : the password
     */
    private void signUp(String username,String password,String email){
        //generate userVO
        UserVO userVO = new UserVO();
        userVO.setUsername(username);
        userVO.setPassword(password);
        userVO.setEmail(email);

        //generate request
        Gson gson = new Gson();
        String json = gson.toJson(userVO);

        RequestBody body = RequestBody.create(
                json,
                MediaType.get("application/json;charset=utf-8"));

        Request request = new Request.Builder()
                .url(URLConstant.SIGN_UP_URL)
                .post(body)
                .build();

        //send request
        OkHttpClient client = new OkHttpClient();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAGConstant.SIGN_UP_TAG,"Sign up failed",e);
                runOnUiThread(() -> Toast.makeText(SignUpController.this,"Sign up failed: " + ErrorMessage.NETWORK_ERROR,Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseBody = response.body().string();
                Log.d(TAGConstant.SIGN_UP_TAG,"Response: "+ responseBody);
                runOnUiThread(() -> {
                    try {
                        Gson gson1 = new Gson();
                        JsonObject jsonResponse = gson1.fromJson(responseBody, JsonObject.class);
                        int code = jsonResponse.get("code").getAsInt();
                        if (code == 1) {
                            // login successfully: handle response
                            Long id = jsonResponse.get("data").getAsJsonObject().get("id").getAsLong();
                            String token = jsonResponse.get("data").getAsJsonObject().get("token").getAsString();
                            Toast.makeText(SignUpController.this, "Sign up successfully", Toast.LENGTH_SHORT).show();

                            //save user data
                            SharedPreferencesUtil.saveUserInfo(SignUpController.this,id,username,token);

                            // save user data for continuously login
                            if (!JsonUtil.jsonFileIsExist()) {
                                JsonUtil.saveLoginInfoToFile(username, password);
                            } else {
                                JsonUtil.updateLoginInfoToFile(username, password);
                            }

                            Intent resultIntent = new Intent();
                            resultIntent.putExtra(KeySet.UserKey, username);
                            setResult(RESULT_OK, resultIntent);
                            finish();
                        } else {
                            // login failed
                            String msg = jsonResponse.get("msg").getAsString();
                            Toast.makeText(SignUpController.this, "Sign up failed: " + msg, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAGConstant.SIGN_UP_TAG, "Exception while parsing response", e);
                        Toast.makeText(SignUpController.this, "Sign up failed: " + ErrorMessage.INVALID_RESPONSE, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

}