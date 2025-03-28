package com.example.feelink;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class ChatActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private EditText etMessage;
    private FirestoreManager firestoreManager;
    private String conversationId, otherUserId;

    private TextView tvUserName;
    private ImageView ivProfilePicture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Get conversation data from intent
        otherUserId = getIntent().getStringExtra("OTHER_USER_ID");
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        firestoreManager = new FirestoreManager(currentUserId);

        conversationId = generateConversationId(firestoreManager.getUserId(), otherUserId);

        setupViews();
        loadMessages();

    }

    private String generateConversationId(String userId1, String userId2) {
        String[] ids = {userId1, userId2};
        Arrays.sort(ids);
        return ids[0] + "_" + ids[1];
    }

    private void setupViews() {
        recyclerView = findViewById(R.id.recyclerMessages);
        etMessage = findViewById(R.id.etMessage);
        tvUserName = findViewById(R.id.tvUserName);
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        findViewById(R.id.btnSend).setOnClickListener(v -> sendMessage());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new MessageAdapter(new ArrayList<>(), firestoreManager.getUserId()));

        loadUserName();
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (!text.isEmpty()) {
            // Create a temporary message with local timestamp
            Message tempMessage = new Message(text, firestoreManager.getUserId(), new Date());

            // Add the temporary message to the adapter
            MessageAdapter adapter = (MessageAdapter) recyclerView.getAdapter();
            List<Message> currentMessages = new ArrayList<>(adapter.messages);
            currentMessages.add(tempMessage);
            adapter.updateMessages(currentMessages);
            recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);

            // Send the message via Firestore
            firestoreManager.sendMessage(conversationId, text, otherUserId);
            etMessage.setText("");
        }
    }

    private void loadMessages() {
        firestoreManager.getMessages(conversationId, messages -> {
            ((MessageAdapter) recyclerView.getAdapter()).updateMessages(messages);
            if (messages != null && !messages.isEmpty()) {
                recyclerView.smoothScrollToPosition(messages.size() - 1);
            }
        });
    }

    private void loadUserName() {
        firestoreManager.getUsernameById(otherUserId, new FirestoreManager.OnUsernameListener() {
            @Override
            public void onSuccess(String username) {
                tvUserName.setText(username);

            }

            @Override
            public void onFailure(String fallback) {
                tvUserName.setText(fallback);
            }
        });
    }

}