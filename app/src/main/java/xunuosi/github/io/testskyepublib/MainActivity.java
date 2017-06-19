package xunuosi.github.io.testskyepublib;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.skytree.epub.BookInformation;

public class MainActivity extends AppCompatActivity {
    final String RELOADBOOK_ACTION = "com.skytree.android.intent.action.RELOADBOOK";
    final String PROGRESS_ACTION = "com.skytree.android.intent.action.PROGRESS";
    final String RELOAD_ACTION = "com.skytree.android.intent.action.RELOAD";

    private Button mBtnLoad, mBtnRead, mBtnLoadInternet;
    private LocalService ls = null;
    boolean isBound = false;
    MyApplication app;
    SkyReceiver mSkyReceiver;
    SkyUtility  st;
    private BookInformation lastBook = null;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocalService.LocalBinder binder = (LocalService.LocalBinder) service;
            ls = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    public void debug(String msg) {
//		if (Setting.isDebug()) {
        Log.w("EPub", msg);
//		}
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        doBindService();
        setContentView(R.layout.activity_main);
        app = (MyApplication) getApplication();
        st = new SkyUtility(this);
        st.makeSetup();
        this.registerFonts();

        mBtnLoad = (Button) findViewById(R.id.btn_load);
        mBtnLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                installBookFromAssets();
            }
        });
        mBtnRead = (Button) findViewById(R.id.btn_open);
        mBtnRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BookInformation bi = app.bis.get(0);
                lastBook = bi;
                openBookViewer(bi);
            }
        });

        mBtnLoadInternet = (Button) findViewById(R.id.btn_internet);
        mBtnLoadInternet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LoadInternetBook();
            }
        });
    }

    private void LoadInternetBook() {
        if (ls != null) {
//            ls.startDownload("http://192.168.99.151:8080/test/Alice.epub", null, null, null);
            ls.startDownload("http://scs.skyepub.net/samples/Alice.epub", "", "", "");
        }
    }

    private void openBookViewer(BookInformation bi) {
        this.openBookViewer(bi, false);
    }

    public void openBookViewer(BookInformation bi, boolean fromBeginning) {
        Intent intent;
        intent = new Intent(this, MyBookViewActivity.class);
        intent.putExtra("BOOKCODE", bi.bookCode);
        intent.putExtra("TITLE", bi.title);
        intent.putExtra("AUTHOR", bi.creator);
        intent.putExtra("BOOKNAME", bi.fileName);

        if (fromBeginning || bi.position < 0.0f) {
            intent.putExtra("POSITION", (double) -1.0f); // 7.x -1 stands for start position for both LTR and RTL book.
        } else {
            intent.putExtra("POSITION", bi.position);
        }
        intent.putExtra("THEMEINDEX", app.setting.theme);
        intent.putExtra("DOUBLEPAGED", app.setting.doublePaged);
        intent.putExtra("transitionType", app.setting.transitionType);
        intent.putExtra("GLOBALPAGINATION", app.setting.globalPagination);
        intent.putExtra("RTL", bi.isRTL);
        intent.putExtra("VERTICALWRITING", bi.isVerticalWriting);

        intent.putExtra("SPREAD", bi.spread);
        intent.putExtra("ORIENTATION", bi.orientation);

        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSkyReceiver = new SkyReceiver();
        IntentFilter intentFilter = new IntentFilter(RELOADBOOK_ACTION);
        registerReceiver(mSkyReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mSkyReceiver != null) {
            unregisterReceiver(mSkyReceiver);
        }
    }

    private void installBookFromAssets() {
        if (ls != null) {
            ls.installBook("file://android_asset/books/text.epub");
        }
    }

    private void doBindService() {
        Intent intent = new Intent(this, LocalService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        isBound = true;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unbindService(mConnection);
    }

    public class SkyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PROGRESS_ACTION)) {
                int bookCode = intent.getIntExtra("BOOKCODE",-1);
                int bytes_downloaded = intent.getIntExtra("BYTES_DOWNLOADED",-1);
                int bytes_total = intent.getIntExtra("BYTES_TOTAL",-1);
                double percent = intent.getDoubleExtra("PERCENT",0);
//	        	debug("Receiver BookCode:"+bookCode+" "+percent);
                Message msg = new Message();
                Bundle b = new Bundle();
                b.putInt("BOOKCODE", bookCode);
                b.putInt("BYTES_DOWNLOADED", bytes_downloaded);
                b.putInt("BYTES_TOTAL", bytes_total);
                b.putDouble("PERCENT", percent);
                msg.setData(b);
                new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        int bookCode = msg.getData().getInt("BOOKCODE");
                        int bytes_downloaded = msg.getData().getInt("BYTES_DOWNLOADED");
                        int bytes_total = msg.getData().getInt("BYTES_TOTAL");
                        double percent = msg.getData().getDouble("PERCENT");
//                        refreshPieView(bookCode,percent);
                    }
                }.sendMessage(msg);
            }else if  (intent.getAction().equals(RELOAD_ACTION)) {
                debug("Reload Requested");
                reload();
            }else if (intent.getAction().equals(RELOADBOOK_ACTION)) {
                int bookCode = intent.getIntExtra("BOOKCODE",-1);
                reload(bookCode);
            }
        }
    }

    public void reload() {
        app.reloadBookInformations();
        mBtnRead.setEnabled(true);
    }

    public void reload(int bookCode) {
        app.reloadBookInformations();
        mBtnRead.setEnabled(true);
    }

    public void registerFonts() {
        this.registerCustomFont("Underwood","uwch.ttf");
        this.registerCustomFont("Mayflower","Mayflower_Antique.ttf");
    }

    public void registerCustomFont(String fontFaceName,String fontFileName) {
        st.copyFontToDevice(fontFileName);
        app.customFonts.add(new CustomFont(fontFaceName,fontFileName));
    }
}
