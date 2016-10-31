package com.kodonho.android.firebase_auth;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "Firebase Auth";

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    FirebaseDatabase database;
    DatabaseReference userRef;
    ValueEventListener valueEventListener;

    Button btnSignin;
    Button btnSignup;
    Button btnSignout;
    EditText etEmail;
    EditText etPassword;
    TextView tvResult;

    ListView listView;
    ArrayList<Map<String,User>> datas = new ArrayList<>();
    ListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 데이터베이스 리스너
        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot users) {
                Log.i("FireBase","snapshot="+users.getValue());
                datas = new ArrayList<>();
                for(DataSnapshot userData : users.getChildren()){
                    try {
                        Map<String, User> data = new HashMap<>();
                        String userId = userData.getKey();
                        User user = userData.getValue(User.class);
                        data.put(userId, user);
                        datas.add(data);
                    }catch(DatabaseException e){
                        e.printStackTrace();
                    }
                }
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        btnSignin = (Button)findViewById(R.id.btnSignin);
        btnSignout = (Button)findViewById(R.id.btnSignout);
        btnSignup = (Button)findViewById(R.id.btnSignup);
        etEmail = (EditText)findViewById(R.id.etEmail);
        etPassword = (EditText)findViewById(R.id.etPassword);

        // 1. 인증객체 가져오기
        mAuth = FirebaseAuth.getInstance();
        // 2. 리스너 설정
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // 로그인을 하면 데이터베이스 리스너를 다시 등록해준다
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                    userRef.addValueEventListener(valueEventListener);
                } else {
                    // 로그아웃을 하면 데이터베이스 리스너를 해제해준다
                    // 그리고 datas를 초기화하고, adapter 를 갱신한다
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                    userRef.removeEventListener(valueEventListener);
                    datas = new ArrayList<>();
                    adapter.notifyDataSetChanged();
                }
            }
        };

        // 4. 신규계정 생성
        btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();
                String pw = etPassword.getText().toString().trim();
                if(!"".equals(email) && !"".equals(pw)){
                    addUser(email,pw);
                }else{
                    Toast.makeText(MainActivity.this, "Email 과 Password 를 입력하셔야 합니다",Toast.LENGTH_LONG).show();
                }
            }
        });

        // 5. sign in
        btnSignin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();
                String pw = etPassword.getText().toString().trim();
                if(!"".equals(email) && !"".equals(pw)){
                    signIn(email,pw);
                }else{
                    Toast.makeText(MainActivity.this, "Email 과 Password 를 입력하셔야 합니다",Toast.LENGTH_LONG).show();
                }
            }
        });

        btnSignout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                Toast.makeText(MainActivity.this, "Sign out 되었습니다",Toast.LENGTH_LONG).show();
            }
        });

        // Database 불러오기
        listView = (ListView) findViewById(R.id.listView);
        adapter = new ListAdapter();
        listView.setAdapter(adapter);

        // 참조포인트
        database = FirebaseDatabase.getInstance();
        userRef = database.getReference("users");
        userRef.addValueEventListener(valueEventListener);
    }



    public void signIn(String email,String pw){
        mAuth.signInWithEmailAndPassword(email,pw)
            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (!task.isSuccessful()) {
                        Toast.makeText(MainActivity.this, "Sign in 에 실패하였습니다",
                                Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(MainActivity.this, "Sign in 에 성공하였습니다",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.w(TAG, "Exception : " + e.getMessage());
                }
            });;
    }

    public void addUser(String email,String pw){
        mAuth.createUserWithEmailAndPassword(email, pw)
            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (!task.isSuccessful()) {
                        Toast.makeText(MainActivity.this, "사용자 등록에 실패하였습니다",
                                Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(MainActivity.this, "사용자 등록에 성공하였습니다",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                @Override
                public void onSuccess(AuthResult authResult) {
                    Log.w(TAG, "User : " + authResult.getUser());
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.w(TAG, "Exception : " + e.getMessage());
                }
            });
    }

    // 3. 리스너 해제 및 재등록 처리
    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    class ListAdapter extends BaseAdapter {

        LayoutInflater inflater;

        public ListAdapter(){
            inflater = getLayoutInflater();
        }

        @Override
        public int getCount() {
            return datas.size();
        }

        @Override
        public Object getItem(int position) {
            return datas.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null)
                convertView = inflater.inflate(R.layout.list_item,null);

            TextView tvName = (TextView)convertView.findViewById(R.id.tvName);
            TextView tvUid = (TextView)convertView.findViewById(R.id.tvUid);
            TextView tvEmail = (TextView)convertView.findViewById(R.id.tvEmail);

            Map<String,User> data = datas.get(position);
            String uid = data.keySet().iterator().next();
            User user = data.get(uid);

            tvUid.setText(uid);
            tvName.setText(user.username);
            tvEmail.setText(user.email);

            return convertView;
        }
    }
}
