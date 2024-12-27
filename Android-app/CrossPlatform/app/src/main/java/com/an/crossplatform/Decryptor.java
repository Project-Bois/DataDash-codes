package com.an.crossplatform;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.an.crossplatform.AESUtils.EncryptionUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Decryptor extends AppCompatActivity {

    private ArrayList<String> encryptedFiles;
    private EditText passwordInput;
    TextView decryptedFilesTextView;
    private Button decryptButton;
    private int incorrectPasswordAttempts = 0;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decryptor);

        Intent intent = getIntent();
        encryptedFiles = intent.getStringArrayListExtra("files");

        passwordInput = findViewById(R.id.editTextText2);
        decryptButton = findViewById(R.id.button);
        decryptedFilesTextView = findViewById(R.id.textView2);

        decryptButton.setOnClickListener(v -> {
            String password = passwordInput.getText().toString();
            if (password.isEmpty()) {
                Toast.makeText(this, "Please enter a password.", Toast.LENGTH_SHORT).show();
                return;
            }
            decryptFilesInBackground(password);
        });
    }

    private void decryptFilesInBackground(String password) {
        // Create a new ExecutorService to run the decryption task in the background
        executorService = Executors.newSingleThreadExecutor();

        executorService.execute(() -> {
            runOnUiThread(() -> Toast.makeText(Decryptor.this, "Decryption started...", Toast.LENGTH_SHORT).show());
            decryptFiles(password);
        });
    }

    private void decryptFiles(String password) {
        boolean allDecryptedSuccessfully = true; // Flag to track decryption status
        boolean shouldStopDecryption = false; // Flag to stop decryption on incorrect attempt

        runOnUiThread(() -> {
            decryptedFilesTextView.setText("");
        });

        for (String filePath : encryptedFiles) {

            File inputFile = new File(filePath);

            if (inputFile.isDirectory()) {
                continue;
            }

            // Remove the last 6 characters (".crypt") from the filename to get the output filename
            String outputFileName = filePath.substring(0, filePath.length() - 6);
            File outputFile = new File(inputFile.getParent(), new File(outputFileName).getName());

            try {
                EncryptionUtils.decryptFile(password, inputFile, outputFile);

                // If decryption is successful, append the filename to textView2
                runOnUiThread(() -> {
                    String currentText = decryptedFilesTextView.getText().toString();
                    String newText = currentText + "\n" + outputFileName; // Append filename to the current text
                    decryptedFilesTextView.setText(newText);
                });

            } catch (Exception e) {
                allDecryptedSuccessfully = false; // Set flag to false if any decryption fails
                incorrectPasswordAttempts++;

                // If an exception occurs, delete the outputFile (if exists)
                if (outputFile.exists()) {
                    outputFile.delete();
                }

                if (incorrectPasswordAttempts >= 3) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Password incorrect multiple times. Deleting encrypted files.", Toast.LENGTH_LONG).show();
                    });
                    deleteAllFiles();
                    break; // Exit the loop after deleting files
                }

                runOnUiThread(() -> {
                    Toast.makeText(this, "Incorrect Password, Remaining attempts: "+(3-incorrectPasswordAttempts), Toast.LENGTH_LONG).show();
                });
                e.printStackTrace();
                break;
            }
        }


        // Show toast if all files were decrypted successfully
        if (allDecryptedSuccessfully) {
            runOnUiThread(() -> {
                Toast.makeText(this, "All files decrypted successfully!", Toast.LENGTH_SHORT).show();
            });
            deleteEncryptedFiles();
        }
    }

    private void deleteEncryptedFiles() {
        for (String filePath : encryptedFiles) {
            File inputFile = new File(filePath);
            if (inputFile.exists()) {
                inputFile.delete();
            }
        }

        // Shut down the executor to release resources
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }

        runOnUiThread(() -> {
            finish();
        });
    }

    private void deleteAllFiles() {
        // Delete All files, including folders
        for (String filePath : encryptedFiles) {
            File inputFile = new File(filePath);
            if (inputFile.exists()) {
                deleteRecursive(inputFile);
            }
        }

        // Shut down the executor to release resources
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }

        runOnUiThread(() -> {
            finish();
        });
    }

    // Recursive method for deleting files and directories
    private void deleteRecursive(File fileOrDir) {
        if (fileOrDir.isDirectory()) {
            File[] children = fileOrDir.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        fileOrDir.delete(); // Deletes the file or empty directory
    }
}
