package com.example.final_projectsss;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.final_projectsss.ai.Ai_fragment;
import com.example.final_projectsss.log_in.Log_in_fragment;
import com.example.final_projectsss.log_in.Manager_fragment;
import com.example.final_projectsss.products.ManagerProductsFragment;
import com.example.final_projectsss.products.UserProductsFragment;
import com.example.final_projectsss.timeschedule.Schedule_fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {
    boolean x=false;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseAuth auth;
    BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // hide menu until role is known
        bottomNavigationView.setVisibility(View.GONE);
        bottomNavigationView.getMenu().clear();

        setupBottomNavigation();

        loadStartPageByRole();
    }

    private void loadStartPageByRole() {
        if (auth.getCurrentUser() == null) {
            setmenu("");
            changefrag(new Home_fragment(), R.id.homemenu);
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show();
                        auth.signOut();
                        setmenu("");
                        changefrag(new Home_fragment(), R.id.homemenu);
                        return;
                    }
                    if(documentSnapshot.getBoolean("active") != null || documentSnapshot.getBoolean("active")) {
                        String role = documentSnapshot.getString("role");
                        if (role != null) role = role.trim().toLowerCase();

                        if ("manager".equals(role)) {
                            setmenu("manager");
                            changefrag(new Manager_fragment(), R.id.managerdash);
                        } else {
                            setmenu("user");
                            changefrag(new UserProductsFragment(), R.id.productsmenu);
                        }
                    }else{
                    auth.signOut();
                    setmenu("");
                    changefrag(new Home_fragment(), R.id.homemenu);}
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                    auth.signOut();
                    setmenu("");
                    changefrag(new Home_fragment(), R.id.homemenu);
                });
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
           if(!x) {
               int id = item.getItemId();

               if (id == R.id.log_inmenu) {
                   changefrag(new Log_in_fragment());
                   return true;
               } else if (id == R.id.homemenu) {
                   changefrag(new Home_fragment());
                   return true;
               } else if (id == R.id.productsmenu) {
                   changefrag(new UserProductsFragment());
                   return true;
               } else if (id == R.id.aimenu) {
                   if (auth.getCurrentUser() == null) {
                       Toast.makeText(this, "You must log in first", Toast.LENGTH_SHORT).show();
                       changefrag(new Log_in_fragment());
                   } else {
                       changefrag(new Ai_fragment());
                   }
                   return true;
               } else if (id == R.id.timeschedule) {
                   if (auth.getCurrentUser() == null) {
                       Toast.makeText(this, "You must log in first", Toast.LENGTH_SHORT).show();
                       changefrag(new Log_in_fragment());
                   } else {
                       changefrag(new Schedule_fragment());
                   }
                   return true;
               } else if (id == R.id.log_out_menu) {
                   auth.signOut();
                   setmenu("");
                   changefrag(new Home_fragment());
                   return true;
               } else if (id == R.id.managerdash) {
                   changefrag(new Manager_fragment());
                   return true;
               } else if (id == R.id.products_mang_menu) {
                   changefrag(new ManagerProductsFragment());
                   return true;
               }

               return false;
           }
           x=false;
            return true;
          });

    }


    public void changefrag(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
    public void changefrag(Fragment fragment, int selectedMenuId) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();

        x=true;

        if (selectedMenuId != -1 && bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(selectedMenuId);
        }
    }

    public void setmenu(String role) {
        bottomNavigationView.getMenu().clear();

        if (role == null) role = "";
        role = role.trim().toLowerCase();

        if ("manager".equals(role)) {
            bottomNavigationView.inflateMenu(R.menu.menu_manager);
            bottomNavigationView.setVisibility(View.VISIBLE);
        }
        else if ("user".equals(role)) {
            bottomNavigationView.inflateMenu(R.menu.menu_client);
            bottomNavigationView.setVisibility(View.VISIBLE);
        }
        else {
            bottomNavigationView.inflateMenu(R.menu.menu);
            bottomNavigationView.setVisibility(View.VISIBLE);
        }
    }
}