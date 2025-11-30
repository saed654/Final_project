package com.example.final_projectsss;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id= item.getItemId();
        /// log
        if(id==R.id.log_inmenu){
            return true;
        }
        ///  home
        if(id==R.id.homemenu){
            return true;
        }
        /// products
        if(id==R.id.productsmenu){
            return true;
        }
        /// aimenu
        if(id==R.id.aimenu){
            return true;
        }
        /// timeschedule
        if(id==R.id.timeschedule){
            return true;
        }
        return true;
    }

    public void changefrag(Fragment fragment){
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
    }
    public boolean onCreateOptionsMenu(Menu menu) {
// inflates the menu XML into view

        getMenuInflater().inflate(R.menu.menu,menu);
        return true;
    }
}