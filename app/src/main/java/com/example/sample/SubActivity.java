package com.example.sample;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import android.content.Context;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import android.widget.Toast;
import androidx.core.view.GravityCompat;
import com.google.android.material.navigation.NavigationView;

import android.content.Intent;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import android.view.ViewGroup;
import androidx.annotation.Nullable;


import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.example.sample.NavigationActivity;
import com.example.sample.ProductItem;

import java.util.ArrayList;
import java.util.Arrays;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class SubActivity extends AppCompatActivity {
    private DatabaseReference mDatabase;
    private ValueEventListener dataListener;

    private ListView listView;
    private ArrayList<String> data;
    private ArrayAdapter<String> adapter;
    private DrawerLayout mDrawerLayout;
    private Context context = this;
    private String clickedItemKey; // clickedItemKey 변수를 선언합니다.

    private boolean[] itemClicked; // 아이템 클릭 여부를 저장하는 배열


    private static final int REQUEST_ENABLE_BT = 10;
    private BluetoothAdapter bluetoothAdapter;
    private Set<BluetoothDevice> devices;
    private BluetoothDevice bluetoothDevice;
    private BluetoothSocket bluetoothSocket = null;
    private OutputStream outputStream = null;
    private InputStream inputStream = null;
    private Thread workerThread = null;
    private byte[] readBuffer;
    private int readBufferPosition;
    private TextView textView_pm25;
    private TextView textView_conncetDevice;
    private Dialog dialog_help;
    //private BackPressCloseHandler backkeyclickhandler;
    boolean connect_status;
    int pairedDeviceCount;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub1);

        // Initialize Firebase Database
        mDatabase = FirebaseDatabase.getInstance().getReference("productItems");

        listView = findViewById(R.id.list_view);
        data = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, data);
        listView.setAdapter(adapter);


        // 데이터 로드 및 표시
        loadAndDisplayData();

        Button addButton = findViewById(R.id.add_button);
        addButton.setOnClickListener(v -> showAddItemDialog());

        Toolbar toolbar = findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);

        mDrawerLayout = findViewById(R.id.drawer_layout);

        NavigationView navigationView = findViewById(R.id.nav_view);
        NavigationActivity navigationActivity = new NavigationActivity(this, mDrawerLayout);
        navigationView.setNavigationItemSelectedListener(navigationActivity);


        textView_pm25 = findViewById(R.id.pm25);
        textView_conncetDevice = findViewById(R.id.connectname);


        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth 미지원 기기입니다.", Toast.LENGTH_SHORT).show();
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, REQUEST_ENABLE_BT);
            }
            selectBluetoothDevice();
        }


    }
    public void selectBluetoothDevice() {
        devices = bluetoothAdapter.getBondedDevices();
        pairedDeviceCount = devices.size();

        if (pairedDeviceCount == 0) {
            Toast.makeText(getApplicationContext(), "먼저 Bluetooth 설정에 들어가 페어링을 진행해 주세요.", Toast.LENGTH_SHORT).show();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("페어링 된 블루투스 디바이스 목록");
            List<String> list = new ArrayList<>();
            for (BluetoothDevice bluetoothDevice : devices) {
                list.add(bluetoothDevice.getName());
            }
            list.add("취소");

            final CharSequence[] charSequences = list.toArray(new CharSequence[0]);

            builder.setItems(charSequences, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    connectDevice(charSequences[which].toString());
                }
            });
            builder.setCancelable(false);
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }
    }
    /*
        @Override
        public void onBackPressed() {
            backkeyclickhandler.onBackPressed();
        }
    */
    public void connectDevice(String deviceName) {
        for (BluetoothDevice tempDevice : devices) {
            if (deviceName.equals(tempDevice.getName())) {
                bluetoothDevice = tempDevice;
                break;
            }
        }
        Toast.makeText(getApplicationContext(), bluetoothDevice.getName() + " 연결 완료!", Toast.LENGTH_SHORT).show();
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
        connect_status = true;

        try {
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();
            inputStream = bluetoothSocket.getInputStream();
            receiveData();
        } catch (IOException e) {
            e.printStackTrace();
        }
        textView_conncetDevice.setText(bluetoothDevice.getName());
    }

    public void receiveData() {
        final Handler handler = new Handler();
        readBufferPosition = 0;
        readBuffer = new byte[1024];

        workerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        int byteAvailable = inputStream.available();
                        if (byteAvailable > 0) {
                            byte[] bytes = new byte[byteAvailable];
                            inputStream.read(bytes);
                            for (int i = 0; i < byteAvailable; i++) {
                                byte tempByte = bytes[i];
                                if (tempByte == '\n') {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String text = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            textView_pm25.setText(text);

                                        }
                                    });
                                } else {
                                    readBuffer[readBufferPosition++] = tempByte;
                                }
                            }
                        }
                    } catch (IOException ex) {
                        stopWorkerThread();
                    }
                }
            }
        });
        workerThread.start();
    }

    public void sendData(String text) {
        text += "\n";
        try {
            outputStream.write(text.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopWorkerThread() {
        if (workerThread != null && !workerThread.isInterrupted()) {
            workerThread.interrupt();
        }
    }
    private void loadAndDisplayData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userUid = currentUser.getUid();
            String userPath = "users/" + userUid + "/productItems";

            DatabaseReference userRef = mDatabase.child(userPath);
            userRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    data.clear(); // Clear previous data
                    for (DataSnapshot dataSnapshotItem : dataSnapshot.getChildren()) {
                        ProductItem item = dataSnapshotItem.getValue(ProductItem.class);
                        if (item != null) {
                            String itemName = item.getName();
                            String expiryDate = item.getExpiryDate();
                            int quantity = item.getQuantity();
                            String key = dataSnapshotItem.getKey(); // 상품 항목의 키 값을 가져옴
                            addItem(itemName, expiryDate, quantity, key); // addItem 메서드 호출 시 key 값을 전달
                        }
                    }
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(SubActivity.this, "Failed to load data", Toast.LENGTH_SHORT).show();
                }
            });
        }

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, data) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                // 아이템의 클릭 여부에 따라 배경색을 변경합니다.
                if (itemClicked[position]) {
                    view.setBackgroundColor(getResources().getColor(R.color.clicked_item_color));
                } else {
                    view.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                }

                return view;
            }
        };

        listView.setAdapter(adapter);

        // 삭제 버튼의 클릭 이벤트 핸들러
        Button deleteButton = findViewById(R.id.delete_button);
        deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog());

    }


    private void showDeleteConfirmationDialog() {
        if (clickedItemKey != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("상품 삭제")
                    .setMessage("선택한 상품을 삭제하시겠습니까?")
                    .setPositiveButton("삭제", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            deleteSelectedItem();
                        }
                    })
                    .setNegativeButton("취소", null)
                    .show();
        } else {
            Toast.makeText(SubActivity.this, "삭제할 상품을 선택해주세요.", Toast.LENGTH_SHORT).show();
        }
    }


    private void deleteSelectedItem() {
        if (clickedItemKey != null) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                String userUid = currentUser.getUid();
                String userPath = "users/" + userUid + "/productItems/" + clickedItemKey;

                DatabaseReference userRef = mDatabase.child(userPath);
                userRef.removeValue()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(SubActivity.this, "상품이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(SubActivity.this, "상품 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        } else {
            Toast.makeText(SubActivity.this, "삭제할 상품을 선택해주세요.", Toast.LENGTH_SHORT).show();
        }
    }



    private void saveToFirebase(ProductItem productItem) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userUid = currentUser.getUid();
            String userPath = "users/" + userUid + "/productItems"; // 사용자별 경로 생성

            DatabaseReference userRef = mDatabase.child(userPath);
            String key = userRef.push().getKey();
            if (key != null) {
                userRef.child(key).setValue(productItem)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(SubActivity.this, "상품이 추가되었습니다.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(SubActivity.this, "상품 추가에 실패했습니다.", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        }
    }

    private void showAddItemDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_item, null);
        builder.setView(dialogView)
                .setTitle("상품 추가")
                .setPositiveButton("추가", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EditText editItemName = dialogView.findViewById(R.id.edit_item_name);
                        String itemName = editItemName.getText().toString();

                        DatePicker datePicker = dialogView.findViewById(R.id.datepicker_expiry);
                        int year = datePicker.getYear();
                        int month = datePicker.getMonth();
                        int day = datePicker.getDayOfMonth();
                        String expiryDate = String.format("%04d-%02d-%02d", year, month + 1, day); // 유통 기한을 YYYY-MM-DD 형식으로 변환하여 저장

                        TextView textQuantity = dialogView.findViewById(R.id.text_quantity);
                        int quantity = Integer.parseInt(textQuantity.getText().toString()); // 수량을 가져옴


                        // Firebase Realtime Database에 생성된 ProductItem을 저장합니다.
                        saveToFirebase(new ProductItem(itemName, expiryDate, quantity));



                    }
                })
                .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();

        // 수량 증가 버튼 클릭 이벤트 처리
        Button buttonIncrease = dialogView.findViewById(R.id.button_increase);
        buttonIncrease.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView textQuantity = dialogView.findViewById(R.id.text_quantity);
                int quantity = Integer.parseInt(textQuantity.getText().toString());
                quantity++;
                textQuantity.setText(String.valueOf(quantity));
            }
        });

        // 수량 감소 버튼 클릭 이벤트 처리
        Button buttonDecrease = dialogView.findViewById(R.id.button_decrease);
        buttonDecrease.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView textQuantity = dialogView.findViewById(R.id.text_quantity);
                int quantity = Integer.parseInt(textQuantity.getText().toString());
                if (quantity > 1) {
                    quantity--;
                    textQuantity.setText(String.valueOf(quantity));
                }
            }
        });
    }


    private void addItem(String newItem, String expiryDate, int quantity, String key) {
        String itemDetails = newItem + " - 유통 기한: " + expiryDate + ", 수량: " + quantity;
        data.add(itemDetails);

        // 새로운 아이템이 추가될 때마다 itemClicked 배열을 업데이트합니다.
        itemClicked = new boolean[data.size()];
        Arrays.fill(itemClicked, false);

        adapter.notifyDataSetChanged();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (clickedItemKey != null && clickedItemKey.equals(key)) {
                // 이미 선택된 아이템을 클릭한 경우, 클릭 여부를 해제하고 리스트뷰를 업데이트합니다.
                clickedItemKey = null;
                itemClicked[position] = false;
            } else {
                clickedItemKey = key;
                itemClicked[position] = true;
            }

            adapter.notifyDataSetChanged();
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dataListener != null) {
            mDatabase.removeEventListener(dataListener);
        }
    }

    public void openDrawer(View view) {
        mDrawerLayout.openDrawer(GravityCompat.START);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
