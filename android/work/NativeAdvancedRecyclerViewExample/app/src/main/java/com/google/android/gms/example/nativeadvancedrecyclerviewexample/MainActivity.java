/*
 * Copyright (C) 2017 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.gms.example.nativeadvancedrecyclerviewexample;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A simple activity showing the use of menu items in
 * a {@link RecyclerView} widget.
 */
public class MainActivity extends AppCompatActivity {

    // The number of native ads to load and display.
    public static final int NUMBER_OF_ADS = 5;

    // The AdLoader used to load ads.
    private AdLoader adLoader;

    // List of native ads that have been successfully loaded.
    private List<UnifiedNativeAd> nativeAds = new ArrayList<>();

    // List of MenuItems that populate the RecyclerView.
    private List<Object> mRecyclerViewItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the Google Mobile Ads SDK.
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(final InitializationStatus initializationStatus) {
                Log.e("MainActivity", "onInitializationComplete");
            }
        });

        if (savedInstanceState == null) {
            // Create new fragment to display a progress spinner while the data set for the
            // RecyclerView is populated.
            Fragment loadingScreenFragment = new LoadingScreenFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.fragment_container, loadingScreenFragment);

            // Commit the transaction.
            transaction.commit();

            // Update the RecyclerView item's list with menu items.
            addMenuItemsFromJson();
            loadNativeAds();
        }
    }

    public List<Object> getRecyclerViewItems() {
        return mRecyclerViewItems;
    }

    private void loadMenu() {
        // Create new fragment and transaction
        Fragment newFragment = new RecyclerViewFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack
        transaction.replace(R.id.fragment_container, newFragment);
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();
    }

    /**
     * Adds {@link MenuItem}'s from a JSON file.
     */
    private void addMenuItemsFromJson() {
        try {
            String jsonDataString = readJsonDataFromFile();
            JSONArray menuItemsJsonArray = new JSONArray(jsonDataString);

            for (int i = 0; i < menuItemsJsonArray.length(); ++i) {

                JSONObject menuItemObject = menuItemsJsonArray.getJSONObject(i);

                String menuItemName = menuItemObject.getString("name");
                String menuItemDescription = menuItemObject.getString("description");
                String menuItemPrice = menuItemObject.getString("price");
                String menuItemCategory = menuItemObject.getString("category");
                String menuItemImageName = menuItemObject.getString("photo");

                MenuItem menuItem = new MenuItem(menuItemName, menuItemDescription, menuItemPrice,
                    menuItemCategory, menuItemImageName);
                mRecyclerViewItems.add(menuItem);
            }
        } catch (IOException | JSONException exception) {
            Log.e(MainActivity.class.getName(), "Unable to parse JSON file.", exception);
        }
    }

    /**
     * Reads the JSON file and converts the JSON data to a {@link String}.
     *
     * @return A {@link String} representation of the JSON data.
     * @throws IOException if unable to read the JSON file.
     */
    private String readJsonDataFromFile() throws IOException {

        InputStream inputStream = null;
        StringBuilder builder = new StringBuilder();

        try {
            String jsonDataString = null;
            inputStream = getResources().openRawResource(R.raw.menu_items_json);
            BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(inputStream, "UTF-8"));
            while ((jsonDataString = bufferedReader.readLine()) != null) {
                builder.append(jsonDataString);
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }

        return new String(builder);
    }

    private void insertAdsInMenuItems() {
        if (nativeAds.size() <= 0) {
            return;
        }

        final int offset = (mRecyclerViewItems.size() / nativeAds.size()) + 1;
        int index = 0;
        for (UnifiedNativeAd ad : nativeAds) {
            mRecyclerViewItems.add(index, ad);
            index = index + offset;
        }
    }

    private void loadNativeAds() {
        final AdLoader.Builder builder = new AdLoader.Builder(this, getString(R.string.ad_unit_id));
        adLoader = builder.forUnifiedNativeAd(new UnifiedNativeAd.OnUnifiedNativeAdLoadedListener() {
            @Override
            public void onUnifiedNativeAdLoaded(final UnifiedNativeAd unifiedNativeAd) {
                // A native ad loaded successfully, check if the ad loader has finished loading
                // and if so, insert the ads into the list.
                nativeAds.add(unifiedNativeAd);
                if (!adLoader.isLoading()) {
                    insertAdsInMenuItems();
                    loadMenu();
                }
            }
        }).withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(final int i) {
                // A native ad failed to load, check if the ad loader has finished loading
                // and if so, insert the ads into the list.
                Log.e("MainActivity", "The previous native ad failed to load. Attempting to load another.");
                if (!adLoader.isLoading()) {
                    insertAdsInMenuItems();
                    loadMenu();
                }
            }
        }).build();

        // Load the Native Express ad.
        adLoader.loadAds(new AdRequest.Builder().build(), NUMBER_OF_ADS);
    }
}
