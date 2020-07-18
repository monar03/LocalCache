package jp.aquabox.test;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.jakewharton.disklrucache.DiskLruCache;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import androidx.appcompat.app.AppCompatActivity;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import jp.aquabox.cache.DiskCache;
import jp.aquabox.cache.R;

public class MainActivity extends AppCompatActivity {
    private DiskCache diskCache;
    private int i = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();

        try {
            diskCache = new DiskCache(DiskLruCache.open(getCacheDir(), 1, 1, 300000));
        } catch (IOException e) {
            e.printStackTrace();
        }

        findViewById(R.id.in).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                diskCache.set("test1", new Data(i, "query" + i), -1, TimeUnit.MINUTES);
            }
        });
        findViewById(R.id.out).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Disposable d = diskCache.<Data>get("test1")
                        .doOnSuccess(new Consumer<Data>() {
                            @Override
                            public void accept(Data data) throws Exception {
                                ((TextView) findViewById(R.id.print)).setText(data.query);
                            }
                        })
                        .subscribe();
            }
        });
    }
}

class Data implements Serializable {
    final int test;
    final String query;

    Data(int test, String query) {
        this.test = test;
        this.query = query;
    }

}

