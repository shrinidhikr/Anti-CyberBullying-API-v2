package com.google.firebase.udacity.friendlychat;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;


import java.util.ArrayList;
import java.util.Arrays;

import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static int counter=0;
    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;
    public static final int RC_SIGN_IN = 1;
    private static final int RC_PHOTO_PICKER =  2;

    private ListView mMessageListView;
    private MessageAdapter mMessageAdapter;
    private ProgressBar mProgressBar;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;
    protected Handler handler;
    private String mUsername;

    private TextView txtSpeechInput;
    private ImageButton btnSpeak;
    private final int REQ_CODE_SPEECH_INPUT = 100;

    int count=0;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mMessagesDatabaseReference,mCount;
    private ChildEventListener mChildEventListener;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mChatPhotosStorageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler();

        mUsername = ANONYMOUS;
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseStorage = FirebaseStorage.getInstance();
        mMessagesDatabaseReference = mFirebaseDatabase.getReference().child("messages");
        mCount=mFirebaseDatabase.getReference().child("counters");
        mChatPhotosStorageReference = mFirebaseStorage.getReference().child("chat_photos");
        // Initialize references to views
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageListView = (ListView) findViewById(R.id.messageListView);
        mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendButton = (Button) findViewById(R.id.sendButton);

        // Initialize message ListView and its adapter
        List<FriendlyMessage> friendlyMessages = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(this, R.layout.item_message, friendlyMessages);
        mMessageListView.setAdapter(mMessageAdapter);

        // Initialize progress bar
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);
            }
        });

        // Enable Send button when there's text to send
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                   //       mSendButton.setEnabled(true);
                } else {
                    //mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});

        // Send button sends a message and clears the EditText
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final String comment = mMessageEditText.getText().toString();
                Toast.makeText(getApplicationContext(),"Reviewing response",  Toast.LENGTH_SHORT).show();

                //String request_url = "https://pesanticyber.herokuapp.com/?query=\"";
                /*String a ="https://pesanticyber.herokuapp.com/?query=";
                String b = comment;
                String request_url = a+"\""+b+"\"";*/

                Uri.Builder builder = new Uri.Builder();
                builder.scheme("https").encodedAuthority("pesanticyber.herokuapp.com").appendQueryParameter("query",comment);
                String request_url = builder.build().toString();

                Log.d("urlreqs",request_url);

                   final RequestQueue rq = Volley.newRequestQueue(MainActivity.this);

                final StringRequest stringRequest = new StringRequest(request_url, new Response.Listener<String>() {
                    private long mRequestStartTime = System.currentTimeMillis();
                    @Override
                    public void onResponse(String response) {

                               try {
                                   String responsing;
                                   Log.e("response",response);
                                   responsing = response.substring(1,response.length()-2);
                                   Log.e("responsing",responsing);

                                   long totalRequestTime = System.currentTimeMillis() - mRequestStartTime;
                                   Log.e("responseTime",String.valueOf(totalRequestTime));
                                   Log.e("counter",String.valueOf(counter));

                                   if(Float.parseFloat(responsing)<0.5)
                                   {
                                       Log.e("responsingnotbully",responsing);
                                       FriendlyMessage friendlyMessage = new FriendlyMessage(comment, mUsername, null);
                                       mMessagesDatabaseReference.push().setValue(friendlyMessage);
                                       //mMessageEditText.setText("");
                                   }
                                   else{
                                       counter++;
                                       if(counter>2)
                                       {
                                           mSendButton.setEnabled(false);
                                           Log.e("counter",String.valueOf(counter));
                                           Toast.makeText(getApplicationContext(),"You have exceeded limits of abusive comments. You cannot send anymore comments.",Toast.LENGTH_LONG);
                                       }

                                   }
                                  /* else{

                                       DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                                       DatabaseReference itemsRef = rootRef.child("counters");
                                       ValueEventListener eventListener = new ValueEventListener() {
                                           @Override
                                           public void onDataChange(DataSnapshot dataSnapshot) {
                                               Log.e("ke",dataSnapshot.getKey());
                                               Log.e("kess",dataSnapshot.toString());

                                               for(DataSnapshot ds : dataSnapshot.getChildren()) {
                                                   if(ds.getKey()==mUsername) {
                                                       count = Integer.parseInt(ds.getValue(String.class));
                                                       Log.e("TAG",ds.getValue(String.class) );
                                                   }
                                               }
                                           }

                                           @Override
                                           public void onCancelled(DatabaseError databaseError) {}
                                       };
                                       itemsRef.addListenerForSingleValueEvent(eventListener);




                                       Log.e("responsecount",responsing);
                                      FriendlyMessage friendlyMessage = new FriendlyMessage(comment, mUsername, null);
                                     count++;
                                      mCount.child(mUsername).setValue(count);
                                   }*/


                                 //  mMessageEditText.setText(response.toString());
                                   Toast.makeText(getApplicationContext(),responsing,  Toast.LENGTH_LONG).show();
                                   mMessageEditText.setText("");

                               } catch (Exception e) {
                                Log.d("Volley Exception: ", String.valueOf(e));
                                e.printStackTrace();
                            }
                        }

                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                        long mRequestStartTime = System.currentTimeMillis();
                        long totalRequestTime = System.currentTimeMillis() - mRequestStartTime;
                        Log.e("errorresponseTime",String.valueOf(totalRequestTime));


                        Log.i("Volley Error: ", String.valueOf(error));
                        Toast.makeText(getApplicationContext(),"Connection timeout! Try again!  ",Toast.LENGTH_SHORT).show();
                    }
                });
                stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                        100000,
                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                rq.add(stringRequest);


               /* FriendlyMessage friendlyMessage = new FriendlyMessage(mMessageEditText.getText().toString(), mUsername, null);
                mMessagesDatabaseReference.push().setValue(friendlyMessage);
                // Clear input box
                mMessageEditText.setText("");*/
            }
        });

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if(user!=null) {
                    onSignedInInitialize(user.getDisplayName());

                    Toast.makeText(MainActivity.this,"You are now signed in",Toast.LENGTH_LONG).show();
                } else {
                    onSignedOutCleanup();
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setAvailableProviders(Arrays.asList(
                                            new AuthUI.IdpConfig.EmailBuilder().build(),
                                            new AuthUI.IdpConfig.PhoneBuilder().build(),
                                            new AuthUI.IdpConfig.GoogleBuilder().build())
                                            )
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };


        txtSpeechInput = (EditText) findViewById(R.id.messageEditText);
        btnSpeak = (ImageButton) findViewById(R.id.btnSpeak);

        // hide the action bar
        //getActionBar().hide();

        btnSpeak.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                promptSpeechInput();
            }
        });
    }

    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.sign_out_menu:
                AuthUI.getInstance().signOut(this);
                return true;
            //case R.id.item1:
                //Intent i = new Intent(getApplicationContext(),FindColleges.class);
                //startActivity(i);
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void onSignedInInitialize(String username) {
        mUsername = username;
        attachDatabaseReadListener();
    }

    private void attachDatabaseReadListener() {
        if (mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    FriendlyMessage friendlyMessage = dataSnapshot.getValue(FriendlyMessage.class);
                    mMessageAdapter.add(friendlyMessage);
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            };
            mMessagesDatabaseReference.addChildEventListener(mChildEventListener);
        }
    }

    private void onSignedOutCleanup(){
            mUsername = ANONYMOUS;
            mMessageAdapter.clear();
            detachDatabaseReadListener();
    }

    private void detachDatabaseReadListener(){
        if(mChildEventListener != null) {
            mMessagesDatabaseReference.removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Signed In", Toast.LENGTH_LONG).show();
            } else if(resultCode == RESULT_CANCELED ){
                Toast.makeText(this, "Signed In Canceled", Toast.LENGTH_LONG).show();
                finish();
            } }else if(resultCode == RESULT_OK && requestCode == RC_PHOTO_PICKER){
                Uri selectedImageUri = data.getData();
               final StorageReference photoRef = mChatPhotosStorageReference.child(selectedImageUri.getLastPathSegment());
                photoRef.putFile(selectedImageUri).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }
                        return photoRef.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()) {
                            Uri downloadUri = task.getResult();
                            FriendlyMessage friendlyMessage = new FriendlyMessage(null, mUsername, downloadUri.toString());
                            mMessagesDatabaseReference.push().setValue(friendlyMessage);
                        } else {
                            Toast.makeText(MainActivity.this, "upload failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });

            }
            else if(requestCode == REQ_CODE_SPEECH_INPUT)
        {
            if (resultCode == RESULT_OK && null != data) {

                ArrayList<String> result = data
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                txtSpeechInput.setText(result.get(0));
            }
        }
        }

    @Override
    protected void onPause(){
        super.onPause();
        if(mAuthStateListener != null)
        mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        detachDatabaseReadListener();
        mMessageAdapter.clear();
    }
    @Override
    protected void onResume(){
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }


}
