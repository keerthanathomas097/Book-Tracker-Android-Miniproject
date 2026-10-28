package com.example.kdbms;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String[] CATEGORIES =
            {"Fiction", "Non-fiction", "Tech", "Self-help", "Biography", "Other"};
    private static final String[] STATUSES = {"Wishlist", "Reading", "Read"};

    private DatabaseHelper dbHelper;
    private ListView listBooks;
    private TextView emptyView;
    private EditText searchBox;
    private ChipGroup chipGroup;
    private ArrayAdapter<DatabaseHelper.Book> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        dbHelper = new DatabaseHelper(this);
        listBooks = findViewById(R.id.listBooks);
        emptyView = findViewById(R.id.emptyView);
        searchBox = findViewById(R.id.searchBox);
        chipGroup = findViewById(R.id.chipGroup);

        // List rows: title + author on line 1, category/status/stars on line 2
        adapter = new ArrayAdapter<DatabaseHelper.Book>(this,
                android.R.layout.simple_list_item_2, android.R.id.text1,
                new ArrayList<DatabaseHelper.Book>()) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View row = super.getView(position, convertView, parent);
                DatabaseHelper.Book b = getItem(position);
                TextView line1 = row.findViewById(android.R.id.text1);
                TextView line2 = row.findViewById(android.R.id.text2);

                String heart = b.favorite ? "♥ " : "";
                String author = (b.author == null || b.author.isEmpty()) ? "" : "  —  " + b.author;
                line1.setText(heart + b.title + author);

                String info = b.category + "  •  " + b.status;
                if (b.status.equals("Read")) {
                    info += "  •  " + stars(b.rating);
                }
                line2.setText(info);
                return row;
            }
        };
        listBooks.setAdapter(adapter);

        // Tap a book -> edit/delete dialog
        listBooks.setOnItemClickListener((parent, view, position, id) ->
                showBookDialog(adapter.getItem(position)));

        // Long-press a book -> toggle favorite quickly
        listBooks.setOnItemLongClickListener((parent, view, position, id) -> {
            DatabaseHelper.Book b = adapter.getItem(position);
            b.favorite = !b.favorite;
            dbHelper.updateBook(b);
            Toast.makeText(this, b.favorite ? "Added to favorites" : "Removed from favorites",
                    Toast.LENGTH_SHORT).show();
            loadBooks();
            return true;
        });

        // Filter chips
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> loadBooks());

        // Live search
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadBooks();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        loadBooks();
    }

    // Called by the "+ Add Book" button (android:onClick="addBook")
    public void addBook(View view) {
        showBookDialog(null);
    }

    // Reloads the list based on the chip filter + search text
    private void loadBooks() {
        int checked = chipGroup.getCheckedChipId();
        String status = null;
        boolean favOnly = false;
        if (checked == R.id.chipWishlist) status = "Wishlist";
        else if (checked == R.id.chipReading) status = "Reading";
        else if (checked == R.id.chipRead) status = "Read";
        else if (checked == R.id.chipFav) favOnly = true;

        List<DatabaseHelper.Book> books =
                dbHelper.getBooks(status, searchBox.getText().toString(), favOnly);
        adapter.clear();
        adapter.addAll(books);
        emptyView.setVisibility(books.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // Add (existing == null) or Edit/Delete (existing != null) dialog
    private void showBookDialog(final DatabaseHelper.Book existing) {
        final boolean isEdit = existing != null;

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(8), dp(20), 0);

        final EditText etTitle = new EditText(this);
        etTitle.setHint("Title");
        etTitle.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

        final EditText etAuthor = new EditText(this);
        etAuthor.setHint("Author");
        etAuthor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

        final Spinner spCategory = new Spinner(this);
        spCategory.setAdapter(spinnerAdapter(CATEGORIES));

        final Spinner spStatus = new Spinner(this);
        spStatus.setAdapter(spinnerAdapter(STATUSES));

        final TextView ratingLabel = new TextView(this);
        ratingLabel.setText("Your rating");
        ratingLabel.setPadding(0, dp(12), 0, 0);

        final RatingBar ratingBar = new RatingBar(this);
        ratingBar.setNumStars(5);
        ratingBar.setStepSize(1f);
        ratingBar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final EditText etNotes = new EditText(this);
        etNotes.setHint("Notes / short review");
        etNotes.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        etNotes.setMinLines(2);

        final CheckBox cbFav = new CheckBox(this);
        cbFav.setText("♥ Favorite");

        form.addView(etTitle);
        form.addView(etAuthor);
        form.addView(spCategory);
        form.addView(spStatus);
        form.addView(ratingLabel);
        form.addView(ratingBar);
        form.addView(etNotes);
        form.addView(cbFav);

        // Rating only makes sense once the book is "Read"
        spStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int vis = STATUSES[position].equals("Read") ? View.VISIBLE : View.GONE;
                ratingLabel.setVisibility(vis);
                ratingBar.setVisibility(vis);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // Pre-fill when editing
        if (isEdit) {
            etTitle.setText(existing.title);
            etAuthor.setText(existing.author);
            spCategory.setSelection(indexOf(CATEGORIES, existing.category));
            spStatus.setSelection(indexOf(STATUSES, existing.status));
            ratingBar.setRating(existing.rating);
            etNotes.setText(existing.notes);
            cbFav.setChecked(existing.favorite);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.addView(form);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(isEdit ? "Edit Book" : "Add Book")
                .setView(scroll)
                .setPositiveButton("Save", null)   // overridden below so we can validate
                .setNegativeButton("Cancel", null);
        if (isEdit) {
            builder.setNeutralButton("Delete", null);
        }

        final AlertDialog dialog = builder.create();
        dialog.show();

        // SAVE
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            if (title.isEmpty()) {
                etTitle.setError("Title is required");
                return;
            }

            DatabaseHelper.Book b = isEdit ? existing : new DatabaseHelper.Book();
            b.title = title;
            b.author = etAuthor.getText().toString().trim();
            b.category = CATEGORIES[spCategory.getSelectedItemPosition()];
            b.status = STATUSES[spStatus.getSelectedItemPosition()];
            b.rating = b.status.equals("Read") ? ratingBar.getRating() : 0f;
            b.notes = etNotes.getText().toString().trim();
            b.favorite = cbFav.isChecked();

            boolean ok = isEdit ? dbHelper.updateBook(b) : dbHelper.addBook(b);
            Toast.makeText(this, ok ? "Saved" : "Failed to save", Toast.LENGTH_SHORT).show();
            loadBooks();
            dialog.dismiss();
        });

        // DELETE (with confirmation)
        if (isEdit) {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v ->
                    new AlertDialog.Builder(this)
                            .setTitle("Delete book?")
                            .setMessage("“" + existing.title + "” will be removed.")
                            .setPositiveButton("Delete", (d, which) -> {
                                dbHelper.deleteBook(existing.id);
                                Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                                loadBooks();
                                dialog.dismiss();
                            })
                            .setNegativeButton("Cancel", null)
                            .show());
        }
    }

    // ---------- helpers ----------

    private ArrayAdapter<String> spinnerAdapter(String[] items) {
        ArrayAdapter<String> a = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, items);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return a;
    }

    private int indexOf(String[] arr, String value) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(value)) return i;
        }
        return 0;
    }

    private String stars(float rating) {
        StringBuilder sb = new StringBuilder();
        int r = Math.round(rating);
        for (int i = 1; i <= 5; i++) sb.append(i <= r ? "★" : "☆");
        return sb.toString();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}