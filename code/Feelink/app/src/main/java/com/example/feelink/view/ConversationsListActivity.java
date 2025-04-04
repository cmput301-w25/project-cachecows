package com.example.feelink.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.feelink.adapter.ConversationsAdapter;
import com.example.feelink.controller.FirestoreManager;
import com.example.feelink.R;
import com.example.feelink.model.Conversation;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;
/**
 * Displays chat conversations between users. Shows recent messages and allows navigation to chat threads.
 * Implements core features of the app's direct messaging "Wow" factor.
 * <p>
 * Key user stories:
 * <ul>
 *   <li>US 03.03.01 (View other users' profiles via participant details)</li>
 *   <li>Follow system requirements from US 05.02.01 (Messaging requires mutual follows)</li>
 * </ul>
 */

public class ConversationsListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ConversationsAdapter adapter;
    private FirestoreManager firestoreManager;
    /**
     * Initializes conversation list and real-time updates from Firestore
     * @param savedInstanceState Preserved state if available
     *
     * Handles: US 05.03.01 (Recent social interactions visibility)
     */

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