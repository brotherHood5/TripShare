package com.example.tripblog.ui.post;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.core.view.ViewCompat;

import com.example.tripblog.R;
import com.example.tripblog.model.Post;
import com.example.tripblog.ui.SimpleLoadingDialog;
import com.example.tripblog.utils.PathUtil;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.JsonArray;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Properties;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditablePostDetailActivity extends PostDetailActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        isEditable = true;
        super.onCreate(savedInstanceState);

        // Top app bar
        binding.appbar.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            int collapseToolbarHeight = binding.collapseToolbarLayout.getHeight();
            if (collapseToolbarHeight + verticalOffset < (2 * ViewCompat.getMinimumHeight(binding.collapseToolbarLayout))) {
                binding.toolbar.getNavigationIcon().setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
                binding.toolbar.getMenu().getItem(0).setVisible(false);
            }
            else {
                binding.toolbar.getNavigationIcon().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                binding.toolbar.getMenu().getItem(0).setVisible(true);
            }
        });
    }

    @Override
    protected void reloadEditableView() {
        super.reloadEditableView();
        // Edit date
        binding.tripDates.setOnClickListener((view) -> {editTripDates();});

        // Edit title on un-focus event
        binding.tripTitle.setTextIsSelectable(isEditable);
        binding.tripTitle.setFocusable(isEditable);
        binding.tripTitle.setOnFocusChangeListener(onUpdateTripTitle());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.post_detail_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.coverImgChooser:
                onClickRequestPermission();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onClickRequestPermission() {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        }
        else {
            String[] permissions = {
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };
            requestPermissions(permissions, REQUEST_CODE);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            }
        }
    }

    private View.OnFocusChangeListener onUpdateTripTitle() {
        return new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean isFocus) {
                if (isFocus) return;
                String titleTxt = binding.tripTitle.getText().toString().trim();
                RequestBody title = RequestBody.create(MediaType.parse("multipart/form-data"), titleTxt);
                Properties data = new Properties();
                data.put("title", title);
                callUpdateApi(data);
            }
        };
    }

    private void editTripDates() {
        if (tripDates == null) {
            tripDates = MaterialDatePicker.Builder.dateRangePicker()
                    .setTitleText("Trip dates")
                    .build();

            tripDates.addOnPositiveButtonClickListener((selection -> {
                Calendar cal = Calendar.getInstance();
                Pair<Long, Long> dates = (Pair<Long, Long>) selection;
                cal.setTimeInMillis(dates.first);
                currPost.setStartDate(cal.getTime());
                cal.setTimeInMillis(dates.second);
                currPost.setEndDate(cal.getTime());

                setTripDatesText(currPost.getStartDate(), currPost.getEndDate(), D_M_YY);
            }));
        }

        tripDates.show(getSupportFragmentManager(), "TRIP-DATES-PICKER");
    }

    private void updateTripDate() {

    }

    private void updateCoverImg(Uri imageUri) {
        try {
            SimpleLoadingDialog loadingDialog = new SimpleLoadingDialog(this);
            loadingDialog.show();

            String imageRealPath = PathUtil.getRealPath(this, imageUri);
            File coverImgFile = new File(imageRealPath);
            RequestBody coverImg = RequestBody.create(MediaType.parse("multipart/form-data"), coverImgFile);
            MultipartBody.Part multipartBodyCoverImg = MultipartBody.Part.createFormData("cover_img", coverImgFile.getName(), coverImg);
            Properties data = new Properties();
            data.put("cover_img", multipartBodyCoverImg);
            data.put("imageRealPath", imageRealPath);
            data.put("loading", loadingDialog);
            callUpdateApi(data);
        } catch (Exception err) {
            Log.e(TAG, err.toString());
        }
    }

    private void callUpdateApi(Properties properties) {
        RequestBody postId = RequestBody.create(MediaType.parse("multipart/form-data"), currPost.getId().toString());
        postService.updatePost(
                        postId,
                        (RequestBody) properties.get("title"),
                        (RequestBody) properties.get("brief_description"),
                        (RequestBody) properties.get("start_date"),
                        (RequestBody) properties.get("end_date"),
                        (RequestBody) properties.get("is_public"),
                        (MultipartBody.Part) properties.get("cover_img")
                )
                .enqueue(new Callback<JsonArray>() {
                    @Override
                    public void onResponse(Call<JsonArray> call, Response<JsonArray> response) {
                        if (properties.containsKey("imageRealPath")) {
                            ((SimpleLoadingDialog) properties.get("loading")).dismiss();
                            PathUtil.deleteTempFile(EditablePostDetailActivity.this, (String) properties.get("imageRealPath"));
                        }

                        if (!response.isSuccessful()) return;
                        JsonArray body = response.body();
                        // Update success
                        if (body.get(0).getAsInt() == 1) {
                            Post updatedPost = new Gson().fromJson(body.get(1).getAsJsonObject(), Post.class);
                            currPost.setTitle(updatedPost.getTitle());
                            currPost.setStartDate(updatedPost.getStartDate());
                            currPost.setEndDate(updatedPost.getEndDate());
                            currPost.setPublic(updatedPost.getPublic());
                            currPost.setCoverImg(updatedPost.getCoverImg());
                            currPost.setBriefDescription(currPost.getBriefDescription());
                            currPost.setUpdatedAt(updatedPost.getUpdatedAt());

                            loadData();
                            Snackbar
                                    .make(binding.getRoot(), "Upload cover image successfully.", Snackbar.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonArray> call, Throwable t) {
                        if (properties.containsKey("imageRealPath")) {
                            ((SimpleLoadingDialog) properties.get("loading")).dismiss();
                            PathUtil.deleteTempFile(EditablePostDetailActivity.this, (String) properties.get("imageRealPath"));
                        }

                        Snackbar
                                .make(binding.getRoot(), "Can't upload cover image!", Snackbar.LENGTH_SHORT).show();
                    }
                });
    }

    protected void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        activityResultLauncher.launch(Intent.createChooser(intent, "Select Picture"));
    }

    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK  && result.getData() != null) {

                        final Uri imageUri = result.getData().getData();
                        updateCoverImg(imageUri);
                    }
                }
            }
    );
}