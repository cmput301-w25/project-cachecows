package com.example.feelink;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class ConversationsListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ConversationsAdapter adapter;
    private FirestoreManager firestoreManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversations_list);

        firestoreManager = new FirestoreManager(FirebaseAuth.getInstance().getCurrentUser().getUid());
        recyclerView = findViewById(R.id.recyclerConversations);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadConversations();
    }

    private void loadConversations() {
        firestoreManager.getConversations(new FirestoreManager.OnConversationsListener() {
            @Override
            public void onSuccess(List<Conversation> conversations) {
                adapter = new ConversationsAdapter(conversations, conversation -> {
                    Intent intent = new Intent(ConversationsListActivity.this, ChatActivity.class);
                    intent.putExtra("OTHER_USER_ID", getOtherUserId(conversation.getParticipants()));
                    startActivity(intent);
                });
                recyclerView.setAdapter(adapter);
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(ConversationsListActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getOtherUserId(List<String> participantIds) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        return participantIds.get(0).equals(currentUserId) ?
                participantIds.get(1) : participantIds.get(0);
    }
}