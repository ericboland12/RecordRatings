package com.example.recordratings.records;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.recordratings.MainActivity;
import com.example.recordratings.R;
import com.example.recordratings.misc.DatabaseHelper;
import com.example.recordratings.misc.MovePage;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class EditRecord extends AppCompatActivity {
    private int PICK_IMAGE_REQUEST = 1;
    private MovePage m = new MovePage();
    private EditText albumName;
    private EditText artistName;
    private EditText description;
    private ImageView photo;
    private RatingBar rating;
    private Spinner genre;

    public static int idTemp;
    public static String albumTemp;
    public static String artistTemp;
    public static double ratingTemp;
    public static String genreTemp;
    public static String descTemp;
    public static Bitmap photoTemp;
    public static String photoStringTemp;

    public static Bitmap photoFinal;

    //Database declarations
    private DatabaseHelper dbh;

    //Button declaration
    private Button browseGalleryBtn;
    private Button mSubmitBtn;


    private SharedPreferences shared;
    private FirebaseDatabase database;
    private StorageReference mStorageRef;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private com.google.firebase.database.DatabaseReference dbRef;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(returnDark()){
            setTheme(R.style.darkTheme);
        }
        setContentView(R.layout.activity_edit_record);

        //Input edit text values into global variables
        albumName = (EditText) findViewById(R.id.editText);
        artistName = (EditText) findViewById(R.id.editText2);
        rating = findViewById(R.id.add_record_rating);
        genre = findViewById(R.id.genre_spinner);
        description = findViewById(R.id.editText3);

        //Inputs stored values
        albumName.setText(albumTemp);
        artistName.setText(artistTemp);
        description.setText(descTemp);
        rating.setRating((float) ratingTemp);

        //Sets up Firebase
        database = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();

        //Adds options to Spinner
        String[] items = new String[]{"Pop", "Rock", "Jazz", "Blues", "Rap", "Country", "Folk",
                "Metal", "Progressive", "Psychedelic", "Punk", "Alternative",
                "Indie", "Classical", "Other"};


        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        genre.setAdapter(adapter);

        int index = 0;

        for(int i = 0; i < items.length; i++){
            if(items[i].equals(genreTemp)){
                index = i;
            }
        }

        genre.setSelection(index);

        //Initializes db helper
        dbh = new DatabaseHelper(this);

        //Allows user to search gallery for photos
        browseGalleryBtn = findViewById(R.id.browse_btn);
        browseGalleryBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                chooseImage();
            }

        });

        mSubmitBtn = findViewById(R.id.edit_record_btn);
        mSubmitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(albumName.getEditableText().toString().isEmpty() ||
                        artistName.getEditableText().toString().isEmpty() ||
                        description.getEditableText().toString().isEmpty()){
                    Toast.makeText(v.getContext(), "One or more of your fields are empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                else if(photoFinal == null){
                    Toast.makeText(v.getContext(), "Please select a photo", Toast.LENGTH_SHORT).show();
                    return;
                }
                else{
                    boolean res = dbh.updateData(idTemp,
                            albumName.getText().toString(),
                            artistName.getText().toString(),
                            rating.getRating(),
                            getBytes(photoFinal),
                            genre.getSelectedItem().toString(),
                            description.getText().toString());

                    if(res){
                        String album = albumName.getText().toString();
                        String artist = artistName.getText().toString();
                        double rating2 = rating.getRating();
                        String photoUrl = photoStringTemp;
                        String genre2 = genre.getSelectedItem().toString();
                        String desc = description.getText().toString();
//                        Records newRecord = new Records(0, album, artist, rating2, photoUrl, genre2, desc);

//                        db.collection("records").document(get)
                        startActivity(new Intent(EditRecord.this, MainActivity.class));
                    }else{
                        Toast.makeText(v.getContext(), "Something went wrong.  Please check your fields.", Toast.LENGTH_SHORT).show();
                    }
                }

            }
        });
    }

    public void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (resultCode == RESULT_OK) {
            try {
                final Uri imageUri = data.getData();
                final StorageReference ref = mStorageRef.child(data.getDataString());
                ref.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                        ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                Uri downloadUri = uri;
                                photoStringTemp = downloadUri.toString();
                            }
                        });
//                        Uri downloadUrl = taskSnapshot.getStorage().get
//                        photoToString = downloadUrl.toString();
                    }
                });
                final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show();
            }

        }else {
            Toast.makeText(this, "You haven't picked Image",Toast.LENGTH_LONG).show();
        }
    }

    // convert from bitmap to byte array
    public static byte[] getBytes(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
        return stream.toByteArray();
    }

    private boolean returnDark(){
        shared = getSharedPreferences("DarkMode", MODE_PRIVATE);
        return shared.getBoolean("darkMode", false);
    }
}
