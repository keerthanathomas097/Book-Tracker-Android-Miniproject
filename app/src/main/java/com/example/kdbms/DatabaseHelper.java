package com.example.kdbms;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "BookTrackerDB";
    private static final int DB_VERSION = 1;
    private static final String TABLE = "books";

    // Simple model class for one book
    public static class Book {
        public int id;
        public String title;
        public String author;
        public String category;
        public String status;     // Wishlist / Reading / Read
        public float rating;      // 0 - 5
        public String notes;
        public boolean favorite;
    }

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT NOT NULL, " +
                "author TEXT, " +
                "category TEXT, " +
                "status TEXT NOT NULL, " +
                "rating REAL DEFAULT 0, " +
                "notes TEXT, " +
                "favorite INTEGER DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Nothing to migrate yet (version 1).
    }

    private ContentValues toValues(Book b) {
        ContentValues v = new ContentValues();
        v.put("title", b.title);
        v.put("author", b.author);
        v.put("category", b.category);
        v.put("status", b.status);
        v.put("rating", b.rating);
        v.put("notes", b.notes);
        v.put("favorite", b.favorite ? 1 : 0);
        return v;
    }

    public boolean addBook(Book b) {
        SQLiteDatabase db = getWritableDatabase();
        return db.insert(TABLE, null, toValues(b)) != -1;
    }

    public boolean updateBook(Book b) {
        SQLiteDatabase db = getWritableDatabase();
        return db.update(TABLE, toValues(b), "id = ?",
                new String[]{String.valueOf(b.id)}) > 0;
    }

    public boolean deleteBook(int id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE, "id = ?", new String[]{String.valueOf(id)}) > 0;
    }

    /**
     * @param status        "Wishlist", "Reading", "Read" or null for all
     * @param search        text to match in title or author (can be empty)
     * @param favoritesOnly true to return only favorites
     */
    public List<Book> getBooks(String status, String search, boolean favoritesOnly) {
        List<Book> list = new ArrayList<>();
        StringBuilder where = new StringBuilder("1=1");
        List<String> args = new ArrayList<>();

        if (status != null) {
            where.append(" AND status = ?");
            args.add(status);
        }
        if (favoritesOnly) {
            where.append(" AND favorite = 1");
        }
        if (search != null && !search.trim().isEmpty()) {
            where.append(" AND (title LIKE ? OR author LIKE ?)");
            String like = "%" + search.trim() + "%";
            args.add(like);
            args.add(like);
        }

        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE, null, where.toString(),
                args.toArray(new String[0]), null, null, "title COLLATE NOCASE ASC");
        try {
            while (c.moveToNext()) {
                Book b = new Book();
                b.id = c.getInt(c.getColumnIndexOrThrow("id"));
                b.title = c.getString(c.getColumnIndexOrThrow("title"));
                b.author = c.getString(c.getColumnIndexOrThrow("author"));
                b.category = c.getString(c.getColumnIndexOrThrow("category"));
                b.status = c.getString(c.getColumnIndexOrThrow("status"));
                b.rating = c.getFloat(c.getColumnIndexOrThrow("rating"));
                b.notes = c.getString(c.getColumnIndexOrThrow("notes"));
                b.favorite = c.getInt(c.getColumnIndexOrThrow("favorite")) == 1;
                list.add(b);
            }
        } finally {
            c.close();
        }
        return list;
    }
}