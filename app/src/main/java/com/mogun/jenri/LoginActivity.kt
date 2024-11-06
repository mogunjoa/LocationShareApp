package com.mogun.jenri

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.kakao.sdk.user.model.User
import com.mogun.jenri.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
        if (error != null) {
            // 로그인 실패
            showErrorToast()
            error.printStackTrace()
        } else if (token != null) {
            // 로그인 성공
            getKakaoAccountInfo()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Kakao SDK 초기화
        KakaoSdk.init(this, getString(R.string.KAKAO_KEY))

        binding.kakaoLoginButton.setOnClickListener {
            if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
                // 카카오톡 앱 로그인
                UserApiClient.instance.loginWithKakaoTalk(this) { token, error ->
                    if (error != null) {
                        // 로그인 실패
                        if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                            return@loginWithKakaoTalk
                        }
                        UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
                    } else if (token != null) {
                        if (Firebase.auth.currentUser == null) {
                            // 카카오톡에서 정보를 가져와 파이버베이스 로그인
                            getKakaoAccountInfo()
                        } else {
                            navigateToMapActivity()
                        }
                    }
                }
            } else {
                // 카카오톡 계정 로그인
                UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
            }
        }
        UserApiClient.instance

    }

    private fun navigateToMapActivity() {
        startActivity(Intent(this, MapActivity::class.java))
    }

    private fun getKakaoAccountInfo() {
        UserApiClient.instance.me { user, error ->
            if (error != null) {
                showErrorToast()
                Log.e("getKakaoAccountInfo", "사용자 정보 요청 실패", error)
            } else if (user != null) {
                Log.e(
                    "LoginActivity",
                    "user: 회원번호: ${user.id} / 이메일 : ${user.kakaoAccount?.email} / 닉네임 : ${user.kakaoAccount?.profile?.nickname} / 프로필 사진 : ${user.kakaoAccount?.profile?.thumbnailImageUrl}"
                )

                checkKakaoUserData(user)
            }
        }
    }

    private fun checkKakaoUserData(user: User) {
        val kakaoEmail = user.kakaoAccount?.email.orEmpty()

        if(kakaoEmail.isEmpty()) {
            // 추가로 이메일을 받는 작업

            return
        }

        signInFirebase(user, kakaoEmail)
    }

    private fun signInFirebase(user: User, email: String) {
        val uid = user.id.toString()

        Firebase.auth.createUserWithEmailAndPassword(email, uid).addOnCompleteListener {
            if(it.isSuccessful) {
                updateFirebaseDatabase(user)
            } else {
                showErrorToast()
            }
        }.addOnFailureListener {
            // 이미 가입된 계정
            if(it is FirebaseAuthUserCollisionException) {
                Firebase.auth.signInWithEmailAndPassword(email, uid).addOnCompleteListener { result ->
                    if(result.isSuccessful) {
                        updateFirebaseDatabase(user)
                    } else {
                        showErrorToast()
                    }
                }.addOnFailureListener { error ->
                    error.printStackTrace()
                    showErrorToast()
                }
            } else {
                showErrorToast()
            }
        }
    }

    private fun updateFirebaseDatabase(user: User) {
        val uid = Firebase.auth.currentUser?.uid.orEmpty()
        val userMap = mutableMapOf<String, Any>()
        userMap["uid"] = uid
        userMap["name"] = user.kakaoAccount?.profile?.nickname.orEmpty()
        userMap["profilePhoto"] = user.kakaoAccount?.profile?.thumbnailImageUrl.orEmpty()
        userMap["email"] = user.kakaoAccount?.email.orEmpty()

        Firebase.database.reference.child("Person").child(uid).updateChildren(userMap)
        navigateToMapActivity()
    }

    private fun showErrorToast() {
        Toast.makeText(this, "사용자 로그인 요청에 실패했습니다.", Toast.LENGTH_SHORT).show()
    }
}