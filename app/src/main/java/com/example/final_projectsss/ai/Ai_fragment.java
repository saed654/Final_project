package com.example.final_projectsss.ai;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.final_projectsss.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Ai_fragment extends Fragment {

    // ===== UI =====
    private String productName = "";
    private EditText etMessage;
    private Button btnSend;
    private LinearLayout chatContainer;
    private ScrollView scrollChat;

    // ===== API =====
    // IMPORTANT:
    // For real apps, DO NOT keep your secret key inside the APK.
    // Move it to a backend/server if possible.
    private static final String API_KEY = "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";
    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String MODEL = "openrouter/free";

    private static final String KEY_PRODUCT_NAME = "product_name";
    private static final String KEY_MESSAGES = "messages";

    // HTTP client
    private OkHttpClient client;

    // conversation history
    private JSONArray messages;

    // prevent multiple sends while waiting
    private boolean isLoading = false;

    public Ai_fragment() {
        // required empty constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_ai_fragment, container, false);

        etMessage = rootView.findViewById(R.id.etMessage);
        btnSend = rootView.findViewById(R.id.btnSend);
        chatContainer = rootView.findViewById(R.id.chatContainer);
        scrollChat = rootView.findViewById(R.id.scrollChat);

        client = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(40, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build();

        messages = new JSONArray();

        if (savedInstanceState != null) {
            productName = savedInstanceState.getString(KEY_PRODUCT_NAME, "");

            String savedMessages = savedInstanceState.getString(KEY_MESSAGES, "");
            if (!savedMessages.isEmpty()) {
                try {
                    messages = new JSONArray(savedMessages);
                } catch (Exception e) {
                    messages = new JSONArray();
                }
            }

            rebuildChatFromMessages();
        } else {
            // First screen welcome only once
            appendMessage("AI", "Hello! Ask me anything about products, skincare, or beauty tips.", false);
        }

        // Get product name if passed from previous fragment
        if (getArguments() != null) {
            productName = getArguments().getString("product_name", "");
        }

        // Auto-ready prompt for chosen product
        if (!productName.isEmpty() && messages.length() == 0) {
            String readyPrompt = "Give me info about " + productName;
            etMessage.setText("");
            appendMessage("You", readyPrompt, true);
            sendMessageToAI(readyPrompt);
        }

        btnSend.setOnClickListener(v -> {
            String userMessage = etMessage.getText().toString().trim();

            if (TextUtils.isEmpty(userMessage)) {
                showToast("Write a message first");
                return;
            }

            if (isLoading) {
                showToast("Please wait for the AI response");
                return;
            }

            appendMessage("You", userMessage, true);
            etMessage.setText("");
            sendMessageToAI(userMessage);
        });

        return rootView;
    }

    private void sendMessageToAI(String userMessage) {
        if (!isAdded()) return;

        isLoading = true;
        btnSend.setEnabled(false);

        try {
            JSONObject userObj = new JSONObject();
            userObj.put("role", "user");
            userObj.put("content", userMessage);
            messages.put(userObj);
        } catch (Exception e) {
            showAiMessage("Failed to prepare message.");
            isLoading = false;
            btnSend.setEnabled(true);
            return;
        }

        TextView loadingView = createMessageView("AI is typing...", false);
        chatContainer.addView(loadingView);
        scrollToBottom();

        try {
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("model", MODEL);

            JSONArray requestMessages = new JSONArray();

            JSONObject systemObj = new JSONObject();
            systemObj.put("role", "system");
            systemObj.put("content",
                    "You are a helpful AI assistant inside a skincare and beauty shop app. " +
                            "Answer clearly and shortly. Be friendly. " +
                            "Explain skincare and product usage simply. " +
                            "If you are not sure, say so honestly. " +
                            "Current product: " + (productName == null ? "" : productName));
            requestMessages.put(systemObj);

            for (int i = 0; i < messages.length(); i++) {
                requestMessages.put(messages.getJSONObject(i));
            }

            bodyJson.put("messages", requestMessages);

            RequestBody body = RequestBody.create(
                    bodyJson.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    if (!isAdded() || getActivity() == null) return;

                    getActivity().runOnUiThread(() -> {
                        if (!isAdded()) return;

                        chatContainer.removeView(loadingView);
                        showAiMessage("Connection failed. Please try again.");
                        isLoading = false;
                        btnSend.setEnabled(true);
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String responseData = response.body() != null ? response.body().string() : "";

                    if (!isAdded() || getActivity() == null) return;

                    getActivity().runOnUiThread(() -> {
                        if (!isAdded()) return;

                        chatContainer.removeView(loadingView);

                        try {
                            if (!response.isSuccessful()) {
                                showAiMessage("Request failed. Code: " + response.code());
                                isLoading = false;
                                btnSend.setEnabled(true);
                                return;
                            }

                            JSONObject jsonObject = new JSONObject(responseData);
                            JSONArray choices = jsonObject.optJSONArray("choices");

                            if (choices == null || choices.length() == 0) {
                                showAiMessage("No response received.");
                                isLoading = false;
                                btnSend.setEnabled(true);
                                return;
                            }

                            JSONObject messageObject = choices.getJSONObject(0).getJSONObject("message");
                            String aiReply = messageObject.optString("content", "").trim();

                            if (aiReply.isEmpty()) {
                                aiReply = "No response received.";
                            }

                            JSONObject assistantObj = new JSONObject();
                            assistantObj.put("role", "assistant");
                            assistantObj.put("content", aiReply);
                            messages.put(assistantObj);

                            appendMessage("AI", aiReply, false);

                        } catch (Exception e) {
                            showAiMessage("Response parsing error.");
                        }

                        isLoading = false;
                        btnSend.setEnabled(true);
                    });
                }
            });

        } catch (Exception e) {
            chatContainer.removeView(loadingView);
            showAiMessage("Unexpected error happened.");
            isLoading = false;
            btnSend.setEnabled(true);
        }
    }

    private void appendMessage(String sender, String message, boolean isUser) {
        if (!isAdded()) return;

        TextView tv = createMessageView(sender + ": " + message, isUser);
        chatContainer.addView(tv);
        scrollToBottom();
    }

    private TextView createMessageView(String text, boolean isUser) {
        Context context = getContext();
        if (context == null) {
            context = requireActivity();
        }

        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tv.setPadding(30, 20, 30, 20);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(20, 12, 20, 12);

        if (isUser) {
            params.gravity = Gravity.END;
            tv.setBackgroundColor(Color.parseColor("#D1E7FF"));
            tv.setTextColor(Color.BLACK);
        } else {
            params.gravity = Gravity.START;
            tv.setBackgroundColor(Color.parseColor("#EFEFEF"));
            tv.setTextColor(Color.BLACK);
        }

        tv.setLayoutParams(params);
        return tv;
    }

    private void scrollToBottom() {
        if (scrollChat != null) {
            scrollChat.post(() -> scrollChat.fullScroll(View.FOCUS_DOWN));
        }
    }

    private void showToast(String msg) {
        if (!isAdded() || getContext() == null) return;
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private void showAiMessage(String msg) {
        appendMessage("AI", msg, false);
    }

    private void rebuildChatFromMessages() {
        if (chatContainer == null) return;

        chatContainer.removeAllViews();
        appendMessage("AI", "Hello! Ask me anything about products, skincare, or beauty tips.", false);

        for (int i = 0; i < messages.length(); i++) {
            try {
                JSONObject obj = messages.getJSONObject(i);
                String role = obj.optString("role", "");
                String content = obj.optString("content", "");

                if ("user".equals(role)) {
                    appendMessage("You", content, true);
                } else if ("assistant".equals(role)) {
                    appendMessage("AI", content, false);
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_PRODUCT_NAME, productName);
        outState.putString(KEY_MESSAGES, messages != null ? messages.toString() : "[]");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (client != null) {
            client.dispatcher().cancelAll();
        }

        etMessage = null;
        btnSend = null;
        chatContainer = null;
        scrollChat = null;
    }
}