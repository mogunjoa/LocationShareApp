package com.mogun.jenri

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.mogun.jenri.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val callback:(OAuthToken?, Throwable?) -> Unit = { token, error->
        if(error != null) {
            // 로그인 실패
        } else if(token != null) {
            // 로그인 성공
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Kakao SDK 초기화
        KakaoSdk.init(this, getString(R.string.KAKAO_KEY))

        binding.kakaoLoginButton.setOnClickListener {
            if(UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
                // 카카오톡 앱 로그인
                UserApiClient.instance.loginWithKakaoTalk(this) { token, error ->
                    if(error != null) {
                        // 로그인 실패
                        if(error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                            return@loginWithKakaoTalk
                        }
                        UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
                    } else if(token != null) {
                        // 로그인 성공
                        Log.i("token", token.toString())
                    }
                }
            } else{
                // 카카오톡 계정 로그인
                UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
            }
         }
        UserApiClient.instance

    }
}