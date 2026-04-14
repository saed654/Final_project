package com.example.final_projectsss.products;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.app.*;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.*;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.final_projectsss.MainActivity;
import com.example.final_projectsss.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;


public class ManagerProductsFragment extends UserProductsFragment
        implements ProductAdapter.Listener {

    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    private static final String TAG = "ManagerProductsFragment";
    Product p;
    String tempimgbit="";
    Uri tempUri;
    ImageView dialogImage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // All launchers should be initialized here, in onCreate.
        setupLaunchers();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_user_products, container, false);

        FloatingActionButton fab = v.findViewById(R.id.fab_add);
        rv = v.findViewById(R.id.rv);

        rv.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        adapter = new ProductAdapter(products, true, this);
        rv.setAdapter(adapter);
        fab.setOnClickListener(v1 -> openManagerOrdersSafely());
        loadProducts();
        return v;
    }


    @Override
    public void onAdd() {
        showAddEditDialog(null);
    }

    @Override
    public void onLongPress(Product p) {
        if (!isAdded()) return;

        new AlertDialog.Builder(requireContext())
                .setItems(new String[]{"Edit", "Delete"}, (d, i) -> {
                    if (i == 0) {
                        showAddEditDialog(p);
                    } else {
                        db.collection("products")
                                .document(p.id)
                                .delete()
                                .addOnSuccessListener(unused -> safeToast("Product deleted"))
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error deleting product", e);
                                    safeToast("Failed :"+e.getMessage());
                                });
                    }
                })
                .show();
    }
    private void showAddEditDialog(Product edit) {
        if (!isAdded()) return;

        // Reset temp values when adding a new product
        if (edit == null) {
            tempimgbit = "";
            tempUri = null;
        } else {
            tempimgbit = (edit.imgbase64 != null) ? edit.imgbase64 : "";
        }

        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_product, null);
        EditText nameET = v.findViewById(R.id.nameET);
        EditText priceET = v.findViewById(R.id. priceET);
        dialogImage = v.findViewById(R.id.image);

        // Fill fields if editing
        if (edit != null) {
            nameET.setText(edit.name);
            priceET.setText(String.valueOf(edit.price));

            if (edit.imgbase64 != null && !edit.imgbase64.isEmpty()) {
                try {
                    Bitmap bmp = base64ToBitmap(edit.imgbase64);
                    if (bmp != null) {
                        dialogImage.setImageBitmap(bmp);
                    } else {
                        dialogImage.setImageResource(R.drawable.ic_placeholder);
                    }
                } catch (Exception e) {
                    dialogImage.setImageResource(R.drawable.ic_placeholder);
                }
            } else {
                dialogImage.setImageResource(R.drawable.ic_placeholder);
            }
        } else {
            dialogImage.setImageResource(R.drawable.ic_placeholder);
        }

        dialogImage.setOnClickListener(x -> chooseImage());

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(v)
                .setPositiveButton(edit == null ? "Add" : "Save", null)
                .setNegativeButton("Cancel", (d, i) -> {
                    tempUri = null;
                    tempimgbit = "";
                    dialogImage = null;
                })
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {
                String name = nameET.getText().toString().trim();
                String priceStr = priceET.getText().toString().trim();

                if (name.isEmpty()) {
                    safeToast("Enter product name");
                    return;
                }

                if (priceStr.isEmpty()) {
                    safeToast("Enter product price");
                    return;
                }

                double price;
                try {
                    price = Double.parseDouble(priceStr);
                } catch (Exception e) {
                    safeToast("Invalid price");
                    return;
                }

                if (price < 0) {
                    safeToast("Price cannot be negative");
                    return;
                }

                Map<String, Object> product = new HashMap<>();
                product.put("name", name);
                product.put("price", price);
                product.put("image", tempimgbit == null ? "" : tempimgbit);

                if (edit == null) {
                    db.collection("products")
                            .add(product)
                            .addOnSuccessListener(documentReference -> {
                                safeToast("Product added");
                                tempUri = null;
                                tempimgbit = "";
                                dialogImage = null;
                                dialog.dismiss();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error adding product", e);
                                safeToast("Failed to add product");
                            });
                } else {
                    db.collection("products")
                            .document(edit.id)
                            .update(product)
                            .addOnSuccessListener(unused -> {
                                safeToast("Product updated");
                                tempUri = null;
                                tempimgbit = "";
                                dialogImage = null;
                                dialog.dismiss();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error updating product", e);
                                safeToast("Failed to update product");
                            });
                }
            });
        });

        dialog.show();
    }

    private void openManagerOrdersSafely() {
        if (!isAdded()) return;
        if (!(getActivity() instanceof MainActivity)) return;
        ((MainActivity) getActivity()).changefrag(new ManagerOrdersFragment());
    }
    private void chooseImage() {
        if (!isAdded()) return;
        new AlertDialog.Builder(requireContext())
                .setItems(new String[]{"Camera", "Gallery","no picture"},
                        (d, i) -> {
                            if (i == 0)
                                openCamera();
                            else if(i==1)
                                openGallery();
                            else{
                                tempimgbit="";
                                dialogImage.setImageResource(R.drawable.ic_placeholder);
                            }
                        }).show();
    }

    private void setupLaunchers() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                r -> {
                    if (r.getResultCode() == RESULT_OK && r.getData() != null) {
                        tempUri = r.getData().getData();
                        try {
                            Bitmap bitmap = ImageDecoder.decodeBitmap(
                                    ImageDecoder.createSource(
                                            requireContext().getContentResolver(),
                                            tempUri
                                    )
                            );
                            tempimgbit = processImage(bitmap);
                            dialogImage.setImageURI(tempUri);
                        } catch (Exception e) {
                            Log.e("ManagerProductsFragment", "Error decoding image from gallery", e);
                        }
                    }
                });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                r -> {
                    if (r.getResultCode() == RESULT_OK && r.getData() != null && r.getData().getExtras() != null) {
                        Object obj = r.getData().getExtras().get("data");
                        if (obj instanceof Bitmap) {
                            Bitmap b = (Bitmap) obj;
                            tempimgbit = processImage(b);
                            if (dialogImage != null) dialogImage.setImageBitmap(b);
                        }
                    }
                });

        // This is the new launcher for requesting camera permission.
        requestCameraPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                // Permission is granted. Launch the camera.
                launchCameraIntent();
            } else {
                // The user denied the permission. Show a message.
                Toast.makeText(getContext(), "Camera Permission is Required to Use Camera", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openGallery() {
        Intent intent = new Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        );
        galleryLauncher.launch(intent);
    }

    private void openCamera() {
        // Check if the permission has already been granted.
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // Permission is already granted, launch the camera.
            launchCameraIntent();
        } else {
            // Permission has not been granted, so request it.
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCameraIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(intent);
    }

    //resizing
    private Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float ratio = (float) width / height;

        if (ratio > 1) {
            width = maxSize;
            height = (int) (width / ratio);
        } else {
            height = maxSize;
            width = (int) (height * ratio);
        }
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    //processing
    private String processImage(Bitmap bitmap) {
        Bitmap resizedBitmap = resizeBitmap(bitmap, 600);
        return bitmapToBase64(resizedBitmap);
    }

    // bitmap----->base64
    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // Compress (KEY POINT)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos);
        byte[] bytes = baos.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    // base64----->bitmap
    public static Bitmap base64ToBitmap(String base64) {
        byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

}
