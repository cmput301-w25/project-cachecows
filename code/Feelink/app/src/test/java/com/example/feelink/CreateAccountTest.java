package com.example.feelink;

import android.content.Intent;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CreateAccountTest {

    @Mock private FirebaseAuth mockAuth;
    @Mock private FirebaseFirestore mockFirestore;
    @Mock private EditText mockNameEditText;
    @Mock private EditText mockUsernameEditText;
    @Mock private EditText mockDobEditText;
    @Mock private EditText mockEmailEditText;
    @Mock private EditText mockPasswordEditText;
    @Mock private EditText mockRepeatedPasswordEditText;
    @Mock private Task<AuthResult> mockAuthTask;
    @Mock private Task<Void> mockVoidTask;
    @Mock private FirebaseUser mockUser;
    @Mock private CollectionReference mockCollectionReference;
    @Mock private DocumentReference mockDocumentReference;
    @Mock private WriteBatch mockBatch;
    @Mock private View mockContentView;

    private CreateAccount createAccount;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        // Create instance of CreateAccount
        createAccount = new CreateAccount();

        // Use reflection to inject mocks
        setPrivateField(createAccount, "mAuth", mockAuth);
        setPrivateField(createAccount, "db", mockFirestore);
        setPrivateField(createAccount, "nameEditText", mockNameEditText);
        setPrivateField(createAccount, "usernameEditText", mockUsernameEditText);
        setPrivateField(createAccount, "dobEditText", mockDobEditText);
        setPrivateField(createAccount, "emailEditText", mockEmailEditText);
        setPrivateField(createAccount, "passwordEditText", mockPasswordEditText);
        setPrivateField(createAccount, "repeatedPasswordEditText", mockRepeatedPasswordEditText);

        // Mock findViewById to return our content view for Snackbar
        when(createAccount.findViewById(android.R.id.content)).thenReturn(mockContentView);

        // Setup common mock behaviors
        when(mockFirestore.collection(anyString())).thenReturn(mockCollectionReference);
        when(mockCollectionReference.document(anyString())).thenReturn(mockDocumentReference);
        when(mockFirestore.batch()).thenReturn(mockBatch);
        when(mockBatch.commit()).thenReturn(mockVoidTask);
        when(mockAuth.getCurrentUser()).thenReturn(mockUser);
    }

    // Helper method to set private fields using reflection
    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    // Helper method to invoke private methods using reflection
    private Object invokePrivateMethod(Object target, String methodName, Class<?>[] paramTypes, Object[] params) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        return method.invoke(target, params);
    }

    @Test
    public void testCreateNewAccount_EmptyFields_DoesNotCreateUser() throws Exception {
        // Arrange - set one field as empty
        when(mockNameEditText.getText().toString()).thenReturn("");
        when(mockUsernameEditText.getText().toString()).thenReturn("username");
        when(mockDobEditText.getText().toString()).thenReturn("01/01/2000");
        when(mockEmailEditText.getText().toString()).thenReturn("test@example.com");
        when(mockPasswordEditText.getText().toString()).thenReturn("Password123");
        when(mockRepeatedPasswordEditText.getText().toString()).thenReturn("Password123");

        // Act
        invokePrivateMethod(createAccount, "createNewAccount", new Class<?>[]{}, new Object[]{});

        // Assert - verify auth was never called
        verify(mockAuth, never()).createUserWithEmailAndPassword(anyString(), anyString());
    }

    @Test
    public void testCreateNewAccount_InvalidEmail_DoesNotCreateUser() throws Exception {
        // Arrange - set invalid email format
        when(mockNameEditText.getText().toString()).thenReturn("Test User");
        when(mockUsernameEditText.getText().toString()).thenReturn("username");
        when(mockDobEditText.getText().toString()).thenReturn("01/01/2000");
        when(mockEmailEditText.getText().toString()).thenReturn("invalid-email");
        when(mockPasswordEditText.getText().toString()).thenReturn("Password123");
        when(mockRepeatedPasswordEditText.getText().toString()).thenReturn("Password123");

        // Act
        invokePrivateMethod(createAccount, "createNewAccount", new Class<?>[]{}, new Object[]{});

        // Assert - verify auth was never called
        verify(mockAuth, never()).createUserWithEmailAndPassword(anyString(), anyString());
    }

    @Test
    public void testCreateNewAccount_PasswordMismatch_DoesNotCreateUser() throws Exception {
        // Arrange - passwords don't match
        when(mockNameEditText.getText().toString()).thenReturn("Test User");
        when(mockUsernameEditText.getText().toString()).thenReturn("username");
        when(mockDobEditText.getText().toString()).thenReturn("01/01/2000");
        when(mockEmailEditText.getText().toString()).thenReturn("test@example.com");
        when(mockPasswordEditText.getText().toString()).thenReturn("Password123");
        when(mockRepeatedPasswordEditText.getText().toString()).thenReturn("DifferentPass123");

        // Act
        invokePrivateMethod(createAccount, "createNewAccount", new Class<?>[]{}, new Object[]{});

        // Assert - verify auth was never called
        verify(mockAuth, never()).createUserWithEmailAndPassword(anyString(), anyString());
    }

    @Test
    public void testCreateNewAccount_UsernameNotAvailable_DoesNotCreateUser() throws Exception {
        // Arrange - all fields valid but username not available
        when(mockNameEditText.getText().toString()).thenReturn("Test User");
        when(mockUsernameEditText.getText().toString()).thenReturn("username");
        when(mockDobEditText.getText().toString()).thenReturn("01/01/2000");
        when(mockEmailEditText.getText().toString()).thenReturn("test@example.com");
        when(mockPasswordEditText.getText().toString()).thenReturn("Password123");
        when(mockRepeatedPasswordEditText.getText().toString()).thenReturn("Password123");

        // Set username as unavailable
        setPrivateField(createAccount, "usernameAvailable", false);

        // Act
        invokePrivateMethod(createAccount, "createNewAccount", new Class<?>[]{}, new Object[]{});

        // Assert - verify auth was never called
        verify(mockAuth, never()).createUserWithEmailAndPassword(anyString(), anyString());
    }

    @Test
    public void testCreateNewAccount_ValidInput_CreatesUser() throws Exception {
        // Arrange - all fields valid
        when(mockNameEditText.getText().toString()).thenReturn("Test User");
        when(mockUsernameEditText.getText().toString()).thenReturn("username");
        when(mockDobEditText.getText().toString()).thenReturn("01/01/2000");
        when(mockEmailEditText.getText().toString()).thenReturn("test@example.com");
        when(mockPasswordEditText.getText().toString()).thenReturn("Password123");
        when(mockRepeatedPasswordEditText.getText().toString()).thenReturn("Password123");

        // Set username as available
        setPrivateField(createAccount, "usernameAvailable", true);

        // Mock successful authentication
        when(mockAuth.createUserWithEmailAndPassword(anyString(), anyString())).thenReturn(mockAuthTask);

        // Setup task completion listener callback
        doAnswer(invocation -> {
            OnCompleteListener<AuthResult> listener = invocation.getArgument(0);
            when(mockAuthTask.isSuccessful()).thenReturn(true);
            listener.onComplete(mockAuthTask);
            return null;
        }).when(mockAuthTask).addOnCompleteListener(any());

        // Act
        invokePrivateMethod(createAccount, "createNewAccount", new Class<?>[]{}, new Object[]{});

        // Assert - verify auth was called with correct parameters
        verify(mockAuth).createUserWithEmailAndPassword("test@example.com", "Password123");
    }

    @Test
    public void testAddUserToFirestore_ValidInput_CommitsBatch() throws Exception {
        // Arrange - prepare for addUserToFirestore call
        when(mockUser.getUid()).thenReturn("test-user-id");

        // Mock successful batch commit
        doAnswer(invocation -> {
            OnSuccessListener<Void> listener = invocation.getArgument(0);
            listener.onSuccess(null);
            return mockVoidTask;
        }).when(mockVoidTask).addOnSuccessListener(any());

        // Act
        invokePrivateMethod(
                createAccount,
                "addUserToFirestore",
                new Class<?>[] {FirebaseUser.class, String.class, String.class, String.class, String.class},
                new Object[] {mockUser, "Test User", "username", "01/01/2000", "test@example.com"}
        );

        // Assert
        verify(mockFirestore).batch();
        verify(mockFirestore).collection("users");
        verify(mockFirestore).collection("usernames");
        verify(mockBatch, times(2)).set(any(), any());
        verify(mockBatch).commit();
    }

    @Test
    public void testAddUserToFirestore_FirestoreFailure_DeletesUser() throws Exception {
        // Arrange - prepare for addUserToFirestore call
        when(mockUser.getUid()).thenReturn("test-user-id");

        // Mock batch commit failure
        doAnswer(invocation -> {
            OnFailureListener listener = invocation.getArgument(0);
            listener.onFailure(new Exception("Test exception"));
            return mockVoidTask;
        }).when(mockVoidTask).addOnFailureListener(any());

        // Act
        invokePrivateMethod(
                createAccount,
                "addUserToFirestore",
                new Class<?>[] {FirebaseUser.class, String.class, String.class, String.class, String.class},
                new Object[] {mockUser, "Test User", "username", "01/01/2000", "test@example.com"}
        );

        // Assert - verify user deletion on failure
        verify(mockUser).delete();
    }
}
