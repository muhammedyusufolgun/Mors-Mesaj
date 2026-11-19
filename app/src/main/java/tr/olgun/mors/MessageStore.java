package tr.olgun.mors;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.util.ArrayList;

final class MessageStore extends SQLiteOpenHelper {
    static final class Entry{
        final long id; final String text;
        Entry(long id,String text){this.id=id;this.text=text;}
        public String toString(){return text;}
    }
    MessageStore(Context context){super(context,"messages.db",null,1);}
    public void onCreate(SQLiteDatabase db){db.execSQL("CREATE TABLE messages (id INTEGER PRIMARY KEY, body TEXT NOT NULL)");}
    public void onUpgrade(SQLiteDatabase db,int from,int to){}
    void save(String text){ContentValues values=new ContentValues();values.put("body",text);getWritableDatabase().insertOrThrow("messages",null,values);}
    void remove(long id){getWritableDatabase().delete("messages","id=?",new String[]{Long.toString(id)});}
    ArrayList<Entry> entries(){
        ArrayList<Entry> list=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT id,body FROM messages ORDER BY id DESC",null)){
            while(c.moveToNext())list.add(new Entry(c.getLong(0),c.getString(1)));
        }
        return list;
    }
}
